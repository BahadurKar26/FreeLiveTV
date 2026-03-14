package com.rancon.freelivetv.data

object Config {
    // Updated to user's new repository (QC-009)
    const val BASE_GITHUB_URL = "https://raw.githubusercontent.com/BahadurKar26/FreeLiveTV/main"
    
    const val CHANNELS_URL = "$BASE_GITHUB_URL/app/src/main/assets/curated_channels.json"
    const val MOVIES_URL = "$BASE_GITHUB_URL/app/src/main/assets/movies.json"
    const val SERIES_URL = "$BASE_GITHUB_URL/app/src/main/assets/series.json"
    const val SOURCES_URL = "$BASE_GITHUB_URL/app/src/main/assets/sources.json"
    
    const val EPG_URL = "https://iptv-org.github.io/epg/guides.xml"
    
    // Archive.org Queries
    const val ARCHIVE_SERIES_URL = "https://archive.org/advancedsearch.php?q=collection:(tv_series)&fl[]=identifier,title,description,year,rating&sort[]=downloads+desc&rows=50&output=json"
    const val ARCHIVE_MOVIES_URL = "https://archive.org/advancedsearch.php?q=collection:(feature_films)&fl[]=identifier,title,description,year,rating&sort[]=downloads+desc&rows=50&output=json"
    
    // TMDB (Phase 2 enrichment)
    const val TMDB_API_KEY = "90c68a4baddabebeffc50f29fd6774f6"
    const val TMDB_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI5MGM2OGE0YmFkZGFiZWJlZmZjNTBmMjlmZDY3NzRmNiIsIm5iZiI6MTc3MzM4NzYyMi44MDMsInN1YiI6IjY5YjNiZjY2NTM0OTI2MjJkNjRhNmRlNCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.1XxTDYF1QUC-Nnhekm4dL3oQ3g0UUsK9yrA-B1xr-2A"
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3"
    
    // Cleanup & Sync Constants
    const val INACTIVE_THRESHOLD_DAYS = 7
    const val SYNC_INTERVAL_HOURS = 12
}
