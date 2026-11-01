package com.symon.movienight.ui.main

import com.symon.movienight.data.CuratedImdbCatalog
import com.symon.movienight.data.MovieRepository
import com.symon.movienight.model.Movie
import com.symon.movienight.model.MovieGenreFilter
import com.symon.movienight.viewmodel.MovieNightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Repository finto ("Fake") per testare il ViewModel in modo istantaneo e deterministico.
 */
private class FakeMovieRepository : MovieRepository {
  override suspend fun getCatalog(genreFilter: MovieGenreFilter): List<Movie> {
    val all = CuratedImdbCatalog.movies
    return if (genreFilter.apiGenre == null) {
      all
    } else {
      all.filter { movie -> movie.genres.any { it.equals(genreFilter.apiGenre, ignoreCase = true) } }
    }
  }

  override suspend fun searchLiveImdb(query: String): List<Movie> {
    return CuratedImdbCatalog.movies.filter { it.title.contains(query, ignoreCase = true) }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `caricamento iniziale popola il mazzo di film`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository())
    val state = viewModel.uiState.value

    assertTrue("Il mazzo non deve essere vuoto", state.deck.isNotEmpty())
    assertNotNull("Deve esserci un film attivo in cima al mazzo", state.currentMovie)
    assertEquals(0, state.currentIndex)
  }

  @Test
  fun `swipe a destra (likeCurrentMovie) salva il film nei preferiti e avanza`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository())
    val firstMovie = viewModel.uiState.value.currentMovie!!

    viewModel.likeCurrentMovie()

    val state = viewModel.uiState.value
    assertEquals(1, state.likedMovies.size)
    assertEquals(firstMovie.id, state.likedMovies.first().id)
    assertEquals(1, state.currentIndex)
  }

  @Test
  fun `swipe a sinistra (skipCurrentMovie) scarta il film e avanza`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository())
    val firstMovie = viewModel.uiState.value.currentMovie!!

    viewModel.skipCurrentMovie()

    val state = viewModel.uiState.value
    assertTrue(state.likedMovies.isEmpty())
    assertEquals(1, state.skippedMovies.size)
    assertEquals(firstMovie.id, state.skippedMovies.first().id)
    assertEquals(1, state.currentIndex)
  }

  @Test
  fun `pulsante Scegli per me a caso pesca dai preferiti se presenti`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository())
    val likedMovie = viewModel.uiState.value.currentMovie!!

    viewModel.likeCurrentMovie()
    viewModel.pickRandomForTonight()

    val winner = viewModel.uiState.value.pickedWinner
    assertNotNull("Deve essere stato estratto un film vincitore", winner)
    assertEquals(likedMovie.id, winner?.id)
  }

  @Test
  fun `filtro per genere mostra solo i film di quel genere`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository())

    viewModel.selectGenre(MovieGenreFilter.HORROR)

    val state = viewModel.uiState.value
    assertEquals(MovieGenreFilter.HORROR, state.selectedGenre)
    assertTrue(state.deck.isNotEmpty())
    assertTrue(state.deck.all { "Horror" in it.genres })
  }
}
