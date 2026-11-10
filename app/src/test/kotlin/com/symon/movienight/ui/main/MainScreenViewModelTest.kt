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
  val pushedMalUpdates = mutableListOf<String>()
  val deletedMalIds = mutableListOf<Int>()

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

  override suspend fun importMyAnimeListProfile(
    username: String,
    accessToken: String?
  ): MalUserImportResult {
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
      message = "Sincronizzato @$username",
      isTwoWayAuthenticated = !accessToken.isNullOrBlank()
    )
  }

  override suspend fun pushMalAnimeStatus(
    accessToken: String,
    malId: Int,
    status: String,
    score: Int?,
    numWatchedEpisodes: Int?
  ): Boolean {
    pushedMalUpdates.add("$malId:$status:${score ?: -1}:${numWatchedEpisodes ?: -1}")
    return true
  }

  override suspend fun deleteMalAnimeEntry(
    accessToken: String,
    malId: Int
  ): Boolean {
    deletedMalIds.add(malId)
    return true
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

  @Test
  fun `sincronizzazione bidirezionale MyAnimeList invia in tempo reale aggiunte voti ed episodi su MAL`() = runTest {
    val fakeRepo = FakeMovieRepository()
    val viewModel = MovieNightViewModel(fakeRepo, InMemoryLibraryStorage())

    // 1. Colleghiamo l'account MyAnimeList con token OAuth2 bidirezionale
    viewModel.connectMalWithAccessToken("Symon180", "oauth_test_token_123")
    assertTrue("La sincronizzazione bidirezionale deve risultare attiva", viewModel.uiState.value.isTwoWayMalActive)

    // 2. Aggiungiamo Attack on Titan (malId = 16498) alla lista Da Vedere -> deve inviare plan_to_watch a MAL!
    val aot = CuratedImdbCatalog.anime.first { it.malId == 16498 }
    viewModel.addToWatchlist(aot)
    assertTrue(
      "L'aggiunta in Watchlist deve inviare plan_to_watch a MyAnimeList",
      fakeRepo.pushedMalUpdates.any { it.startsWith("16498:plan_to_watch:") }
    )

    // 3. Spuntiamo un episodio di Attack on Titan -> deve inviare watching e num_watched_episodes = 1 a MAL!
    viewModel.toggleEpisodeWatched(aot, aot.episodes.first().id)
    assertTrue(
      "Spuntare un episodio deve aggiornare subito MyAnimeList a watching con 1 episodio",
      fakeRepo.pushedMalUpdates.any { it.startsWith("16498:watching:") && it.endsWith(":1") }
    )

    // 4. Diamo un voto (9/10) -> deve inviare subito score = 9 a MAL!
    viewModel.rateMedia(aot.id, 9)
    assertTrue(
      "Votare un anime nell'app deve aggiornare il voto su MyAnimeList",
      fakeRepo.pushedMalUpdates.any { it.contains("16498:") && it.contains(":9:") }
    )

    // 5. Rimuoviamo dalla lista -> deve inviare DELETE a MAL!
    viewModel.removeFromLiked(aot)
    assertTrue("Rimuovere un anime deve cancellarlo anche su MAL", 16498 in fakeRepo.deletedMalIds)
  }

  @Test
  fun `paginazione infinita del mazzo e generazione URL OAuth2 PKCE MyAnimeList`() = runTest {
    val fakeRepo = FakeMovieRepository()
    val viewModel = MovieNightViewModel(fakeRepo, InMemoryLibraryStorage())

    assertEquals(1, viewModel.uiState.value.currentPage)
    viewModel.loadMoreDeck()
    assertEquals("Il caricamento pagina successiva incrementa currentPage a 2", 2, viewModel.uiState.value.currentPage)

    val oauthUrl = viewModel.prepareMalOAuthLoginUrl("my_test_client_id")
    assertTrue(oauthUrl.startsWith("https://myanimelist.net/v1/oauth2/authorize?"))
    assertTrue(oauthUrl.contains("client_id=my_test_client_id"))
    assertTrue(oauthUrl.contains("code_challenge_method=plain"))
    assertEquals(64, viewModel.uiState.value.malCodeVerifier.length)
  }
}
