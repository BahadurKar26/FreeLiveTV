#!/usr/bin/env python3
"""
Unified Content Aggregator for FreeLiveTV (v2.5 - Final Path & Query Fix)
-------------------------------------------------------------------------
Outputs:
    curated_channels.json
    movies.json
    series.json
"""

import json
import asyncio
import aiohttp
import re
import hashlib
import os
import time
from datetime import datetime
import urllib.parse

# ----------------------------------------------------------------------
# Configuration
# ----------------------------------------------------------------------
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
# Target Asset Directory
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
ASSETS_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets")
SOURCES_FILE = os.path.join(ASSETS_DIR, "sources.json")

MAX_CHANNELS = 2000
MAX_MOVIES = 1000
MAX_SERIES = 500

TMDB_API_KEY = "90c68a4baddabebeffc50f29fd6774f6"
TMDB_DELAY = 0.1

def log(msg):
    print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}")

# ----------------------------------------------------------------------
# Source loading
# ----------------------------------------------------------------------
async def fetch_sources(session):
    if os.path.exists(SOURCES_FILE):
        log(f"📂 Detected sources.json at: {SOURCES_FILE}")
        with open(SOURCES_FILE, 'r', encoding='utf-8') as f:
            data = json.load(f)
            log(f"📋 Loaded {len(data)} source entries from assets.")
            return data

    log(f"❌ sources.json NOT FOUND at {SOURCES_FILE}. Using fallback search.")
    # More robust fallback queries for Archive.org
    return [
        {"name": "IPTV Global", "url": "https://iptv-org.github.io/iptv/index.m3u", "type": "M3U", "category": "Live TV"},
        {"name": "IA Movies", "url": "https://archive.org/advancedsearch.php?q=mediatype:movies+AND+collection:feature_films&fl[]=identifier,title,description,year&rows=100&output=json", "type": "JSON", "category": "Movies"},
        {"name": "IA Series", "url": "https://archive.org/advancedsearch.php?q=mediatype:movies+AND+collection:classic_tv&fl[]=identifier,title,description,year&rows=100&output=json", "type": "JSON", "category": "Series"}
    ]

# ----------------------------------------------------------------------
# Parsers
# ----------------------------------------------------------------------
def normalize_name(name):
    name = name.lower()
    name = re.sub(r'\(.*?\)|\[.*?\]', '', name)
    name = re.sub(r'\s+(hd|sd|fhd|uhd|720p|1080p|4k)\b', '', name)
    return re.sub(r'[^a-z0-9]', '', name)

def parse_m3u(content, source_category="Live TV"):
    channels = []
    lines = content.split('\n')
    for i in range(len(lines)):
        line = lines[i].strip()
        if line.startswith('#EXTINF:'):
            name_match = re.search(r',(.+)$', line)
            name = name_match.group(1).strip() if name_match else "Unknown"
            logo_match = re.search(r'tvg-logo="([^"]*)"', line)
            category_match = re.search(r'group-title="([^"]*)"', line)

            url = ""
            for j in range(i + 1, len(lines)):
                l = lines[j].strip()
                if l.startswith('http'):
                    url = l
                    break
                if l.startswith('#EXTINF'): break

            if url:
                channels.append({
                    "id": f"ch_{hashlib.md5(url.encode()).hexdigest()[:12]}",
                    "name": name, "url": url,
                    "logo": logo_match.group(1) if logo_match else "",
                    "category": category_match.group(1) if category_match else source_category,
                    "region": "Bangla" if any(x in name.lower() for x in ['bangla', 'bd']) else "Global"
                })
    return channels

def parse_archive_search(data):
    docs = data.get('response', {}).get('docs', [])
    return [{
        "id": f"ia_{d['identifier']}",
        "identifier": d['identifier'],
        "title": d.get('title', 'Unknown'),
        "description": d.get('description', ''),
        "year": d.get('year', 0),
        "poster_url": f"https://archive.org/services/img/{d['identifier']}"
    } for d in docs if 'identifier' in d]

async def fetch_archive_metadata(session, identifier):
    url = f"https://archive.org/metadata/{identifier}"
    try:
        async with session.get(url, timeout=15) as resp:
            return await resp.json() if resp.status == 200 else None
    except: return None

def parse_episode_from_filename(filename):
    patterns = [r'[Ss](\d+)[Ee](\d+)', r'(\d+)[Xx](\d+)', r'[Ee]p(?:isode)?[ _]?(\d+)', r'v(\d+)e(\d+)']
    for p in patterns:
        m = re.search(p, filename, re.I)
        if m:
            g = m.groups()
            if len(g) == 2: return int(g[0]), int(g[1])
            return 1, int(g[0])
    return 1, None

