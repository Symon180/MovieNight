package com.symon.movienight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.symon.movienight.data.CuratedImdbCatalog
import com.symon.movienight.data.DefaultMovieRepository
import com.symon.movienight.data.InMemoryLibraryStorage
import com.symon.movienight.data.LibrarySnapshot
import com.symon.movienight.data.LibraryStorage
import com.symon.movienight.data.MovieRepository
import com.symon.movienight.model.AnimeFormatFilter
import com.symon.movienight.model.AppTab
import com.symon.movienight.model.EraFilter
import com.symon.movienight.model.MediaType
import com.symon.movienight.model.Movie
import com.symon.movienight.model.MovieGenreFilter
import com.symon.movienight.model.UserMediaProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Rappresenta tutto ciò che lo schermo deve mostrare in un dato istante.
 */
data class MovieNightUiState(
  val isLoading: Boolean = false,
  val isLoadingMore: Boolean = false,                           // Caricamento automatico nuove pagine per il mazzo infinito
  val selectedTab: AppTab = AppTab.DISCOVER,
  val selectedMediaType: MediaType = MediaType.MOVIE,
  val selectedAnimeFormat: AnimeFormatFilter = AnimeFormatFilter.ALL, // Divisione Anime in Serie TV e Film
  val selectedGenre: MovieGenreFilter = MovieGenreFilter.ALL,
  val selectedEra: EraFilter = EraFilter.ALL,                   // Filtro Epoca / Decennio (anche classici e anni '70/'80/'90)
  val currentPage: Int = 1,                                     // Pagina corrente del catalogo infinito
  val searchQuery: String = "",
  val deck: List<Movie> = emptyList(),                          // Mazzo infinito di schede da scorrere
  val currentIndex: Int = 0,                                    // Indice della scheda visibile in cima al mazzo
  val likedMovies: List<Movie> = emptyList(),                   // Sezione "Da Vedere" (Watchlist illimitata)
  val watchedMovies: List<Movie> = emptyList(),                 // Sezione "Già Visti" (Watched)
  val skippedMovies: List<Movie> = emptyList(),                 // Schede scartate definitivamente (Swipe a Sinistra ✖️)
  val dislikedIds: Set<String> = emptySet(),                    // ID bloccati per sempre ("Non farmelo più vedere")
  val userProgress: Map<String, UserMediaProgress> = emptyMap(),// Voti personali e progressi episodi per ID
  val pickedWinner: Movie? = null,                              // Il titolo estratto a sorte per la serata!
  val showShortlistSheet: Boolean = false,                      // Mostra/nasconde il riepilogo rapido
  val selectedMediaForDetails: Movie? = null,                   // Scheda programma aperta a tutto schermo
  val isLoadingCast: Boolean = false,                           // Caricamento foto attori / personaggi live
  val selectedMediaForEpisodes: Movie? = null,                  // Serie TV / Anime aperto nel pannello Episodi & Ratings
  val isLoadingEpisodes: Boolean = false,                       // Caricamento episodi live da Jikan / IMDb
  val malUsername: String = "",                                 // Username MyAnimeList collegato
  val isSyncingMal: Boolean = false,                            // Stato caricamento import MyAnimeList
  val malSyncMessage: String? = null                            // Messaggio di conferma sincronizzazione MAL
) {
  /**
   * Il titolo attualmente in cima al mazzo (o `null` se il mazzo è temporaneamente vuoto).
   */
  val currentMovie: Movie?
    get() = deck.getOrNull(currentIndex)

  /**
   * Il prossimo titolo subito sotto la prima carta (utile per l'effetto pila 3D).
   */
  val nextMovieInDeck: Movie?
    get() = deck.getOrNull(currentIndex + 1)

  /**
   * Quante schede restano da scorrere nel mazzo attuale.
   */
  val remainingInDeck: Int
    get() = (deck.size - currentIndex).coerceAtLeast(0)

  /**
   * Totale episodi visti dall'utente in tutte le Serie TV e gli Anime.
   */
  val totalEpisodesWatched: Int
    get() = userProgress.values.sumOf { it.watchedEpisodeIds.size }
}

