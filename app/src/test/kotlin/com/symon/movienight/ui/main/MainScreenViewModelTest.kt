package com.symon.movienight.ui.main

import com.symon.movienight.data.CuratedImdbCatalog
import com.symon.movienight.data.InMemoryLibraryStorage
import com.symon.movienight.data.MalImportEntry
import com.symon.movienight.data.MalUserImportResult
import com.symon.movienight.data.MovieRepository
import com.symon.movienight.model.MediaSource
import com.symon.movienight.model.MediaType
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Repository finto ("Fake") per testare il ViewModel in modo istantaneo e deterministico.
 */
private class FakeMovieRepository : MovieRepository {
  override suspend fun getCatalog(
    genreFilter: MovieGenreFilter,
    mediaType: MediaType
  ): List<Movie> {
    val all = CuratedImdbCatalog.forType(mediaType)
    return if (genreFilter.apiGenre == null) {
      all
    } else {
      all.filter { movie -> movie.genres.any { it.equals(genreFilter.apiGenre, ignoreCase = true) } }
    }
  }

  override suspend fun searchLiveImdb(query: String): List<Movie> {
    return CuratedImdbCatalog.movies.filter { it.title.contains(query, ignoreCase = true) }
  }

  override suspend fun searchLive(query: String, mediaType: MediaType): List<Movie> {
    return CuratedImdbCatalog.forType(mediaType).filter { it.title.contains(query, ignoreCase = true) }
  }

  override suspend fun importMyAnimeListProfile(username: String): MalUserImportResult {
    return MalUserImportResult(
      username = username,
      entries = listOf(
        MalImportEntry(
          movie = CuratedImdbCatalog.anime[0], // Fullmetal Alchemist: Brotherhood
          isWatched = true,
          userScore = 10,
          watchedEpisodesCount = 3
        ),
        MalImportEntry(
          movie = CuratedImdbCatalog.anime[1], // Frieren
          isWatched = false,
          userScore = null,
          watchedEpisodesCount = 0
        )
      ),
      message = "Sincronizzato @$username"
    )
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
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())
    val state = viewModel.uiState.value

    assertTrue("Il mazzo non deve essere vuoto", state.deck.isNotEmpty())
    assertNotNull("Deve esserci un film attivo in cima al mazzo", state.currentMovie)
    assertEquals(0, state.currentIndex)
  }

  @Test
  fun `swipe a destra (likeCurrentMovie) salva il film in Da Vedere e avanza`() = runTest {
    val storage = InMemoryLibraryStorage()
    val viewModel = MovieNightViewModel(FakeMovieRepository(), storage)
    val firstMovie = viewModel.uiState.value.currentMovie!!

    viewModel.likeCurrentMovie()

    val state = viewModel.uiState.value
    assertEquals(1, state.likedMovies.size)
    assertEquals(firstMovie.id, state.likedMovies.first().id)
    assertEquals(1, state.currentIndex)
    assertEquals(1, storage.load().watchlist.size)
  }

  @Test
  fun `swipe a sinistra (skipCurrentMovie) scarta il film e avanza`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())
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
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())
    val likedMovie = viewModel.uiState.value.currentMovie!!

    viewModel.likeCurrentMovie()
    viewModel.pickRandomForTonight()

    val winner = viewModel.uiState.value.pickedWinner
    assertNotNull("Deve essere stato estratto un film vincitore", winner)
    assertEquals(likedMovie.id, winner?.id)
  }

  @Test
  fun `filtro per genere mostra solo i film di quel genere`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())

    viewModel.selectGenre(MovieGenreFilter.HORROR)

    val state = viewModel.uiState.value
    assertEquals(MovieGenreFilter.HORROR, state.selectedGenre)
    assertTrue(state.deck.isNotEmpty())
    assertTrue(state.deck.all { "Horror" in it.genres })
  }

  @Test
  fun `selezione categoria Serie TV e Anime carica i rispettivi cataloghi con fonte corretta`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())

    viewModel.selectMediaType(MediaType.TV_SERIES)
    assertEquals(MediaType.TV_SERIES, viewModel.uiState.value.selectedMediaType)
    assertTrue(viewModel.uiState.value.deck.all { it.mediaType == MediaType.TV_SERIES })
    assertTrue(viewModel.uiState.value.deck.first().episodes.isNotEmpty())

    viewModel.selectMediaType(MediaType.ANIME)
    assertEquals(MediaType.ANIME, viewModel.uiState.value.selectedMediaType)
    assertTrue(viewModel.uiState.value.deck.all { it.mediaType == MediaType.ANIME })
    assertTrue(viewModel.uiState.value.deck.all { it.source == MediaSource.MYANIMELIST })
    assertTrue(viewModel.uiState.value.deck.first().externalPageUrl.contains("myanimelist.net/anime/"))
  }

  @Test
  fun `spostamento da Da Vedere a Gia Visti con voto personale`() = runTest {
    val storage = InMemoryLibraryStorage()
    val viewModel = MovieNightViewModel(FakeMovieRepository(), storage)
    val movie = viewModel.uiState.value.currentMovie!!

    // 1. Aggiungiamo in "Da Vedere"
    viewModel.likeCurrentMovie()
    assertEquals(1, viewModel.uiState.value.likedMovies.size)

    // 2. Segniamo come "Già Visto" con voto 9/10
    viewModel.markAsWatched(movie, userRating = 9)

    val state = viewModel.uiState.value
    assertTrue("Deve essere stato rimosso da Da Vedere", state.likedMovies.isEmpty())
    assertEquals(1, state.watchedMovies.size)
    assertEquals(9, state.userProgress[movie.id]?.userRating)
    assertEquals(1, storage.load().watched.size)
  }

  @Test
  fun `tracciamento e rating dei singoli episodi di una Serie TV o Anime`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())
    val breakingBad = CuratedImdbCatalog.tvSeries.first { it.id == "tt0903747" }
    val ozymandiasEp = breakingBad.episodes.first { it.episodeNumber == 14 }

    // Votiamo l'episodio Ozymandias con 10 stelle!
    viewModel.rateEpisode(breakingBad, ozymandiasEp.id, rating = 10)

    val state = viewModel.uiState.value
    val prog = state.userProgress[breakingBad.id]
    assertNotNull(prog)
    assertTrue("L'episodio votato deve risultare visto", ozymandiasEp.id in prog!!.watchedEpisodeIds)
    assertEquals(10, prog.episodeRatings[ozymandiasEp.id])
    assertTrue("La serie deve comparire tra i Già Visti / In visione", state.watchedMovies.any { it.id == breakingBad.id })

    // Togliamo la spunta di visto dall'episodio
    viewModel.toggleEpisodeWatched(breakingBad, ozymandiasEp.id)
    assertFalse(ozymandiasEp.id in viewModel.uiState.value.userProgress[breakingBad.id]!!.watchedEpisodeIds)
  }

  @Test
  fun `importazione profilo MyAnimeList popola Da Vedere e Gia Visti`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())

    viewModel.importFromMyAnimeList("Symon180")

    val state = viewModel.uiState.value
    assertEquals("Symon180", state.malUsername)
    assertTrue(state.watchedMovies.any { it.malId == 5114 }) // Fullmetal Alchemist: Brotherhood
    assertTrue(state.likedMovies.any { it.malId == 52991 })  // Frieren in Plan to Watch
    assertEquals(10, state.userProgress["mal_5114"]?.userRating)
    assertEquals(3, state.userProgress["mal_5114"]?.watchedEpisodeIds?.size)
  }
}
