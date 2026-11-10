package com.symon.movienight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.symon.movienight.data.DefaultMovieRepository
import com.symon.movienight.data.InMemoryLibraryStorage
import com.symon.movienight.data.LibrarySnapshot
import com.symon.movienight.data.LibraryStorage
import com.symon.movienight.data.MovieRepository
import com.symon.movienight.model.AppTab
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
  val selectedTab: AppTab = AppTab.DISCOVER,
  val selectedMediaType: MediaType = MediaType.MOVIE,
  val selectedGenre: MovieGenreFilter = MovieGenreFilter.ALL,
  val searchQuery: String = "",
  val deck: List<Movie> = emptyList(),                          // Mazzo di schede da scorrere
  val currentIndex: Int = 0,                                    // Indice della scheda visibile in cima al mazzo
  val likedMovies: List<Movie> = emptyList(),                   // Sezione "Da Vedere" (Watchlist)
  val watchedMovies: List<Movie> = emptyList(),                 // Sezione "Già Visti" (Watched)
  val skippedMovies: List<Movie> = emptyList(),                 // Schede scartate nella sessione corrente
  val userProgress: Map<String, UserMediaProgress> = emptyMap(),// Voti personali e progressi episodi per ID
  val pickedWinner: Movie? = null,                              // Il titolo estratto a sorte per la serata!
  val showShortlistSheet: Boolean = false,                      // Mostra/nasconde il riepilogo rapido
  val selectedMediaForEpisodes: Movie? = null,                  // Serie TV / Anime aperto nel pannello Episodi & Ratings
  val isLoadingEpisodes: Boolean = false,                       // Caricamento episodi live da Jikan / IMDb
  val malUsername: String = "",                                 // Username MyAnimeList collegato
  val isSyncingMal: Boolean = false,                            // Stato caricamento import MyAnimeList
  val malSyncMessage: String? = null                            // Messaggio di conferma sincronizzazione MAL
) {
  /**
   * Il titolo attualmente in cima al mazzo (o `null` se il mazzo è finito).
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
 * - Lo scorrimento delle schede (Film, Serie TV, Anime)
 * - La sezione "Da Vedere" (Watchlist) e la sezione "Già Visti" (Watched)
 * - I voti personali ai titoli e i Ratings/Spunte dei singoli episodi
 * - Il collegamento e l'importazione da MyAnimeList (MAL)
 * - Il salvataggio automatico su memoria locale tramite `LibraryStorage`
 */
class MovieNightViewModel(
  private val repository: MovieRepository = DefaultMovieRepository(),
  private val storage: LibraryStorage = InMemoryLibraryStorage()
) : ViewModel() {

  private val _uiState = MutableStateFlow(MovieNightUiState(isLoading = true))
  val uiState: StateFlow<MovieNightUiState> = _uiState.asStateFlow()

  init {
    // 1. Ricarichiamo i film/serie/anime salvati sul telefono ("Da vedere", "Già visti", voti, episodi)
    val saved = storage.load()
    _uiState.update {
      it.copy(
        likedMovies = saved.watchlist,
        watchedMovies = saved.watched,
        userProgress = saved.progress,
        malUsername = saved.malUsername
      )
    }
    // 2. Carichiamo il catalogo iniziale
    loadCatalog(MovieGenreFilter.ALL, MediaType.MOVIE)
  }

  private fun persistCurrentLibrary() {
    val state = _uiState.value
    storage.save(
      LibrarySnapshot(
        watchlist = state.likedMovies,
        watched = state.watchedMovies,
        progress = state.userProgress,
        malUsername = state.malUsername
      )
    )
  }

  /**
   * Cambia la sezione attiva nella barra in basso (Scopri / Da Vedere / Già Visti).
   */
  fun selectTab(tab: AppTab) {
    _uiState.update { it.copy(selectedTab = tab) }
  }

  /**
   * Cambia il tipo di contenuto (Film 🎬, Serie TV 📺, Anime ⛩️) e aggiorna il mazzo.
   */
  fun selectMediaType(mediaType: MediaType) {
    if (_uiState.value.selectedMediaType == mediaType && _uiState.value.deck.isNotEmpty()) return
    loadCatalog(_uiState.value.selectedGenre, mediaType)
  }

  /**
   * Carica il mazzo applicando il filtro di genere scelto.
   */
  fun selectGenre(genre: MovieGenreFilter) {
    loadCatalog(genre, _uiState.value.selectedMediaType)
  }

  private fun loadCatalog(genre: MovieGenreFilter, mediaType: MediaType) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isLoading = true,
          selectedGenre = genre,
          selectedMediaType = mediaType,
          searchQuery = "",
          currentIndex = 0
        )
      }
      val items = repository.getCatalog(genre, mediaType)
      _uiState.update {
        it.copy(
          isLoading = false,
          deck = items,
          currentIndex = 0
        )
      }
    }
  }

  /**
   * Cerca un titolo per nome interrogando IMDb (per Film/Serie) o MyAnimeList Jikan (per Anime).
   */
  fun searchMovies(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    val currentType = _uiState.value.selectedMediaType
    if (query.isBlank()) {
      loadCatalog(_uiState.value.selectedGenre, currentType)
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val results = repository.searchLive(query, currentType)
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
   * Azione "Mi ispira / Da Vedere" (Swipe a Destra o pulsante ❤️):
   * aggiunge il titolo corrente alla lista "Da Vedere" (`likedMovies`) e passa al successivo.
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
  }

  /**
   * Azione "Già Visto ✅" direttamente dalla scheda Swipe:
   * segna il titolo corrente come già visto (rimuovendolo da "Da Vedere" se presente)
   * e passa subito alla scheda successiva!
   */
  fun markCurrentMovieWatched(userRating: Int? = null) {
    val state = _uiState.value
    val movie = state.currentMovie ?: return
    markAsWatched(movie, userRating)
    _uiState.update {
      it.copy(currentIndex = it.currentIndex + 1)
    }
  }

  /**
   * Sposta o aggiunge un titolo nella sezione "Già Visti" (Watched),
   * salvando anche l'eventuale voto personale (1..10 ⭐).
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
        userProgress = state.userProgress + (movie.id to newProg)
      )
    }
    persistCurrentLibrary()
  }

  /**
   * Riporta un titolo da "Già Visti" alla lista "Da Vedere" (Watchlist).
   */
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

  /**
   * Rimuove un titolo dalla lista "Già Visti".
   */
  fun removeFromWatched(movie: Movie) {
    _uiState.update { state ->
      state.copy(watchedMovies = state.watchedMovies.filterNot { it.id == movie.id })
    }
    persistCurrentLibrary()
  }

  /**
   * Assegna o modifica il voto personale complessivo (da 1 a 10 stelle ⭐) a un Film, Serie TV o Anime.
   */
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

  /**
   * Apre il pannello "Episodi & Ratings" per una Serie TV o un Anime.
   * Se la serie/anime non ha ancora gli episodi caricati, li richiede al Repository (es. Jikan API).
   */
  fun openEpisodeTracker(movie: Movie) {
    _uiState.update {
      it.copy(
        selectedMediaForEpisodes = movie,
        isLoadingEpisodes = movie.episodes.isEmpty()
      )
    }
    if (movie.episodes.isEmpty()) {
      viewModelScope.launch {
        val loadedEpisodes = repository.fetchEpisodesForMedia(movie)
        val enrichedMovie = movie.copy(episodes = loadedEpisodes)
        _uiState.update { state ->
          state.copy(
            isLoadingEpisodes = false,
            selectedMediaForEpisodes = enrichedMovie,
            deck = state.deck.map { if (it.id == movie.id) enrichedMovie else it },
            likedMovies = state.likedMovies.map { if (it.id == movie.id) enrichedMovie else it },
            watchedMovies = state.watchedMovies.map { if (it.id == movie.id) enrichedMovie else it }
          )
        }
        persistCurrentLibrary()
      }
    }
  }

  /**
   * Chiude il pannello "Episodi & Ratings".
   */
  fun closeEpisodeTracker() {
    _uiState.update { it.copy(selectedMediaForEpisodes = null, isLoadingEpisodes = false) }
  }

  /**
   * Spunta o deseleziona un singolo episodio come "Visto" (☑️).
   * Se l'utente inizia a vedere episodi di una Serie TV o Anime, l'opera viene assicurata
   * nella sezione "Già Visti & Episodi" così può seguirne l'avanzamento!
   */
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

  /**
   * Assegna il voto personale dell'utente (da 1 a 10 ⭐) a un singolo episodio!
   * Quando voti un episodio, viene anche segnato automaticamente come "Visto".
   */
  fun rateEpisode(media: Movie, episodeId: String, rating: Int) {
    val cleanRating = rating.coerceIn(1, 10)
    _uiState.update { state ->
      val currentProg = state.userProgress[media.id] ?: UserMediaProgress(mediaId = media.id)
      val updatedWatchedSet = currentProg.watchedEpisodeIds + episodeId
      val updatedEpRatings = currentProg.episodeRatings + (episodeId to cleanRating)
      // Calcoliamo anche la media automatica dei voti dati agli episodi se il titolo non ha ancora un voto globale
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

  /**
   * Importa la lista anime da un profilo MyAnimeList (MAL) tramite username.
   */
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

  /**
   * Azione "Salta" (Swipe a Sinistra o pulsante ✖️):
   * scarta il titolo corrente e passa al successivo.
   */
  fun skipCurrentMovie() {
    val state = _uiState.value
    val movie = state.currentMovie ?: return
    _uiState.update {
      it.copy(
        skippedMovies = it.skippedMovies + movie,
        currentIndex = it.currentIndex + 1
      )
    }
  }

  /**
   * Pulsante "Scegli per me a caso!" (🎲):
   * - Se l'utente ha già messo almeno 1 titolo nella lista "Da Vedere", pesca a sorte tra quelli!
   * - Altrimenti pesca a sorte direttamente dal mazzo corrente filtrato.
   */
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

  /**
   * Permette di scegliere direttamente un titolo specifico dalla lista "Da Vedere".
   */
  fun chooseMovieDirectly(movie: Movie) {
    _uiState.update {
      it.copy(
        pickedWinner = movie,
        showShortlistSheet = false
      )
    }
  }

  /**
   * Rimuove un titolo dalla lista "Da Vedere".
   */
  fun removeFromLiked(movie: Movie) {
    _uiState.update {
      it.copy(likedMovies = it.likedMovies.filterNot { item -> item.id == movie.id })
    }
    persistCurrentLibrary()
  }

  /**
   * Chiude la finestra del titolo vincitore.
   */
  fun dismissWinner() {
    _uiState.update { it.copy(pickedWinner = null) }
  }

  /**
   * Apre o chiude il pannello rapido con la lista dei titoli salvati.
   */
  fun toggleShortlistSheet(show: Boolean) {
    _uiState.update { it.copy(showShortlistSheet = show) }
  }

  /**
   * Rimescola il mazzo corrente per ricominciare a scorrere.
   */
  fun reshuffleDeck() {
    _uiState.update {
      it.copy(
        deck = it.deck.shuffled(),
        currentIndex = 0,
        skippedMovies = emptyList()
      )
    }
  }
}