def parse_archive_episodes(metadata, base_item):
    if not metadata or 'files' not in metadata: return None, []
    files = metadata['files']
    videos = []
    ident = metadata['metadata']['identifier']
    for f in files:
        name = f.get('name', '')
        if name.lower().endswith(('.mp4', '.mkv', '.m4v', '.avi')):
            s, e = parse_episode_from_filename(name)
            if e is None: continue
            videos.append({
                "id": f"{base_item['id']}_s{s}e{e}",
                "seriesId": base_item['id'],
                "title": name.replace('_', ' ').replace('.mp4', ''),
                "seasonNumber": s, "episodeNumber": e,
                "streamUrl": f"https://archive.org/download/{ident}/{name}",
                "duration": int(float(f.get('length', 0))) // 60 if f.get('length') else 0,
                "thumbnailUrl": base_item['poster_url']
            })
    if not videos: return None, []

    videos.sort(key=lambda x: (x['seasonNumber'], x['episodeNumber']))
    seasons = []
    for ep in videos:
        if not seasons or seasons[-1]['number'] != ep['seasonNumber']:
            seasons.append({"number": ep['seasonNumber'], "episodes": []})
        seasons[-1]['episodes'].append(ep)

    series_obj = {
        "id": base_item['id'], "title": base_item['title'], "posterUrl": base_item['poster_url'],
        "genre": ["General"], "year": base_item['year'], "description": base_item['description'],
        "seasons": seasons, "rating": 0.0
    }
    return series_obj, videos

# ----------------------------------------------------------------------
# TMDB Logic
# ----------------------------------------------------------------------
async def enrich_series(session, series):
    if not TMDB_API_KEY: return series
    try:
        query = urllib.parse.quote(series['title'])
        url = f"https://api.themoviedb.org/3/search/tv?api_key={TMDB_API_KEY}&query={query}"
        async with session.get(url, timeout=10) as resp:
            data = await resp.json()
            if data.get('results'):
                first = data['results'][0]
                series['rating'] = first.get('vote_average', 0.0)
                if first.get('poster_path'): series['posterUrl'] = f"https://image.tmdb.org/t/p/w500{first['poster_path']}"
                if first.get('backdrop_path'): series['backdropUrl'] = f"https://image.tmdb.org/t/p/w500{first['backdrop_path']}"
                series['description'] = first.get('overview', series['description'])
    except: pass
    return series

# ----------------------------------------------------------------------
# Main Runner
# ----------------------------------------------------------------------
async def main():
    log("🚀 Starting Aggregator v2.5...")
    if not os.path.exists(ASSETS_DIR):
        os.makedirs(ASSETS_DIR, exist_ok=True)

    async with aiohttp.ClientSession(headers={'User-Agent': USER_AGENT}) as session:
        sources = await fetch_sources(session)
        live_channels, movies, series_list = [], [], []

        for src in sources:
            log(f"🔍 {src['name']}...")
            try:
                async with session.get(src['url'], timeout=60) as resp:
                    if resp.status != 200: continue

                    if src['type'] == 'M3U':
                        live_channels.extend(parse_m3u(await resp.text(), src['category']))
                    elif src['type'] == 'JSON':
                        data = await resp.json()
                        items = parse_archive_search(data)

                        if src['category'] == 'Movies':
                            movies.extend(items)
                        elif src['category'] == 'Series':
                            for itm in items:
                                meta = await fetch_archive_metadata(session, itm['identifier'])
                                if meta:
                                    s_obj, _ = parse_archive_episodes(meta, itm)
                                    if s_obj:
                                        s_obj = await enrich_series(session, s_obj)
                                        series_list.append(s_obj)
                                        await asyncio.sleep(TMDB_DELAY)
            except Exception as e: log(f"   ⚠️ Error: {e}")

        # Mirror grouping
        grouped = {}
        for ch in live_channels:
            n = normalize_name(ch['name'])
            if n not in grouped:
                grouped[n] = {**ch, "urls": []}
                if 'url' in grouped[n]: del grouped[n]['url']
            grouped[n]['urls'].append(ch['url'])

        # Save results
        log("💾 Saving JSON files...")
        with open(os.path.join(ASSETS_DIR, 'curated_channels.json'), 'w', encoding='utf-8') as f:
            json.dump(list(grouped.values())[:MAX_CHANNELS], f, indent=2, ensure_ascii=False)

        with open(os.path.join(ASSETS_DIR, 'movies.json'), 'w', encoding='utf-8') as f:
            mv_out = [{
                "id": m['id'], "title": m['title'],
                "streamUrl": f"https://archive.org/download/{m['identifier']}/{m['identifier']}.mp4",
                "posterUrl": m['poster_url'], "description": m['description'],
                "genre": ["Movie"], "year": m['year'], "rating": 0.0
            } for m in movies[:MAX_MOVIES]]
            json.dump(mv_out, f, indent=2, ensure_ascii=False)

        with open(os.path.join(ASSETS_DIR, 'series.json'), 'w', encoding='utf-8') as f:
            json.dump(series_list[:MAX_SERIES], f, indent=2, ensure_ascii=False)

    log(f"🎉 DONE: {len(grouped)} Channels, {len(mv_out)} Movies, {len(series_list)} Series.")

if __name__ == "__main__":
    asyncio.run(main())
