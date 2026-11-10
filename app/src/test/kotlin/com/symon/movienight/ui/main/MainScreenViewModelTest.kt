package com.symon.movienight.ui.main

import com.symon.movienight.data.CuratedImdbCatalog
import com.symon.movienight.data.DefaultMovieRepository
import com.symon.movienight.data.InMemoryLibraryStorage
import com.symon.movienight.data.MalImportEntry
import com.symon.movienight.data.MalUserImportResult
import com.symon.movienight.data.MovieRepository
import com.symon.movienight.model.AnimeFormatFilter
import com.symon.movienight.model.EraFilter
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
    mediaType: MediaType,
    animeFormat: AnimeFormatFilter,
    eraFilter: EraFilter,
    page: Int
  ): List<Movie> {
    val all = CuratedImdbCatalog.forType(mediaType)
    val byGenre = if (genreFilter.apiGenre == null) {
      all
    } else {
      all.filter { movie -> movie.genres.any { it.equals(genreFilter.apiGenre, ignoreCase = true) } }
    }
    val byFormat = if (mediaType != MediaType.ANIME) {
      byGenre
    } else {
      when (animeFormat) {
        AnimeFormatFilter.ALL -> byGenre
        AnimeFormatFilter.SERIES -> byGenre.filter { it.isAnimeSeries }
        AnimeFormatFilter.MOVIE -> byGenre.filter { it.isAnimeMovie }
      }
    }
    return byFormat.filter { eraFilter.matches(it.year) }
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
  fun `swipe a sinistra (skipCurrentMovie) blocca per sempre il programma in dislikedIds`() = runTest {
    val storage = InMemoryLibraryStorage()
    val viewModel = MovieNightViewModel(FakeMovieRepository(), storage)
    val firstMovie = viewModel.uiState.value.currentMovie!!

    viewModel.skipCurrentMovie()

    val state = viewModel.uiState.value
    assertTrue(state.likedMovies.isEmpty())
    assertTrue("L'ID deve essere salvato nei programmi da non mostrare più", firstMovie.id in state.dislikedIds)
    assertTrue(firstMovie.id in storage.load().dislikedIds)

    // Se ricarichiamo il catalogo, il film scartato non deve più apparire!
    viewModel.selectGenre(MovieGenreFilter.ALL)
    assertFalse("Il film scartato a sinistra non deve più comparire nel mazzo", viewModel.uiState.value.deck.any { it.id == firstMovie.id })
  }

  @Test
  fun `swipe verso l alto (markCurrentMovieWatched) segna subito come Gia Visto`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())
    val firstMovie = viewModel.uiState.value.currentMovie!!

    viewModel.markCurrentMovieWatched()

    val state = viewModel.uiState.value
    assertEquals(1, state.watchedMovies.size)
    assertEquals(firstMovie.id, state.watchedMovies.first().id)
    assertEquals(1, state.currentIndex)
  }

  @Test
  fun `swipe verso il basso (putBackInPool) rimette la scheda in fondo al mazzo`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())
    val initialSize = viewModel.uiState.value.deck.size
    val firstMovie = viewModel.uiState.value.currentMovie!!

    viewModel.putBackInPool()

    val state = viewModel.uiState.value
    assertEquals(initialSize + 1, state.deck.size)
    assertEquals(firstMovie.id, state.deck.last().id)
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
  fun `filtro per genere e rigoroso e Attack on Titan non compare mai in Commedia`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())

    viewModel.selectMediaType(MediaType.ANIME)
    viewModel.selectGenre(MovieGenreFilter.COMEDY)

    val state = viewModel.uiState.value
    assertEquals(MovieGenreFilter.COMEDY, state.selectedGenre)
    assertTrue("Ci devono essere anime commedia reali (es. Spy x Family, One Punch Man)", state.deck.isNotEmpty())
    assertTrue("Tutti gli anime filtrati devono avere il genere Comedy", state.deck.all { "Comedy" in it.genres })
    assertFalse("Attack on Titan NON deve essere in Commedia!", state.deck.any { it.title.contains("Attack on Titan") })
  }

  @Test
  fun `nessun limite di 6 sugli episodi delle serie e apertura scheda dettagli`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())
    val breakingBad = CuratedImdbCatalog.tvSeries.first { it.id == "tt0903747" }
    val attackOnTitan = CuratedImdbCatalog.anime.first { it.id == "mal_16498" }

    assertEquals("Breaking Bad deve avere tutti i 62 episodi", 62, breakingBad.episodes.size)
    assertEquals("Attack on Titan deve avere tutti gli 89 episodi", 89, attackOnTitan.episodes.size)

    viewModel.openProgramDetails(breakingBad)
    val detailMedia = viewModel.uiState.value.selectedMediaForDetails
    assertNotNull(detailMedia)
    assertTrue(detailMedia!!.resolvedCast.isNotEmpty())
    assertTrue(detailMedia.watchLinks.isNotEmpty())
    assertTrue(detailMedia.resolvedLanguages.isNotBlank())
  }

  @Test
  fun `spostamento da Da Vedere a Gia Visti con voto personale`() = runTest {
    val storage = InMemoryLibraryStorage()
    val viewModel = MovieNightViewModel(FakeMovieRepository(), storage)
    val movie = viewModel.uiState.value.currentMovie!!

    viewModel.likeCurrentMovie()
    assertEquals(1, viewModel.uiState.value.likedMovies.size)

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
    val ozymandiasEp = breakingBad.episodes.first { it.id == "tt0903747_e60" }

    viewModel.rateEpisode(breakingBad, ozymandiasEp.id, rating = 10)

    val state = viewModel.uiState.value
    val prog = state.userProgress[breakingBad.id]
    assertNotNull(prog)
    assertTrue("L'episodio votato deve risultare visto", ozymandiasEp.id in prog!!.watchedEpisodeIds)
    assertEquals(10, prog.episodeRatings[ozymandiasEp.id])
    assertTrue("La serie deve comparire tra i Già Visti / In visione", state.watchedMovies.any { it.id == breakingBad.id })

    viewModel.toggleEpisodeWatched(breakingBad, ozymandiasEp.id)
    assertFalse(ozymandiasEp.id in viewModel.uiState.value.userProgress[breakingBad.id]!!.watchedEpisodeIds)
  }

  @Test
  fun `importazione profilo MyAnimeList popola Da Vedere e Gia Visti`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())

    viewModel.importFromMyAnimeList("Symon180")

    val state = viewModel.uiState.value
    assertEquals("Symon180", state.malUsername)
    assertTrue(state.watchedMovies.any { it.malId == 5114 })
    assertTrue(state.likedMovies.any { it.malId == 52991 })
    assertEquals(10, state.userProgress["mal_5114"]?.userRating)
    assertEquals(3, state.userProgress["mal_5114"]?.watchedEpisodeIds?.size)
  }

  @Test
  fun `i film anime stanno solo in Anime e la sezione Anime si divide in Serie e Film`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())

    // 1. Nella sezione Film normali NON deve esserci La città incantata o altri film anime
    val normalMovies = viewModel.uiState.value.deck
    assertFalse(
      "I film anime non devono stare nella sezione Film!",
      normalMovies.any { DefaultMovieRepository.isAnimeTitle(it) }
    )

    // 2. Nella sezione Anime -> Film Anime devono esserci solo i Film Anime (totalEpisodes == 1)
    viewModel.selectAnimeFormat(AnimeFormatFilter.MOVIE)
    val animeMovies = viewModel.uiState.value.deck
    assertTrue("Devono esserci film anime (es. La città incantata, Your Name, Akira)", animeMovies.isNotEmpty())
    assertTrue("Tutti gli elementi devono essere Film Anime", animeMovies.all { it.isAnimeMovie && !it.isEpisodic })
    assertTrue(animeMovies.any { it.title.contains("La città incantata") })

    // 3. Nella sezione Anime -> Serie Anime devono esserci solo le Serie Anime (totalEpisodes > 1)
    viewModel.selectAnimeFormat(AnimeFormatFilter.SERIES)
    val animeSeries = viewModel.uiState.value.deck
    assertTrue("Devono esserci serie anime", animeSeries.isNotEmpty())
    assertTrue("Tutti gli elementi devono essere Serie Anime con episodi", animeSeries.all { it.isAnimeSeries && it.isEpisodic })
  }

  @Test
  fun `filtro per epoca mostra i classici e anni 80 e 90`() = runTest {
    val viewModel = MovieNightViewModel(FakeMovieRepository(), InMemoryLibraryStorage())

    viewModel.selectEra(EraFilter.CLASSICS)
    val classics = viewModel.uiState.value.deck
    assertTrue("Devono esserci film classici (< 1980) come Il Padrino o Alien", classics.isNotEmpty())
    assertTrue("Tutti i titoli devono essere usciti prima del 1980", classics.all { it.year <= 1979 })

    viewModel.selectEra(EraFilter.YEARS_80S)
    val eighties = viewModel.uiState.value.deck
    assertTrue("Devono esserci film anni '80 come Ritorno al futuro o Blade Runner", eighties.isNotEmpty())
    assertTrue("Tutti i titoli devono essere degli anni '80", eighties.all { it.year in 1980..1989 })
  }

  @Test
  fun `attori e personaggi hanno foto ritratto associate`() = runTest {
    val inception = CuratedImdbCatalog.movies.first { it.id == "tt1375666" }
    val castMembers = inception.resolvedCastMembers
    assertTrue("Deve esserci il cast di Inception", castMembers.isNotEmpty())
    assertTrue(
      "Leonardo DiCaprio deve avere l'URL della sua foto ritratto",
      castMembers.first().photoUrl.startsWith("https://")
    )
  }
}
