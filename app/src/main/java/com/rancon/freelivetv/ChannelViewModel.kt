package com.rancon.freelivetv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rancon.freelivetv.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

class ChannelViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val channelRepository = ChannelRepository(application, viewModelScope)
    private val moviesRepository = MoviesRepository(application, viewModelScope)
    private val seriesRepository = SeriesRepository(application, viewModelScope)
    private val discoveryRepository = DiscoveryRepository(application)
    private val recommendationRepository = RecommendationRepository(application)
    private val userDataManager = UserDataManager(application)
    private val interactionDao = database.interactionDao()
    private val programDao = database.programDao()
    private val reminderDao = database.reminderDao()

    // Elite Review 3: Added debounce to prevent UI flicker during heavy syncs
    val channels = channelRepository.allChannels
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val banglaChannels = channelRepository.banglaChannels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val globalChannels = channelRepository.globalChannels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val newsChannels = channelRepository.newsChannels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sportsChannels = channelRepository.sportsChannels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val movieChannels = channelRepository.movieChannels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val entertainmentChannels = channelRepository.entertainmentChannels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val kidsChannels = channelRepository.kidsChannels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val inactiveChannels = channelRepository.inactiveChannels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = channelRepository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _networkType = MutableStateFlow(NetworkUtils.getNetworkTypeName(application))
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    fun updateNetworkState() {
        _networkType.value = NetworkUtils.getNetworkTypeName(getApplication())
    }

    val homeMostWatched = combine(channels, banglaChannels, globalChannels) { all, bangla, global ->
        val mostWatchedIds = all.sortedByDescending { it.lastPlayedTime }.map { it.id }.take(15)
        if (mostWatchedIds.isNotEmpty()) {
            val allDistinct = (bangla + global).distinctBy { it.id }
            mostWatchedIds.mapNotNull { id -> allDistinct.find { it.id == id } }
        } else {
            (bangla + global).distinctBy { it.id }.take(15)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeRecommended = combine(banglaChannels, globalChannels) { bangla, global ->
        val allDistinct = (bangla + global).distinctBy { it.id }
        if (allDistinct.isEmpty()) return@combine emptyList<Channel>()
        
        val seed = allDistinct.size + allDistinct.sumOf { it.name.hashCode() }
        val random = Random(seed.toLong())
        allDistinct.shuffled(random).take(15)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movies = moviesRepository.allMovies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteMovies = moviesRepository.favoriteMovies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val series = seriesRepository.allSeries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching = combine(movies, series, userDataManager.allProgress) { moviesList, seriesList, progressMap ->
        val list = mutableListOf<ContinueWatchingItem>()
        moviesList.forEach { movie ->
            val progress = progressMap[movie.id] ?: 0L
            if (progress > 0) list.add(ContinueWatchingItem(movie.id, movie.title, "movie", movie.posterUrl, progress))
        }
        seriesList.forEach { s ->
            s.seasons.forEach { season ->
                season.episodes.forEach { episode ->
                    val progress = progressMap[episode.id] ?: 0L
                    if (progress > 0) list.add(ContinueWatchingItem(episode.id, "${s.title} - ${episode.getEpisodeDisplay()}", "series", s.posterUrl, progress))
                }
            }
        }
        list.sortedByDescending { it.progress }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class ContinueWatchingItem(val id: String, val title: String, val type: String, val posterUrl: String, val progress: Long)

    val categoryAffinities = interactionDao.getCategoryAffinities().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recommendedChannels = recommendationRepository.getRecommendedChannels().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recommendedMovies = recommendationRepository.getRecommendedMovies().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentSearches = interactionDao.getRecentInteractions(50)
        .map { list -> list.filter { it.actionType == "SEARCH" }.mapNotNull { it.searchQuery }.distinct().take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadContent(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (forceRefresh) {
                    channelRepository.refreshChannels()
                    moviesRepository.refreshMovies()
                    seriesRepository.refreshSeries()
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load content: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadChannels() { loadContent(forceRefresh = true) }

    fun startDiscovery() {
        viewModelScope.launch {
            _isLoading.value = true
            discoveryRepository.performDiscovery()
            discoveryRepository.selfHeal()
            _isLoading.value = false
        }
    }

    fun getCurrentProgram(channelId: String): Flow<Program?> = flow {
        while(true) {
            emit(programDao.getCurrentProgram(channelId, System.currentTimeMillis()))
            kotlinx.coroutines.delay(60000)
        }
    }

    fun getProgramsForChannel(channelId: String): Flow<List<Program>> = programDao.getProgramsForChannel(channelId)

    suspend fun hasReminder(programId: Long): Boolean = reminderDao.hasReminder(programId)

    fun toggleReminder(program: Program) {
        viewModelScope.launch {
            if (reminderDao.hasReminder(program.id)) {
                reminderDao.deleteByProgramId(program.id)
            } else {
                reminderDao.insert(Reminder(programId = program.id, channelId = program.channelId, programTitle = program.title, startTime = program.startTime))
            }
        }
    }

    fun recordInteraction(contentId: String, contentType: String, actionType: String, duration: Long = 0, searchQuery: String? = null, category: String? = null) {
        viewModelScope.launch {
            interactionDao.insert(Interaction(contentId = contentId, contentType = contentType, actionType = actionType, duration = duration, timestamp = System.currentTimeMillis(), searchQuery = searchQuery, category = category))
        }
    }

    fun toggleFavorite(id: String) { viewModelScope.launch { channelRepository.toggleFavorite(id) } }
    fun toggleFavorite(id: String, type: String) {
        viewModelScope.launch { 
            channelRepository.toggleFavorite(id)
            recordInteraction(id, type, "FAVORITE")
        }
    }

    fun isFavorite(id: String): Boolean {
        return channels.value.find { it.id == id }?.isFavorite 
            ?: movies.value.find { it.id == id }?.isFavorite 
            ?: series.value.find { it.id == id }?.isFavorite 
            ?: false
    }

    fun reportChannelFailure(channelId: String) { viewModelScope.launch { channelRepository.reportFailure(channelId) } }
    fun reportChannelSuccess(channelId: String) { viewModelScope.launch { channelRepository.reportSuccess(channelId) } }

    fun recordWatch(id: String) {
        viewModelScope.launch { 
            channelRepository.recordWatch(id)
            val channel = channels.value.find { it.id == id }
            recordInteraction(id, "LIVE", "WATCH", category = channel?.category)
        }
    }
    
    fun recordWatch(id: String, duration: Long) {
        viewModelScope.launch { 
            channelRepository.recordWatch(id)
            val channel = channels.value.find { it.id == id }
            recordInteraction(id, "LIVE", "WATCH", duration = duration, category = channel?.category)
        }
    }

    fun getProgress(id: String): Long = userDataManager.getProgress(id)
    fun saveProgress(id: String, position: Long) { userDataManager.saveProgress(id, position) }

    fun searchChannels(query: String): Flow<List<Channel>> = channelRepository.searchChannels(query)

    fun searchMovies(query: String): List<Movie> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()
        return movies.value.filter { it.title.lowercase().contains(lowerQuery) || it.genre.any { g -> g.lowercase().contains(lowerQuery) } }
    }

    fun searchSeries(query: String): List<Series> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()
        return series.value.filter { it.title.lowercase().contains(lowerQuery) || it.genre.any { g -> g.lowercase().contains(lowerQuery) } }
    }

    fun resetAllData() {
        viewModelScope.launch {
            userDataManager.resetAll()
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                channelRepository.refreshChannels()
                moviesRepository.refreshMovies()
                seriesRepository.refreshSeries()
            }
        }
    }

    fun getSeriesById(seriesId: String): Series? = series.value.find { it.id == seriesId }

    suspend fun getRecommendationsForTopCategory(): Pair<String, List<Channel>> = recommendationRepository.getRecommendationsForTopCategory()

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChannelViewModel::class.java)) {
                    return ChannelViewModel(FreeLiveTVApp.instance) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