/**
 * Il "cervello" dell'app (ViewModel).
 * Gestisce:
 * - Le 4 direzioni di Swipe (Destra = In Lista, Su = Già Visto, Giù = Rimetti nel Pool, Sinistra = Mai più)
 * - Il **Mazzo Infinito**: quando restano poche carte, carica automaticamente nuove pagine da IMDb / MyAnimeList
 *   e ricicla le carte non scartate così il mazzo non finisce mai!
 * - La divisione degli **Anime in Serie e Film** (`AnimeFormatFilter`)
 * - Il filtro per **Epoca / Decennio** (`EraFilter`)
 * - Il caricamento delle **foto reali degli attori e personaggi** quando apri una scheda!
 */
class MovieNightViewModel(
  private val repository: MovieRepository = DefaultMovieRepository(),
  private val storage: LibraryStorage = InMemoryLibraryStorage()
) : ViewModel() {

  private val _uiState = MutableStateFlow(MovieNightUiState(isLoading = true))
  val uiState: StateFlow<MovieNightUiState> = _uiState.asStateFlow()

  init {
    val saved = storage.load()
    _uiState.update {
      it.copy(
        likedMovies = saved.watchlist,
        watchedMovies = saved.watched,
        dislikedIds = saved.dislikedIds,
        userProgress = saved.progress,
        malUsername = saved.malUsername
      )
    }
    loadCatalog(
      genre = MovieGenreFilter.ALL,
      mediaType = MediaType.MOVIE,
      animeFormat = AnimeFormatFilter.ALL,
      eraFilter = EraFilter.ALL
    )
  }

  private fun persistCurrentLibrary() {
    val state = _uiState.value
    storage.save(
      LibrarySnapshot(
        watchlist = state.likedMovies,
        watched = state.watchedMovies,
        dislikedIds = state.dislikedIds,
        progress = state.userProgress,
        malUsername = state.malUsername
      )
    )
  }

  fun selectTab(tab: AppTab) {
    _uiState.update { it.copy(selectedTab = tab) }
  }

  fun selectMediaType(mediaType: MediaType) {
    val state = _uiState.value
    if (state.selectedMediaType == mediaType && state.deck.isNotEmpty()) return
    loadCatalog(
      genre = state.selectedGenre,
      mediaType = mediaType,
      animeFormat = state.selectedAnimeFormat,
      eraFilter = state.selectedEra
    )
  }

  fun selectAnimeFormat(animeFormat: AnimeFormatFilter) {
    val state = _uiState.value
    loadCatalog(
      genre = state.selectedGenre,
      mediaType = MediaType.ANIME,
      animeFormat = animeFormat,
      eraFilter = state.selectedEra
    )
  }

  fun selectGenre(genre: MovieGenreFilter) {
    val state = _uiState.value
    loadCatalog(
      genre = genre,
      mediaType = state.selectedMediaType,
      animeFormat = state.selectedAnimeFormat,
      eraFilter = state.selectedEra
    )
  }

  fun selectEra(eraFilter: EraFilter) {
    val state = _uiState.value
    loadCatalog(
      genre = state.selectedGenre,
      mediaType = state.selectedMediaType,
      animeFormat = state.selectedAnimeFormat,
      eraFilter = eraFilter
    )
  }

  private fun loadCatalog(
    genre: MovieGenreFilter,
    mediaType: MediaType,
    animeFormat: AnimeFormatFilter,
    eraFilter: EraFilter
  ) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isLoading = true,
          selectedGenre = genre,
          selectedMediaType = mediaType,
          selectedAnimeFormat = animeFormat,
          selectedEra = eraFilter,
          currentPage = 1,
          searchQuery = "",
          currentIndex = 0
        )
      }
      val rawItems = repository.getCatalog(
        genreFilter = genre,
        mediaType = mediaType,
        animeFormat = animeFormat,
        eraFilter = eraFilter,
        page = 1
      )
      val state = _uiState.value
      val excludedIds = state.dislikedIds +
        state.likedMovies.map { it.id }.toSet() +
        state.watchedMovies.map { it.id }.toSet()

      val filteredDeck = rawItems.filterNot { it.id in excludedIds }
      _uiState.update {
        it.copy(
          isLoading = false,
          deck = filteredDeck,
          currentIndex = 0
        )
      }
    }
  }

  /**
   * Garantisce che il mazzo sia INFINITO:
   * quando restano <= 5 carte (o l'utente è arrivato in fondo), scarica automaticamente
   * la pagina successiva dal database online e, se necessario, rimette in coda le carte
   * non bloccate così non si rimane mai senza schede da scorrere!
   */
  fun loadMoreDeck() {
    val state = _uiState.value
    if (state.isLoading || state.isLoadingMore) return
    val nextPage = state.currentPage + 1

    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingMore = true, currentPage = nextPage) }
      val moreItems = repository.getCatalog(
        genreFilter = state.selectedGenre,
        mediaType = state.selectedMediaType,
        animeFormat = state.selectedAnimeFormat,
        eraFilter = state.selectedEra,
        page = nextPage
      )

      _uiState.update { current ->
        val excludedIds = current.dislikedIds +
          current.likedMovies.map { it.id }.toSet() +
          current.watchedMovies.map { it.id }.toSet()

        val activeRemainingIds = current.deck.drop(current.currentIndex).map { it.id }.toSet()
        val freshItems = moreItems.filterNot { it.id in excludedIds || it.id in activeRemainingIds }

        val updatedDeck = if (freshItems.isNotEmpty()) {
          current.deck + freshItems
        } else {
          // Se siamo offline o abbiamo già visto tutte le pagine API di questa combinazione di filtri,
          // ricicliamo i titoli validi non scartati così il mazzo continua all'infinito!
          val recyclable = current.deck
            .take(current.currentIndex)
            .filterNot { it.id in excludedIds }
            .distinctBy { it.id }
            .shuffled()
          current.deck + recyclable
        }

        current.copy(
          isLoadingMore = false,
          deck = updatedDeck
        )
      }
    }
  }

  private fun checkAndLoadMoreIfNeeded() {
    val state = _uiState.value
    if (state.remainingInDeck <= 4 && state.searchQuery.isBlank()) {
      loadMoreDeck()
    }
  }

  fun searchMovies(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    val state = _uiState.value
    if (query.isBlank()) {
      loadCatalog(
        genre = state.selectedGenre,
        mediaType = state.selectedMediaType,
        animeFormat = state.selectedAnimeFormat,
        eraFilter = state.selectedEra
      )
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val results = repository.searchLive(query, state.selectedMediaType)
      _uiState.update {
        it.copy(
          isLoading = false,
          deck = results,
          currentIndex = 0
        )
      }
    }
  }

  /**
   * 1. SWIPE A DESTRA (➡️ / Cuore ❤️): "Mettiamolo in lista (Da Vedere)"
   */
  fun likeCurrentMovie() {
    val state = _uiState.value
    val movie = state.currentMovie ?: return
    val updatedLikes = if (state.likedMovies.any { it.id == movie.id }) {
      state.likedMovies
    } else {
      state.likedMovies + movie
    }
    _uiState.update {
      it.copy(
        likedMovies = updatedLikes,
        currentIndex = it.currentIndex + 1
      )
    }
    persistCurrentLibrary()
    checkAndLoadMoreIfNeeded()
  }

  /**
   * 2. SWIPE VERSO L'ALTO (⬆️ / Già Visto ✅): "L'ho già visto"
   */
  fun markCurrentMovieWatched(userRating: Int? = null) {
    val state = _uiState.value
    val movie = state.currentMovie ?: return
    markAsWatched(movie, userRating)
    _uiState.update {
      it.copy(currentIndex = it.currentIndex + 1)
    }
    checkAndLoadMoreIfNeeded()
  }

  /**
   * 3. SWIPE VERSO IL BASSO (⬇️ / Rimetti nel pool 🔄): "Rimettilo nel pool"
   */
  fun putBackInPool() {
    val state = _uiState.value
    val movie = state.currentMovie ?: return
    _uiState.update {
      it.copy(
        deck = it.deck + movie,
        currentIndex = it.currentIndex + 1
      )
    }
    checkAndLoadMoreIfNeeded()
  }

  /**
   * 4. SWIPE A SINISTRA (⬅️ / X ✖️): "Non farmelo più vedere"
   */
  fun skipCurrentMovie() {
    val state = _uiState.value
    val movie = state.currentMovie ?: return
    _uiState.update {
      it.copy(
        skippedMovies = it.skippedMovies + movie,
        dislikedIds = it.dislikedIds + movie.id,
        currentIndex = it.currentIndex + 1
      )
    }
    persistCurrentLibrary()
    checkAndLoadMoreIfNeeded()
  }

  /**
   * Apre la scheda dettagliata del programma e carica anche le foto reali degli attori / personaggi!
   */
  fun openProgramDetails(movie: Movie) {
    _uiState.update {
      it.copy(
        selectedMediaForDetails = movie,
        isLoadingCast = true
      )
    }
    viewModelScope.launch {
      val enrichedCast = repository.fetchCastForMedia(movie)
      if (enrichedCast.isNotEmpty()) {
        val updatedMovie = movie.copy(castMembers = enrichedCast)
        _uiState.update { state ->
          if (state.selectedMediaForDetails?.id == movie.id) {
            state.copy(
              selectedMediaForDetails = updatedMovie,
              isLoadingCast = false
            )
          } else {
            state.copy(isLoadingCast = false)
          }
        }
      } else {
        _uiState.update { it.copy(isLoadingCast = false) }
      }
    }
  }

  fun closeProgramDetails() {
    _uiState.update { it.copy(selectedMediaForDetails = null, isLoadingCast = false) }
  }

  /**
   * Aggiunge direttamente un programma (es. dai "Programmi simili" o dalla scheda dettaglio) alla lista "Da Vedere".
   */
  fun addToWatchlist(movie: Movie) {
    _uiState.update { state ->
      val updatedLikes = if (state.likedMovies.any { it.id == movie.id }) {
        state.likedMovies
      } else {
        listOf(movie) + state.likedMovies
      }
      state.copy(
        likedMovies = updatedLikes,
        dislikedIds = state.dislikedIds - movie.id
      )
    }
    persistCurrentLibrary()
  }

  /**
   * Sposta o aggiunge un titolo nella sezione "Già Visti" (Watched).
   */
  fun markAsWatched(movie: Movie, userRating: Int? = null) {
    _uiState.update { state ->
      val updatedWatchlist = state.likedMovies.filterNot { it.id == movie.id }
      val updatedWatched = if (state.watchedMovies.any { it.id == movie.id }) {
        state.watchedMovies.map { if (it.id == movie.id) movie else it }
      } else {
        listOf(movie) + state.watchedMovies
      }
      val existingProg = state.userProgress[movie.id] ?: UserMediaProgress(mediaId = movie.id)
      val newProg = if (userRating != null) {
        existingProg.copy(userRating = userRating.coerceIn(1, 10))
      } else {
        existingProg
      }
      state.copy(
        likedMovies = updatedWatchlist,
        watchedMovies = updatedWatched,
        dislikedIds = state.dislikedIds - movie.id,
        userProgress = state.userProgress + (movie.id to newProg)
      )
    }
    persistCurrentLibrary()
  }

  fun moveBackToWatchlist(movie: Movie) {
    _uiState.update { state ->
      val updatedWatched = state.watchedMovies.filterNot { it.id == movie.id }
      val updatedWatchlist = if (state.likedMovies.any { it.id == movie.id }) {
        state.likedMovies
      } else {
        listOf(movie) + state.likedMovies
      }
      state.copy(
        likedMovies = updatedWatchlist,
        watchedMovies = updatedWatched
      )
    }
    persistCurrentLibrary()
  }

  fun removeFromWatched(movie: Movie) {
    _uiState.update { state ->
      state.copy(watchedMovies = state.watchedMovies.filterNot { it.id == movie.id })
    }
    persistCurrentLibrary()
  }

  fun rateMedia(mediaId: String, rating: Int) {
    val cleanRating = rating.coerceIn(1, 10)
    _uiState.update { state ->
      val currentProg = state.userProgress[mediaId] ?: UserMediaProgress(mediaId = mediaId)
      state.copy(
        userProgress = state.userProgress + (mediaId to currentProg.copy(userRating = cleanRating))
      )
    }
    persistCurrentLibrary()
  }

  fun openEpisodeTracker(movie: Movie) {
    val enriched = CuratedImdbCatalog.ensureFullEpisodeMode(movie)
    _uiState.update {
      it.copy(
        selectedMediaForEpisodes = enriched,
        isLoadingEpisodes = false
      )
    }
    viewModelScope.launch {
      val loadedEpisodes = repository.fetchEpisodesForMedia(enriched)
      val updatedMovie = enriched.copy(episodes = loadedEpisodes)
      _uiState.update { state ->
        state.copy(
          isLoadingEpisodes = false,
          selectedMediaForEpisodes = updatedMovie,
          deck = state.deck.map { if (it.id == movie.id) updatedMovie else it },
          likedMovies = state.likedMovies.map { if (it.id == movie.id) updatedMovie else it },
          watchedMovies = state.watchedMovies.map { if (it.id == movie.id) updatedMovie else it }
        )
      }
      persistCurrentLibrary()
    }
  }

  fun closeEpisodeTracker() {
    _uiState.update { it.copy(selectedMediaForEpisodes = null, isLoadingEpisodes = false) }
  }

  fun toggleEpisodeWatched(media: Movie, episodeId: String) {
    _uiState.update { state ->
      val currentProg = state.userProgress[media.id] ?: UserMediaProgress(mediaId = media.id)
      val updatedSet = if (episodeId in currentProg.watchedEpisodeIds) {
        currentProg.watchedEpisodeIds - episodeId
      } else {
        currentProg.watchedEpisodeIds + episodeId
      }
      val newProg = currentProg.copy(watchedEpisodeIds = updatedSet)

      val updatedWatchedList = if (updatedSet.isNotEmpty() && state.watchedMovies.none { it.id == media.id }) {
        listOf(media) + state.watchedMovies
      } else {
        state.watchedMovies
      }

      state.copy(
        watchedMovies = updatedWatchedList,
        userProgress = state.userProgress + (media.id to newProg)
      )
    }
    persistCurrentLibrary()
  }

  fun rateEpisode(media: Movie, episodeId: String, rating: Int) {
    val cleanRating = rating.coerceIn(1, 10)
    _uiState.update { state ->
      val currentProg = state.userProgress[media.id] ?: UserMediaProgress(mediaId = media.id)
      val updatedWatchedSet = currentProg.watchedEpisodeIds + episodeId
      val updatedEpRatings = currentProg.episodeRatings + (episodeId to cleanRating)
      val autoMediaRating = currentProg.userRating ?: updatedEpRatings.values.average().toInt().coerceIn(1, 10)
      val newProg = currentProg.copy(
        userRating = autoMediaRating,
        watchedEpisodeIds = updatedWatchedSet,
        episodeRatings = updatedEpRatings
      )

      val updatedWatchedList = if (state.watchedMovies.none { it.id == media.id }) {
        listOf(media) + state.watchedMovies
      } else {
        state.watchedMovies
      }

      state.copy(
        watchedMovies = updatedWatchedList,
        userProgress = state.userProgress + (media.id to newProg)
      )
    }
    persistCurrentLibrary()
  }

  fun importFromMyAnimeList(username: String) {
    val cleanUser = username.trim()
    if (cleanUser.isEmpty()) return

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isSyncingMal = true,
          malUsername = cleanUser,
          malSyncMessage = null
        )
      }
      val result = repository.importMyAnimeListProfile(cleanUser)
      _uiState.update { state ->
        var updatedWatchlist = state.likedMovies
        var updatedWatched = state.watchedMovies
        val updatedProgress = state.userProgress.toMutableMap()

        for (entry in result.entries) {
          val anime = entry.movie
          if (entry.isWatched) {
            if (updatedWatched.none { it.id == anime.id }) {
              updatedWatched = updatedWatched + anime
            }
            val existing = updatedProgress[anime.id] ?: UserMediaProgress(mediaId = anime.id)
            val watchedEpIds = anime.episodes
              .take(entry.watchedEpisodesCount.coerceAtLeast(1))
              .map { it.id }
              .toSet()
            updatedProgress[anime.id] = existing.copy(
              userRating = entry.userScore ?: existing.userRating,
              watchedEpisodeIds = existing.watchedEpisodeIds + watchedEpIds
            )
          } else {
            if (updatedWatchlist.none { it.id == anime.id } && updatedWatched.none { it.id == anime.id }) {
              updatedWatchlist = updatedWatchlist + anime
            }
          }
        }

        state.copy(
          isSyncingMal = false,
          malUsername = cleanUser,
          malSyncMessage = result.message,
          likedMovies = updatedWatchlist,
          watchedMovies = updatedWatched,
          userProgress = updatedProgress
        )
      }
      persistCurrentLibrary()
    }
  }

  fun dismissMalSyncMessage() {
    _uiState.update { it.copy(malSyncMessage = null) }
  }

  fun pickRandomForTonight() {
    val state = _uiState.value
    val pool = if (state.likedMovies.isNotEmpty()) {
      state.likedMovies
    } else {
      state.deck
    }
    if (pool.isEmpty()) return
    val winner = pool.random()
    _uiState.update { it.copy(pickedWinner = winner) }
  }

  fun chooseMovieDirectly(movie: Movie) {
    _uiState.update {
      it.copy(
        pickedWinner = movie,
        showShortlistSheet = false
      )
    }
  }

  fun removeFromLiked(movie: Movie) {
    _uiState.update {
      it.copy(likedMovies = it.likedMovies.filterNot { item -> item.id == movie.id })
    }
    persistCurrentLibrary()
  }

  fun dismissWinner() {
    _uiState.update { it.copy(pickedWinner = null) }
  }

  fun toggleShortlistSheet(show: Boolean) {
    _uiState.update { it.copy(showShortlistSheet = show) }
  }

  /**
   * Rimescola il mazzo (e se il mazzo era finito a causa degli scarti, ricarica il catalogo).
   */
  fun reshuffleDeck() {
    val state = _uiState.value
    if (state.deck.isEmpty()) {
      _uiState.update { it.copy(dislikedIds = emptySet(), skippedMovies = emptyList()) }
      persistCurrentLibrary()
      loadCatalog(
        genre = state.selectedGenre,
        mediaType = state.selectedMediaType,
        animeFormat = state.selectedAnimeFormat,
        eraFilter = state.selectedEra
      )
    } else {
      _uiState.update {
        it.copy(
          deck = it.deck.shuffled(),
          currentIndex = 0
        )
      }
      loadMoreDeck()
    }
  }
}
