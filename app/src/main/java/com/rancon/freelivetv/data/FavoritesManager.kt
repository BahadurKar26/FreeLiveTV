package com.rancon.freelivetv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

class FavoritesManager(private val context: Context) {

    companion object {
        private const val FAVORITE_PREFIX = "favorite_"
    }

    suspend fun addFavorite(id: String, type: String) {
        context.dataStore.edit { preferences ->
            val key = stringPreferencesKey("$FAVORITE_PREFIX$id")
            preferences[key] = type
        }
    }

    suspend fun removeFavorite(id: String) {
        context.dataStore.edit { preferences ->
            val key = stringPreferencesKey("$FAVORITE_PREFIX$id")
            preferences.remove(key)
        }
    }

    fun isFavorite(id: String): Flow<Boolean> {
        val key = stringPreferencesKey("$FAVORITE_PREFIX$id")
        return context.dataStore.data.map { preferences ->
            preferences.contains(key)
        }
    }

    fun isFavoriteSync(id: String): Boolean {
        return runBlocking {
            isFavorite(id).firstOrNull() ?: false
        }
    }

    fun getFavoriteType(id: String): Flow<String?> {
        val key = stringPreferencesKey("$FAVORITE_PREFIX$id")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    fun getAllFavorites(): Flow<Map<String, String>> {
        return context.dataStore.data.map { preferences ->
            preferences.asMap()
                .filterKeys { it.name.startsWith(FAVORITE_PREFIX) }
                .mapKeys { it.key.name.removePrefix(FAVORITE_PREFIX) }
                .mapValues { it.value as String }
        }
    }

    fun getFavoritesByType(type: String): Flow<List<String>> {
        return getAllFavorites().map { favorites ->
            favorites.filter { it.value == type }.keys.toList()
        }
    }

    suspend fun toggleFavorite(id: String, type: String): Boolean {
        var isNowFavorite = false
        context.dataStore.edit { preferences ->
            val key = stringPreferencesKey("$FAVORITE_PREFIX$id")
            if (preferences.contains(key)) {
                preferences.remove(key)
                isNowFavorite = false
            } else {
                preferences[key] = type
                isNowFavorite = true
            }
        }
        return isNowFavorite
    }

    suspend fun clearAllFavorites() {
        context.dataStore.edit { preferences ->
            val keysToRemove = preferences.asMap().keys
                .filter { it.name.startsWith(FAVORITE_PREFIX) }
            keysToRemove.forEach { key ->
                preferences.remove(key)
            }
        }
    }

    fun getFavoritesCount(): Flow<Int> {
        return getAllFavorites().map { it.size }
    }
}