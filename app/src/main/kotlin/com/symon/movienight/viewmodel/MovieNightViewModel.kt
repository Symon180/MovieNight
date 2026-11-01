package com.symon.movienight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.symon.movienight.data.DefaultMovieRepository
import com.symon.movienight.data.MovieRepository
import com.symon.movienight.model.Movie
import com.symon.movienight.model.MovieGenreFilter
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
  val selectedGenre: MovieGenreFilter = MovieGenreFilter.ALL,
  val searchQuery: String = "",
  val deck: List<Movie> = emptyList(),           // Mazzo di film da scorrere
  val currentIndex: Int = 0,                     // Indice della scheda visibile in cima al mazzo
  val likedMovies: List<Movie> = emptyList(),    // Film salvati con "Mi ispira" (Swipe a destra)
  val skippedMovies: List<Movie> = emptyList(),  // Film scartati con "Salta" (Swipe a sinistra)
  val pickedWinner: Movie? = null,               // Il film estratto per la serata!
  val showShortlistSheet: Boolean = false        // Mostra/nasconde il riepilogo dei film che ispirano
) {
  /**
   * Il film attualmente in cima al mazzo (o `null` se il mazzo è finito).
   */
  val currentMovie: Movie?
    get() = deck.getOrNull(currentIndex)

  /**
   * Il prossimo film subito sotto la prima carta (utile per l'effetto pila 3D).
   */
  val nextMovieInDeck: Movie?
    get() = deck.getOrNull(currentIndex + 1)

  /**
   * Quante schede restano da scorrere nel mazzo attuale.
   */
  val remainingInDeck: Int
    get() = (deck.size - currentIndex).coerceAtLeast(0)
}

/**
 * Il "cervello" dell'app (ViewModel).
 * Gestisce lo scorrimento delle schede, i filtri per genere, la ricerca su IMDb
 * e il pulsante "Scegli per me a caso".
 */
class MovieNightViewModel(
  private val repository: MovieRepository = DefaultMovieRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(MovieNightUiState(isLoading = true))
  val uiState: StateFlow<MovieNightUiState> = _uiState.asStateFlow()

  init {
    loadCatalog(MovieGenreFilter.ALL)
  }

  /**
   * Carica il mazzo di film applicando il filtro di genere scelto.
   */
  fun selectGenre(genre: MovieGenreFilter) {
    loadCatalog(genre)
  }

  private fun loadCatalog(genre: MovieGenreFilter) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isLoading = true,
          selectedGenre = genre,
          searchQuery = "",
          currentIndex = 0
        )
      }
      val movies = repository.getCatalog(genre)
      _uiState.update {
        it.copy(
          isLoading = false,
          deck = movies,
          currentIndex = 0
        )
      }
    }
  }

  /**
   * Cerca un film per titolo/attore interrogando anche l'endpoint live di IMDb.
   */
  fun searchMovies(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    if (query.isBlank()) {
      loadCatalog(_uiState.value.selectedGenre)
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val results = repository.searchLiveImdb(query)
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
   * Azione "Mi ispira" (Swipe a Destra o pulsante ❤️):
   * aggiunge il film corrente ai candidati della serata (`likedMovies`) e passa al successivo.
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
  }

  /**
   * Azione "Salta" (Swipe a Sinistra o pulsante ✖️):
   * scarta il film corrente e passa al successivo.
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
   * - Se l'utente ha già messo almeno 1 film nella lista "Mi ispira", pesca a sorte tra quelli!
   * - Altrimenti pesca a sorte direttamente dal mazzo corrente filtrato per genere.
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
   * Permette di scegliere direttamente un film specifico dalla lista dei preferiti.
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
   * Rimuove un film dalla lista "Mi ispira".
   */
  fun removeFromLiked(movie: Movie) {
    _uiState.update {
      it.copy(likedMovies = it.likedMovies.filterNot { item -> item.id == movie.id })
    }
  }

  /**
   * Chiude la finestra del film vincitore (ad es. per pescarne un altro).
   */
  fun dismissWinner() {
    _uiState.update { it.copy(pickedWinner = null) }
  }

  /**
   * Apre o chiude il pannello con la lista dei film salvati stasera.
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
