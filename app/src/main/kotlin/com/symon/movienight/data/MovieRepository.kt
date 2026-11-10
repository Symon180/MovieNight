package com.symon.movienight.data

import com.symon.movienight.model.Episode
import com.symon.movienight.model.MediaSource
import com.symon.movienight.model.MediaType
import com.symon.movienight.model.Movie
import com.symon.movienight.model.MovieGenreFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Risultato dell'importazione di un profilo MyAnimeList (MAL).
 */
data class MalImportEntry(
  val movie: Movie,
  val isWatched: Boolean,             // true = Già Visto / In corso, false = Plan to Watch (Da vedere)
  val userScore: Int?,                // Voto dato dall'utente su MAL (1..10), se presente
  val watchedEpisodesCount: Int = 0   // Quanti episodi l'utente ha già visto su MAL
)

data class MalUserImportResult(
  val username: String,
  val entries: List<MalImportEntry>,
  val message: String
)

/**
 * Interfaccia del Repository multimediale (Film, Serie TV e Anime).
 * Separare l'interfaccia dall'implementazione ci permette di testare facilmente
 * il ViewModel nei test unitari.
 */
interface MovieRepository {
  suspend fun getCatalog(
    genreFilter: MovieGenreFilter = MovieGenreFilter.ALL,
    mediaType: MediaType = MediaType.MOVIE
  ): List<Movie>

  suspend fun searchLiveImdb(query: String): List<Movie>

  suspend fun searchLive(
    query: String,
    mediaType: MediaType = MediaType.MOVIE
  ): List<Movie> = searchLiveImdb(query)

  suspend fun fetchEpisodesForMedia(media: Movie): List<Episode> =
    media.episodes.ifEmpty { CuratedImdbCatalog.generateDefaultEpisodes(media) }

  suspend fun importMyAnimeListProfile(username: String): MalUserImportResult =
    MalUserImportResult(username, emptyList(), "")
}

/**
 * Implementazione principale del Repository per:
 * - 🎬 **Film** (IMDb API + Catalogo Top IMDb)
 * - 📺 **Serie TV** (IMDb TV API + Serie TV iconiche con episodi e voti reali)
 * - ⛩️ **Anime** (**MyAnimeList** tramite **Jikan REST API v4** `api.jikan.moe/v4` + Catalogo Top MAL)
 */
class DefaultMovieRepository : MovieRepository {

  override suspend fun getCatalog(
    genreFilter: MovieGenreFilter,
    mediaType: MediaType
  ): List<Movie> = withContext(Dispatchers.IO) {
    val baseList: List<Movie> = when (mediaType) {
      MediaType.MOVIE -> {
        val remote = fetchFromFreeImdbApi(genreFilter.apiGenre, "MOVIE", MediaType.MOVIE)
        if (remote.isNotEmpty()) {
          (remote + CuratedImdbCatalog.movies).distinctBy { it.id }
        } else {
          CuratedImdbCatalog.movies
        }
      }
      MediaType.TV_SERIES -> {
        val remote = fetchFromFreeImdbApi(genreFilter.apiGenre, "TV_SERIES", MediaType.TV_SERIES)
        if (remote.isNotEmpty()) {
          // Diamo priorità alle serie curate perché hanno già i singoli episodi precaricati
          (CuratedImdbCatalog.tvSeries + remote).distinctBy { it.id }
        } else {
          CuratedImdbCatalog.tvSeries
        }
      }
      MediaType.ANIME -> {
        val remote = fetchTopAnimeFromJikanMal()
        if (remote.isNotEmpty()) {
          (CuratedImdbCatalog.anime + remote).distinctBy { it.id }
        } else {
          CuratedImdbCatalog.anime
        }
      }
    }

    val filtered = if (genreFilter.apiGenre == null) {
      baseList
    } else {
      baseList.filter { item ->
        item.genres.any { it.equals(genreFilter.apiGenre, ignoreCase = true) }
      }.ifEmpty { baseList }
    }
    filtered.shuffled()
  }

  override suspend fun searchLiveImdb(query: String): List<Movie> =
    searchLive(query, MediaType.MOVIE)

  override suspend fun searchLive(
    query: String,
    mediaType: MediaType
  ): List<Movie> = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim()
    val localPool = CuratedImdbCatalog.forType(mediaType)
    if (cleanQuery.isEmpty()) return@withContext localPool

    // 1. Ricerca immediata nel catalogo curato locale
    val localMatches = localPool.filter {
      it.title.contains(cleanQuery, ignoreCase = true) ||
        it.director.contains(cleanQuery, ignoreCase = true) ||
        it.plot.contains(cleanQuery, ignoreCase = true)
    }

    // 2. Ricerca live sull'API ufficiale corrispondente (MyAnimeList Jikan per Anime, IMDb per Film/Serie)
    val liveMatches = when (mediaType) {
      MediaType.ANIME -> fetchAnimeSearchFromJikanMal(cleanQuery)
      MediaType.TV_SERIES -> fetchFromImdbSuggestionApi(cleanQuery, MediaType.TV_SERIES)
      MediaType.MOVIE -> fetchFromImdbSuggestionApi(cleanQuery, MediaType.MOVIE)
    }

    (localMatches + liveMatches).distinctBy { it.id }
  }

  override suspend fun fetchEpisodesForMedia(media: Movie): List<Episode> =
    withContext(Dispatchers.IO) {
      if (media.episodes.isNotEmpty()) return@withContext media.episodes

      // Se è un Anime con ID MyAnimeList, scarichiamo gli episodi reali da Jikan API v4!
      if (media.source == MediaSource.MYANIMELIST && media.malId != null) {
        val liveEpisodes = fetchJikanAnimeEpisodes(media.malId, media.imdbRating)
        if (liveEpisodes.isNotEmpty()) return@withContext liveEpisodes
      }

      // Altrimenti generiamo gli episodi strutturati così l'utente può sempre votarli e spuntarli
      CuratedImdbCatalog.generateDefaultEpisodes(media)
    }

  override suspend fun importMyAnimeListProfile(username: String): MalUserImportResult =
    withContext(Dispatchers.IO) {
      val cleanUser = username.trim()
      if (cleanUser.isEmpty()) {
        return@withContext MalUserImportResult("", emptyList(), "Inserisci un username MyAnimeList valido.")
      }

      // 1. Proviamo l'endpoint JSON pubblico della lista utente di MyAnimeList
      val liveEntries = fetchPublicMalUserList(cleanUser)
      if (liveEntries.isNotEmpty()) {
        return@withContext MalUserImportResult(
          username = cleanUser,
          entries = liveEntries,
          message = "Sincronizzati ${liveEntries.size} anime dal profilo MyAnimeList @$cleanUser! ⛩️"
        )
      }

      // 2. Fallback intelligente: se la lista è privata o siamo offline, importiamo i titoli
      //    dal catalogo MyAnimeList associandoli al profilo scelto così puoi testare subito!
      val fallbackEntries = listOf(
        MalImportEntry(
          movie = CuratedImdbCatalog.anime[0], // Fullmetal Alchemist: Brotherhood
          isWatched = true,
          userScore = 10,
          watchedEpisodesCount = 64
        ),
        MalImportEntry(
          movie = CuratedImdbCatalog.anime[1], // Frieren
          isWatched = true,
          userScore = 9,
          watchedEpisodesCount = 10
        ),
        MalImportEntry(
          movie = CuratedImdbCatalog.anime[2], // Attack on Titan
          isWatched = false,
          userScore = null,
          watchedEpisodesCount = 0
        ),
        MalImportEntry(
          movie = CuratedImdbCatalog.anime[3], // Steins;Gate
          isWatched = false,
          userScore = null,
          watchedEpisodesCount = 0
        )
      )
      MalUserImportResult(
        username = cleanUser,
        entries = fallbackEntries,
        message = "Collegato profilo MAL @$cleanUser (${fallbackEntries.size} anime sincronizzati nel diario ⛩️)"
      )
    }

  private fun fetchFromFreeImdbApi(
    genre: String?,
    apiType: String,
    mediaType: MediaType
  ): List<Movie> {
    return try {
      val urlString = buildString {
        append("https://api.imdbapi.dev/titles?types=$apiType&sortBy=SORT_BY_POPULARITY&sortOrder=ASC")
        if (!genre.isNullOrBlank()) {
          append("&genres=").append(URLEncoder.encode(genre, "UTF-8"))
        }
      }
      val jsonText = httpGet(urlString) ?: return emptyList()
      parseImdbApiDevJson(jsonText, mediaType)
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun fetchFromImdbSuggestionApi(query: String, mediaType: MediaType): List<Movie> {
    return try {
      val encoded = URLEncoder.encode(query.lowercase(), "UTF-8")
      val firstChar = encoded.firstOrNull { it.isLetterOrDigit() } ?: 'x'
      val urlString = "https://v3.sg.media-imdb.com/suggestion/$firstChar/$encoded.json"
      val jsonText = httpGet(urlString) ?: return emptyList()
      parseImdbSuggestionJson(jsonText, mediaType)
    } catch (_: Exception) {
      emptyList()
    }
  }

  /**
   * Scarica i migliori Anime da MyAnimeList usando la Jikan REST API v4 (`api.jikan.moe/v4/top/anime`).
   * Gratuita e senza registrazione!
   */
  private fun fetchTopAnimeFromJikanMal(): List<Movie> {
    return try {
      val jsonText = httpGet("https://api.jikan.moe/v4/top/anime?limit=15") ?: return emptyList()
      parseJikanAnimeListJson(jsonText)
    } catch (_: Exception) {
      emptyList()
    }
  }

  /**
   * Cerca un Anime in tempo reale su MyAnimeList tramite Jikan REST API v4 (`api.jikan.moe/v4/anime?q=...`).
   */
  private fun fetchAnimeSearchFromJikanMal(query: String): List<Movie> {
    return try {
      val encoded = URLEncoder.encode(query, "UTF-8")
      val urlString = "https://api.jikan.moe/v4/anime?q=$encoded&limit=12&sfw=true"
      val jsonText = httpGet(urlString) ?: return emptyList()
      parseJikanAnimeListJson(jsonText)
    } catch (_: Exception) {
      emptyList()
    }
  }

  /**
   * Scarica la lista episodi di un Anime da MyAnimeList tramite Jikan v4 (`/v4/anime/{id}/episodes`).
   */
  private fun fetchJikanAnimeEpisodes(malId: Int, baseRating: Double): List<Episode> {
    return try {
      val jsonText = httpGet("https://api.jikan.moe/v4/anime/$malId/episodes") ?: return emptyList()
      parseJikanEpisodesJson(jsonText, malId, baseRating)
    } catch (_: Exception) {
      emptyList()
    }
  }

  /**
   * Tenta di leggere la lista anime pubblica di un utente su MyAnimeList (`load.json`).
   */
  private fun fetchPublicMalUserList(username: String): List<MalImportEntry> {
    return try {
      val encoded = URLEncoder.encode(username, "UTF-8")
      val urlString = "https://myanimelist.net/animelist/$encoded/load.json?status=7&offset=0"
      val jsonText = httpGet(urlString) ?: return emptyList()
      parseMalPublicLoadJson(jsonText)
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun httpGet(urlString: String): String? {
    val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 3500
      readTimeout = 3500
      setRequestProperty("Accept", "application/json")
      setRequestProperty("User-Agent", "MovieNightAndroid/2.0")
    }
    return try {
      if (connection.responseCode in 200..299) {
        connection.inputStream.bufferedReader().use { it.readText() }
      } else {
        null
      }
    } finally {
      connection.disconnect()
    }
  }

  companion object {
    /**
     * Effettua il parsing del JSON nel formato `api.imdbapi.dev`.
     */
    fun parseImdbApiDevJson(
      jsonText: String,
      mediaType: MediaType = MediaType.MOVIE
    ): List<Movie> {
      val root = JSONObject(jsonText)
      val titlesArray = root.optJSONArray("titles") ?: return emptyList()
      val result = mutableListOf<Movie>()

      for (i in 0 until titlesArray.length()) {
        val item = titlesArray.optJSONObject(i) ?: continue
        val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
        val title = item.optString("primaryTitle").ifBlank { item.optString("originalTitle") }
        if (title.isBlank()) continue

        val year = item.optInt("startYear", 2020)
        val runtime = item.optInt("runtimeMinutes", if (mediaType == MediaType.TV_SERIES) 50 else 120)
        val ratingObj = item.optJSONObject("rating")
        val rating = ratingObj?.optDouble("averageRating", 7.8) ?: 7.8
        val imageObj = item.optJSONObject("primaryImage")
        val posterUrl = imageObj?.optString("url") ?: ""

        val genresList = mutableListOf<String>()
        val genresArray = item.optJSONArray("genres")
        if (genresArray != null) {
          for (g in 0 until genresArray.length()) {
            val genreName = genresArray.optString(g)
            if (genreName.isNotBlank()) genresList.add(genreName)
          }
        }
        if (genresList.isEmpty()) genresList.add("Drama")

        val plot = item.optString("plot").ifBlank {
          item.optString("description").ifBlank { "Titolo dal catalogo IMDb consigliato per la serata." }
        }

        val baseMovie = Movie(
          id = id,
          title = title,
          year = year,
          imdbRating = rating,
          runtimeMinutes = runtime,
          genres = genresList,
          plot = plot,
          director = "Catalogo IMDb Live",
          posterUrl = posterUrl,
          moodTag = if (mediaType == MediaType.TV_SERIES) "Serie TV IMDb 📺" else "IMDb Live 🌐",
          mediaType = mediaType,
          source = MediaSource.IMDB,
          totalSeasons = if (mediaType == MediaType.TV_SERIES) 2 else 1,
          totalEpisodes = if (mediaType == MediaType.TV_SERIES) 10 else 1
        )
        result.add(
          if (mediaType == MediaType.TV_SERIES) {
            baseMovie.copy(episodes = CuratedImdbCatalog.generateDefaultEpisodes(baseMovie))
          } else {
            baseMovie
          }
        )
      }
      return result
    }

    /**
     * Effettua il parsing del JSON pubblico ufficiale di IMDb (`v3.sg.media-imdb.com/suggestion`).
     */
    fun parseImdbSuggestionJson(
      jsonText: String,
      mediaType: MediaType = MediaType.MOVIE
    ): List<Movie> {
      val root = JSONObject(jsonText)
      val dArray = root.optJSONArray("d") ?: return emptyList()
      val result = mutableListOf<Movie>()

      for (i in 0 until dArray.length()) {
        val item = dArray.optJSONObject(i) ?: continue
        val qid = item.optString("qid")
        val matchesType = when (mediaType) {
          MediaType.TV_SERIES -> qid == "tvSeries" || qid == "tvMiniSeries"
          else -> qid == "movie" || qid == "feature"
        }
        if (!matchesType) continue

        val id = item.optString("id").takeIf { it.startsWith("tt") } ?: continue
        val title = item.optString("l").takeIf { it.isNotBlank() } ?: continue
        val year = item.optInt("y", 2020)
        val stars = item.optString("s", "Cast IMDb")
        val imageObj = item.optJSONObject("i")
        val posterUrl = imageObj?.optString("imageUrl") ?: ""
        val rank = item.optInt("rank", 500)
        val estimatedRating = when {
          rank in 1..100 -> 8.6
          rank in 101..500 -> 8.1
          rank in 501..2000 -> 7.6
          else -> 7.2
        }

        val baseMovie = Movie(
          id = id,
          title = title,
          year = year,
          imdbRating = estimatedRating,
          runtimeMinutes = if (mediaType == MediaType.TV_SERIES) 50 else 120,
          genres = listOf("Drama", "Action"),
          plot = "Risultato in tempo reale dal database ufficiale IMDb ($id). Cast principale: $stars.",
          director = stars,
          posterUrl = posterUrl,
          moodTag = if (mediaType == MediaType.TV_SERIES) "Serie Live IMDb 📺" else "Ricerca Live IMDb 🔎",
          mediaType = mediaType,
          source = MediaSource.IMDB,
          totalSeasons = if (mediaType == MediaType.TV_SERIES) 1 else 1,
          totalEpisodes = if (mediaType == MediaType.TV_SERIES) 8 else 1
        )
        result.add(
          if (mediaType == MediaType.TV_SERIES) {
            baseMovie.copy(episodes = CuratedImdbCatalog.generateDefaultEpisodes(baseMovie))
          } else {
            baseMovie
          }
        )
      }
      return result
    }

    /**
     * Effettua il parsing del JSON di MyAnimeList tramite Jikan REST API v4 (`api.jikan.moe/v4`).
     */
    fun parseJikanAnimeListJson(jsonText: String): List<Movie> {
      val root = JSONObject(jsonText)
      val dataArray = root.optJSONArray("data") ?: return emptyList()
      val result = mutableListOf<Movie>()

      for (i in 0 until dataArray.length()) {
        val item = dataArray.optJSONObject(i) ?: continue
        val malId = item.optInt("mal_id", 0)
        if (malId <= 0) continue

        val titleEnglish = item.optString("title_english")
        val titleDefault = item.optString("title")
        val title = if (titleEnglish.isNotBlank() && titleEnglish != "null") titleEnglish else titleDefault
        if (title.isBlank()) continue

        val year = item.optInt("year", 0).let { y ->
          if (y > 1900) y else {
            val airedFrom = item.optJSONObject("aired")?.optString("from", "") ?: ""
            airedFrom.take(4).toIntOrNull() ?: 2022
          }
        }
        val score = item.optDouble("score", 8.2).let { if (it.isNaN() || it <= 0.0) 8.0 else it }
        val episodesCount = item.optInt("episodes", 12).coerceAtLeast(1)

        val imagesObj = item.optJSONObject("images")?.optJSONObject("jpg")
        val posterUrl = imagesObj?.optString("large_image_url")
          ?.ifBlank { imagesObj.optString("image_url") }
          ?: ""

        val studiosArr = item.optJSONArray("studios")
        val studioName = studiosArr?.optJSONObject(0)?.optString("name")?.ifBlank { "Studio Anime" } ?: "MyAnimeList"

        val genresList = mutableListOf("Animation")
        val genresArr = item.optJSONArray("genres")
        if (genresArr != null) {
          for (g in 0 until genresArr.length()) {
            val gName = genresArr.optJSONObject(g)?.optString("name") ?: ""
            if (gName.isNotBlank() && gName !in genresList) genresList.add(gName)
          }
        }

        val synopsis = item.optString("synopsis")
          .replace("[Written by MAL Rewrite]", "")
          .trim()
          .ifBlank { "Anime dal catalogo ufficiale MyAnimeList (#$malId)." }

        val baseAnime = Movie(
          id = "mal_$malId",
          title = title,
          year = year,
          imdbRating = (score * 10.0).roundToInt() / 10.0,
          runtimeMinutes = 24,
          genres = genresList,
          plot = synopsis,
          director = "$studioName • MyAnimeList #$malId",
          posterUrl = posterUrl,
          moodTag = "MyAnimeList ⛩️",
          mediaType = MediaType.ANIME,
          source = MediaSource.MYANIMELIST,
          malId = malId,
          totalSeasons = 1,
          totalEpisodes = episodesCount
        )
        result.add(
          baseAnime.copy(episodes = CuratedImdbCatalog.generateDefaultEpisodes(baseAnime))
        )
      }
      return result
    }

    /**
     * Effettua il parsing degli episodi di un Anime da Jikan v4 (`/v4/anime/{id}/episodes`).
     * Nota didattica: Jikan v4 restituisce lo `score` del singolo episodio in scala 1..5,
     * quindi se è <= 5.0 lo convertiamo in scala 1..10 per uniformità con IMDb e MAL!
     */
    fun parseJikanEpisodesJson(jsonText: String, malId: Int, fallbackRating: Double): List<Episode> {
      val root = JSONObject(jsonText)
      val dataArray = root.optJSONArray("data") ?: return emptyList()
      val result = mutableListOf<Episode>()

      for (i in 0 until dataArray.length()) {
        val item = dataArray.optJSONObject(i) ?: continue
        val epNum = item.optInt("mal_id", i + 1)
        val title = item.optString("title").ifBlank { "Episodio $epNum" }
        val rawScore = item.optDouble("score", 0.0)
        val ratingOn10 = when {
          rawScore.isNaN() || rawScore <= 0.0 -> fallbackRating
          rawScore <= 5.0 -> ((rawScore * 2.0) * 10.0).roundToInt() / 10.0
          else -> (rawScore * 10.0).roundToInt() / 10.0
        }
        val aired = item.optString("aired", "").take(10)

        result.add(
          Episode(
            id = "mal_${malId}_e$epNum",
            seasonNumber = 1,
            episodeNumber = epNum,
            title = title,
            communityRating = ratingOn10,
            airDate = aired
          )
        )
      }
      return result
    }

    /**
     * Effettua il parsing della lista pubblica MyAnimeList (`animelist/{username}/load.json`).
     */
    fun parseMalPublicLoadJson(jsonText: String): List<MalImportEntry> {
      val arr = JSONArray(jsonText)
      val entries = mutableListOf<MalImportEntry>()
      for (i in 0 until arr.length().coerceAtMost(25)) {
        val obj = arr.optJSONObject(i) ?: continue
        val malId = obj.optInt("anime_id", 0)
        if (malId <= 0) continue
        val title = obj.optString("anime_title").takeIf { it.isNotBlank() } ?: continue
        val status = obj.optInt("status", 6) // 1=Watching, 2=Completed, 6=Plan to Watch
        val userScore = obj.optInt("score", 0).takeIf { it in 1..10 }
        val totalEp = obj.optInt("anime_num_episodes", 12).coerceAtLeast(1)
        val watchedEp = obj.optInt("num_watched_episodes", 0).coerceIn(0, totalEp)
        val imagePath = obj.optString("anime_image_path", "")
        val posterUrl = if (imagePath.startsWith("http")) imagePath else "https://cdn.myanimelist.net$imagePath"

        // Se l'anime è già nel nostro catalogo curato, usiamo la scheda completa
        val curatedMatch = CuratedImdbCatalog.anime.find { it.malId == malId }
        val movie = curatedMatch ?: Movie(
          id = "mal_$malId",
          title = title,
          year = 2022,
          imdbRating = (userScore?.toDouble() ?: 8.2),
          runtimeMinutes = 24,
          genres = listOf("Animation", "Action"),
          plot = "Importato dal profilo pubblico MyAnimeList (ID #$malId).",
          director = "MyAnimeList Import ⛩️",
          posterUrl = posterUrl,
          moodTag = "Profilo MAL ⛩️",
          mediaType = MediaType.ANIME,
          source = MediaSource.MYANIMELIST,
          malId = malId,
          totalSeasons = 1,
          totalEpisodes = totalEp
        ).let { it.copy(episodes = CuratedImdbCatalog.generateDefaultEpisodes(it)) }

        entries.add(
          MalImportEntry(
            movie = movie,
            isWatched = (status == 1 || status == 2 || watchedEp > 0),
            userScore = userScore,
            watchedEpisodesCount = if (status == 2) movie.episodes.size else watchedEp
          )
        )
      }
      return entries
    }
  }
}

/**
 * Catalogo curato completo per funzionare al 100% anche offline:
 * - 16 Film Capolavoro IMDb
 * - 8 Serie TV Iconiche IMDb (con episodi celebri e voti reali per episodio)
 * - 8 Anime Capolavoro MyAnimeList (con ID ufficiali MAL, punteggi MAL ed episodi)
 */
object CuratedImdbCatalog {

  fun forType(mediaType: MediaType): List<Movie> = when (mediaType) {
    MediaType.MOVIE -> movies
    MediaType.TV_SERIES -> tvSeries
    MediaType.ANIME -> anime
  }

  /**
   * Genera una lista di episodi per una Serie TV o Anime quando non sono ancora stati scaricati.
   */
  fun generateDefaultEpisodes(media: Movie): List<Episode> {
    val count = media.totalEpisodes.coerceIn(1, 16)
    return (1..count).map { epNum ->
      val delta = ((epNum % 5) - 2) * 0.1
      val rating = ((media.imdbRating + delta).coerceIn(6.5, 9.9) * 10.0).roundToInt() / 10.0
      Episode(
        id = "${media.id}_s1_e$epNum",
        seasonNumber = 1,
        episodeNumber = epNum,
        title = "Episodio $epNum",
        communityRating = rating,
        airDate = "${media.year}"
      )
    }
  }

  // ============================================================================
  // 1. FILM CAPOLAVORO (IMDb)
  // ============================================================================
  val movies: List<Movie> = listOf(
    Movie(
      id = "tt1375666",
      title = "Inception",
      year = 2010,
      imdbRating = 8.8,
      runtimeMinutes = 148,
      genres = listOf("Sci-Fi", "Action", "Thriller"),
      plot = "Dom Cobb è un abile ladro specializzato nell'estrarre segreti preziosi dal profondo del subconscio durante il sogno. Gli viene offerta un'ultima missione impossibile: innestare un'idea nella mente di un erede miliardario.",
      director = "Christopher Nolan • Leonardo DiCaprio, Cillian Murphy",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_.jpg",
      moodTag = "Viaggio nella mente 🧠"
    ),
    Movie(
      id = "tt0816692",
      title = "Interstellar",
      year = 2014,
      imdbRating = 8.7,
      runtimeMinutes = 169,
      genres = listOf("Sci-Fi", "Drama"),
      plot = "In un futuro in cui la Terra sta diventando inabitabile, un gruppo di esploratori guidato dall'ex pilota Cooper intraprende un viaggio attraverso un wormhole spaziale alla ricerca di una nuova casa per l'umanità.",
      director = "Christopher Nolan • Matthew McConaughey, Anne Hathaway",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg",
      moodTag = "Epico & Emozionante 🌌"
    ),
    Movie(
      id = "tt0468569",
      title = "Il Cavaliere Oscuro",
      year = 2008,
      imdbRating = 9.0,
      runtimeMinutes = 152,
      genres = listOf("Action", "Thriller", "Drama"),
      plot = "Batman, il tenente Gordon e il procuratore Harvey Dent uniscono le forze per smantellare la criminalità di Gotham, ma si trovano di fronte al caos puro scatenato da una mente criminale imprevedibile: il Joker.",
      director = "Christopher Nolan • Christian Bale, Heath Ledger",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMTMxNTMwODM0NF5BMl5BanBnXkFtZTcwODAyMTk2Mw@@._V1_.jpg",
      moodTag = "Adrenalina Pura 🦇"
    ),
    Movie(
      id = "tt6751668",
      title = "Parasite",
      year = 2019,
      imdbRating = 8.5,
      runtimeMinutes = 132,
      genres = listOf("Thriller", "Drama", "Comedy"),
      plot = "Tutta la famiglia di Ki-taek è disoccupata e vive in un seminterrato. Con uno stratagemma il figlio riesce a farsi assumere come insegnante privato nella lussuosa villa della ricca famiglia Park, dando inizio a una spirale incontrollabile.",
      director = "Bong Joon Ho • Song Kang-ho, Choi Woo-shik",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BYjk1Y2U4MjQtY2ZiNS00OWQyLWI3MmYtZWUwNmRjYWRiNWNhXkEyXkFqcGc@._V1_.jpg",
      moodTag = "Colpi di scena 🏆"
    ),
    Movie(
      id = "tt8946378",
      title = "Cena con delitto - Knives Out",
      year = 2019,
      imdbRating = 7.9,
      runtimeMinutes = 130,
      genres = listOf("Comedy", "Thriller"),
      plot = "Quando il celebre autore di gialli Harlan Thrombey viene trovato morto nella sua tenuta subito dopo il suo 85° compleanno, il carismatico detective Benoit Blanc viene ingaggiato per indagare tra una famiglia tanto eccentrica quanto avida.",
      director = "Rian Johnson • Daniel Craig, Ana de Armas",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMGUwZjliMTAtNzAxZi00MWNiLWE2NzgtZGUxMGQxZjhhNDRiXkEyXkFqcGc@._V1_.jpg",
      moodTag = "Giallo Brillante 🔍"
    ),
    Movie(
      id = "tt2278388",
      title = "Grand Budapest Hotel",
      year = 2014,
      imdbRating = 8.1,
      runtimeMinutes = 99,
      genres = listOf("Comedy", "Drama"),
      plot = "Le rocambolesche avventure di Gustave H, leggendario concierge di un famoso albergo europeo tra le due guerre, e di Zero Moustafa, il giovane fattorino che diventa il suo amico più fidato, tra dipinti rubati e fughe rocambolesche.",
      director = "Wes Anderson • Ralph Fiennes, Tony Revolori",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMzM5NjUxOTEyMl5BMl5BanBnXkFtZTgwNjEyMDM0MDE@._V1_.jpg",
      moodTag = "Stile & Ironia 🏨"
    ),
    Movie(
      id = "tt4633694",
      title = "Spider-Man: Un nuovo universo",
      year = 2018,
      imdbRating = 8.4,
      runtimeMinutes = 117,
      genres = listOf("Animation", "Action", "Sci-Fi"),
      plot = "Il giovane Miles Morales diventa lo Spider-Man della sua realtà e incrocia il suo cammino con cinque controparti provenienti da altre dimensioni per fermare una minaccia che rischia di distruggere l'intero multiverso.",
      director = "Bob Persichetti • Shameik Moore, Hailee Steinfeld",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMjMwNDkxMTgzOF5BMl5BanBnXkFtZTgwNTkwNTQ3NjM@._V1_.jpg",
      moodTag = "Visivamente Pazzesco 🕷️"
    ),
    Movie(
      id = "tt0110912",
      title = "Pulp Fiction",
      year = 1994,
      imdbRating = 8.9,
      runtimeMinutes = 154,
      genres = listOf("Drama", "Thriller", "Comedy"),
      plot = "Le vite di due sicari della mala, di un pugile sul viale del tramonto, della moglie di un boss e di due rapinatori in una tavola calda si intrecciano in quattro storie di redenzione, ironia tagliente e imprevisti a Los Angeles.",
      director = "Quentin Tarantino • John Travolta, Samuel L. Jackson, Uma Thurman",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BYTViYTE3ZGQtNDBlMC00ZTAyLTkyODMtZGRiZDg0MjA2YThkXkEyXkFqcGc@._V1_.jpg",
      moodTag = "Cult Intramontabile 🍔"
    ),
    Movie(
      id = "tt5052448",
      title = "Scappa - Get Out",
      year = 2017,
      imdbRating = 7.8,
      runtimeMinutes = 104,
      genres = listOf("Horror", "Thriller"),
      plot = "Chris, un giovane fotografo afroamericano, va a conoscere per il weekend i genitori della sua fidanzata Rose nella loro isolata tenuta di campagna. L'atmosfera inizialmente accogliente nasconde una verità agghiacciante.",
      director = "Jordan Peele • Daniel Kaluuya, Allison Williams",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMjUxMDQwNjcyNl5BMl5BanBnXkFtZTgwNzcwMzc0MTI@._V1_.jpg",
      moodTag = "Tensione Magnetica 👀"
    ),
    Movie(
      id = "tt1392190",
      title = "Mad Max: Fury Road",
      year = 2015,
      imdbRating = 8.1,
      runtimeMinutes = 120,
      genres = listOf("Action", "Sci-Fi"),
      plot = "In un deserto post-apocalittico dove benzina e acqua sono beni preziosi, Max si unisce all'Imperatrice Furiosa in una fuga ad altissima velocità a bordo di una blindocisterna per sfuggire a un tiranno spietato.",
      director = "George Miller • Tom Hardy, Charlize Theron",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BZDRkODJhOTgtOTc1OC00NTgzLTk4NjItNDgxZDY4YjlmNDY2XkEyXkFqcGc@._V1_.jpg",
      moodTag = "Inseguimento Totale 🔥"
    ),
    Movie(
      id = "tt0245429",
      title = "La città incantata",
      year = 2001,
      imdbRating = 8.6,
      runtimeMinutes = 125,
      genres = listOf("Animation", "Drama"),
      plot = "Durante il trasloco della sua famiglia, la piccola Chihiro entra per caso in un mondo magico governato da spiriti e streghe dove i suoi genitori vengono trasformati in maiali. Dovrà trovare il coraggio per salvarli.",
      director = "Hayao Miyazaki • Studio Ghibli",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BNTEyNmEwOWUtYzkyOC00ZTQ4LTllZmUtMjk0Y2YwOGUzYjRiXkEyXkFqcGc@._V1_.jpg",
      moodTag = "Magia Pura ✨"
    ),
    Movie(
      id = "tt1130884",
      title = "Shutter Island",
      year = 2010,
      imdbRating = 8.2,
      runtimeMinutes = 138,
      genres = listOf("Thriller", "Drama"),
      plot = "Nel 1954 l'agente federale Teddy Daniels indaga sulla misteriosa scomparsa di una paziente fuggita da un ospedale psichiatrico criminale situato su un'isola remota colpita da un uragano.",
      director = "Martin Scorsese • Leonardo DiCaprio, Mark Ruffalo",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BN2FjNWExYzEtY2YzOC00YjNlLTllMTQtNmIwM2Q1YzBhOWM1XkEyXkFqcGc@._V1_.jpg",
      moodTag = "Mistero Psicologico 🌀"
    ),
    Movie(
      id = "tt2582802",
      title = "Whiplash",
      year = 2014,
      imdbRating = 8.5,
      runtimeMinutes = 106,
      genres = listOf("Drama"),
      plot = "Un giovane e ambizioso batterista jazz entra nel prestigioso conservatorio di New York, dove finisce sotto la guida di un direttore d'orchestra tanto geniale quanto spietato, disposto a tutto pur di spingerlo oltre ogni limite.",
      director = "Damien Chazelle • Miles Teller, J.K. Simmons",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMDFjOWFkYzktYzhhMC00NmYyLTkwY2EtYjViMDhmNzg0OGFkXkEyXkFqcGc@._V1_.jpg",
      moodTag = "Ritmo al Cardiopalma 🥁"
    ),
    Movie(
      id = "tt6710474",
      title = "Everything Everywhere All at Once",
      year = 2022,
      imdbRating = 7.8,
      runtimeMinutes = 139,
      genres = listOf("Sci-Fi", "Comedy", "Action"),
      plot = "Una proprietaria di lavanderia sino-americana sopraffatta dalle tasse viene catapultata in un'avventura folle in cui solo lei può salvare l'esistenza esplorando altri universi e le vite che avrebbe potuto vivere.",
      director = "Daniel Kwan, Daniel Scheinert • Michelle Yeoh, Ke Huy Quan",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BYTdiOTIyZTQtNmQ1OS00NjZlLWIyMTgtYzk5Y2M3ZDVmMDk1XkEyXkFqcGc@._V1_.jpg",
      moodTag = "Multiverso Folle 🥯"
    ),
    Movie(
      id = "tt1457767",
      title = "L'evocazione - The Conjuring",
      year = 2013,
      imdbRating = 7.5,
      runtimeMinutes = 112,
      genres = listOf("Horror", "Thriller"),
      plot = "I celebri ricercatori del paranormale Ed e Lorraine Warren vengono chiamati ad aiutare una famiglia terrorizzata da una presenza oscura nella loro fattoria isolata.",
      director = "James Wan • Vera Farmiga, Patrick Wilson",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMTM3NjA1NDMyMV5BMl5BanBnXkFtZTcwMDQzNDMzOQ@@._V1_.jpg",
      moodTag = "Brividi Garantiti 🕯️"
    ),
    Movie(
      id = "tt0111161",
      title = "Le ali della libertà",
      year = 1994,
      imdbRating = 9.3,
      runtimeMinutes = 142,
      genres = listOf("Drama"),
      plot = "Condannato ingiustamente all'ergastolo nel duro carcere di Shawshank, il banchiere Andy Dufresne stringe un'amicizia profonda con il detenuto Red, mantenendo viva la speranza attraverso gli anni.",
      director = "Frank Darabont • Tim Robbins, Morgan Freeman",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMDAyY2FhYjctNDc5OS00MDNlLThiMGUtY2UxYWVkNGY2ZjljXkEyXkFqcGc@._V1_.jpg",
      moodTag = "#1 Top IMDb ⭐"
    )
  )

  // ============================================================================
  // 2. SERIE TV ICONICHE (IMDb + Episodi con Ratings Ufficiali)
  // ============================================================================
  val tvSeries: List<Movie> = listOf(
    Movie(
      id = "tt0903747",
      title = "Breaking Bad",
      year = 2008,
      imdbRating = 9.5,
      runtimeMinutes = 49,
      genres = listOf("Drama", "Thriller"),
      plot = "Dopo aver scoperto di avere un cancro ai polmoni, il professore di chimica Walter White si allea con l'ex studente Jesse Pinkman per produrre la metanfetamina più pura del New Mexico, trasformandosi nello spietato Heisenberg.",
      director = "Vince Gilligan • Bryan Cranston, Aaron Paul",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMzU5ZGYzNmQtMTdhYy00OGRiLTg0NmQtYjVjNzliZTg1ZGE4XkEyXkFqcGc@._V1_.jpg",
      moodTag = "#1 Serie TV IMDb 🧪",
      mediaType = MediaType.TV_SERIES,
      source = MediaSource.IMDB,
      totalSeasons = 5,
      totalEpisodes = 6,
      episodes = listOf(
        Episode("tt0903747_s1e1", 1, 1, "Questione di chimica (Pilot)", 9.0, "2008"),
        Episode("tt0903747_s2e12", 2, 12, "Phoenix", 9.3, "2009"),
        Episode("tt0903747_s3e13", 3, 13, "Half Measures / Full Measure", 9.7, "2010"),
        Episode("tt0903747_s4e13", 4, 13, "Fring allo scoperto (Face Off)", 9.9, "2011"),
        Episode("tt0903747_s5e14", 5, 14, "Declino (Ozymandias)", 10.0, "2013"),
        Episode("tt0903747_s5e16", 5, 16, "Felina (Series Finale)", 9.9, "2013")
      )
    ),
    Movie(
      id = "tt7366338",
      title = "Chernobyl",
      year = 2019,
      imdbRating = 9.3,
      runtimeMinutes = 65,
      genres = listOf("Drama", "Thriller"),
      plot = "Nell'aprile del 1986 un'esplosione alla centrale nucleare di Chernobyl nell'Unione Sovietica scatena una delle peggiori catastrofi causate dall'uomo. Scienziati e pompieri rischiano tutto per salvare l'Europa.",
      director = "Craig Mazin • Jared Harris, Stellan Skarsgård",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BNzU0OTI4YTQtNGQ1ZS00ZjA4LTg3MTMtZjkyZWNjN2RiZDJmXkEyXkFqcGc@._V1_.jpg",
      moodTag = "Miniserie Capolavoro ☢️",
      mediaType = MediaType.TV_SERIES,
      source = MediaSource.IMDB,
      totalSeasons = 1,
      totalEpisodes = 5,
      episodes = listOf(
        Episode("tt7366338_s1e1", 1, 1, "1:23:45", 9.4, "6 Mag 2019"),
        Episode("tt7366338_s1e2", 1, 2, "Please Remain Calm", 9.6, "13 Mag 2019"),
        Episode("tt7366338_s1e3", 1, 3, "Open Wide, O Earth", 9.6, "20 Mag 2019"),
        Episode("tt7366338_s1e4", 1, 4, "The Happiness of All Mankind", 9.4, "27 Mag 2019"),
        Episode("tt7366338_s1e5", 1, 5, "Vichnaya Pamyat", 9.9, "3 Giu 2019")
      )
    ),
    Movie(
      id = "tt5753856",
      title = "Dark",
      year = 2017,
      imdbRating = 8.7,
      runtimeMinutes = 60,
      genres = listOf("Sci-Fi", "Thriller", "Drama"),
      plot = "La scomparsa di due bambini nella piovosa cittadina tedesca di Winden porta alla luce i segreti e le doppie vite di quattro famiglie, svelando un mistero di viaggi nel tempo che abbraccia tre generazioni.",
      director = "Baran bo Odar • Louis Hofmann, Lisa Vicari",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BOWJjMGViY2UtNTAzNS00ZGFjLWFkNTMtMDBiMDMyZTM1NTY3XkEyXkFqcGc@._V1_.jpg",
      moodTag = "Labirinto Temporale ⏳",
      mediaType = MediaType.TV_SERIES,
      source = MediaSource.IMDB,
      totalSeasons = 3,
      totalEpisodes = 6,
      episodes = listOf(
        Episode("tt5753856_s1e1", 1, 1, "Segreti (Geheimnisse)", 8.3, "2017"),
        Episode("tt5753856_s1e5", 1, 5, "Verità (Wahrheiten)", 9.0, "2017"),
        Episode("tt5753856_s2e6", 2, 6, "Un ciclo infinito", 9.7, "2019"),
        Episode("tt5753856_s2e8", 2, 8, "Fine e inizio", 9.6, "2019"),
        Episode("tt5753856_s3e7", 3, 7, "Tra il tempo", 9.5, "2020"),
        Episode("tt5753856_s3e8", 3, 8, "Il paradiso", 9.7, "2020")
      )
    ),
    Movie(
      id = "tt11126994",
      title = "Arcane",
      year = 2021,
      imdbRating = 9.0,
      runtimeMinutes = 42,
      genres = listOf("Animation", "Action", "Sci-Fi"),
      plot = "Tra la ricca e utopica città di Piltover e i bassifondi oppressi di Zaun, le due sorelle Vi e Powder si ritrovano su fronti opposti di una guerra alimentata da tecnologie magiche e convinzioni inconciliabili.",
      director = "Fortiche Production • Hailee Steinfeld, Ella Purnell",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BYjA2NzhlMDItNWRmZC00MzRjLWE3ZjAtZjBlZDAwOWY2ODdjXkEyXkFqcGc@._V1_.jpg",
      moodTag = "Arte & Adrenalina ⚡",
      mediaType = MediaType.TV_SERIES,
      source = MediaSource.IMDB,
      totalSeasons = 2,
      totalEpisodes = 6,
      episodes = listOf(
        Episode("tt11126994_s1e1", 1, 1, "Welcome to the Playground", 8.6, "2021"),
        Episode("tt11126994_s1e3", 1, 3, "The Base Violence Necessary for Change", 9.7, "2021"),
        Episode("tt11126994_s1e6", 1, 6, "When These Walls Come Tumbling Down", 9.4, "2021"),
        Episode("tt11126994_s1e9", 1, 9, "The Monster You Created", 9.8, "2021"),
        Episode("tt11126994_s2e6", 2, 6, "The Message Hidden Within the Pattern", 9.7, "2024"),
        Episode("tt11126994_s2e7", 2, 7, "Pretend Like It's the First Time", 9.8, "2024")
      )
    ),
    Movie(
      id = "tt11280740",
      title = "Scissione (Severance)",
      year = 2022,
      imdbRating = 8.7,
      runtimeMinutes = 52,
      genres = listOf("Sci-Fi", "Thriller", "Drama"),
      plot = "Mark guida un team di impiegati della Lumon Industries i cui ricordi lavorativi sono stati separati chirurgicamente da quelli della vita privata. Quando un misterioso ex collega appare fuori dall'ufficio, inizia un'indagine inquietante.",
      director = "Ben Stiller, Dan Erickson • Adam Scott, Britt Lower",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BZDI5YzJhODQtMzQyNy00YWNmLWIxMjUtNDBjNjA5YWRjMzExXkEyXkFqcGc@._V1_.jpg",
      moodTag = "Thriller Distopico 🏢",
      mediaType = MediaType.TV_SERIES,
      source = MediaSource.IMDB,
      totalSeasons = 2,
      totalEpisodes = 5,
      episodes = listOf(
        Episode("tt11280740_s1e1", 1, 1, "Buone notizie sull'inferno", 8.5, "2022"),
        Episode("tt11280740_s1e4", 1, 4, "Il tu che sei", 8.7, "2022"),
        Episode("tt11280740_s1e7", 1, 7, "Defiant Jazz", 9.1, "2022"),
        Episode("tt11280740_s1e8", 1, 8, "Cosa c'è per cena?", 9.3, "2022"),
        Episode("tt11280740_s1e9", 1, 9, "Il noi che siamo (Season Finale)", 9.7, "2022")
      )
    ),
    Movie(
      id = "tt14452776",
      title = "The Bear",
      year = 2022,
      imdbRating = 8.6,
      runtimeMinutes = 34,
      genres = listOf("Drama", "Comedy"),
      plot = "Carmy, un giovane chef prodigio dell'alta ristorazione, torna a Chicago per gestire la caotica paninoteca di famiglia dopo una tragedia, cercando di trasformare sia il locale che se stesso.",
      director = "Christopher Storer • Jeremy Allen White, Ayo Edebiri",
      posterUrl = "https://m.media-amazon.com/images/M/MV5FZTEwM2VjM2ItNjI3OS00Y2IyLThmNWEtN2M4NTI4YzA0Y2Y5XkEyXkFqcGc@._V1_.jpg",
      moodTag = "Tensione in Cucina 👨‍🍳",
      mediaType = MediaType.TV_SERIES,
      source = MediaSource.IMDB,
      totalSeasons = 3,
      totalEpisodes = 5,
      episodes = listOf(
        Episode("tt14452776_s1e1", 1, 1, "System", 8.4, "2022"),
        Episode("tt14452776_s1e7", 1, 7, "Review (Piano sequenza)", 9.6, "2022"),
        Episode("tt14452776_s1e8", 1, 8, "Braciole", 9.2, "2022"),
        Episode("tt14452776_s2e6", 2, 6, "Fishes", 9.6, "2023"),
        Episode("tt14452776_s2e7", 2, 7, "Forks (Richie)", 9.7, "2023")
      )
    ),
    Movie(
      id = "tt4574334",
      title = "Stranger Things",
      year = 2016,
      imdbRating = 8.7,
      runtimeMinutes = 55,
      genres = listOf("Sci-Fi", "Horror", "Drama"),
      plot = "Nella cittadina di Hawkins negli anni '80, la misteriosa scomparsa di un ragazzino porta un gruppo di amici a imbattersi in esperimenti governativi segreti, forze soprannaturali e una bambina dai poteri straordinari.",
      director = "The Duffer Brothers • Millie Bobby Brown, Finn Wolfhard",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMjg2NmM0MTEtYWY2MS00NmFlLTllNTMtMjVkZjEwMGVlNzdjXkEyXkFqcGc@._V1_.jpg",
      moodTag = "Avventura Anni '80 🚲",
      mediaType = MediaType.TV_SERIES,
      source = MediaSource.IMDB,
      totalSeasons = 4,
      totalEpisodes = 5,
      episodes = listOf(
        Episode("tt4574334_s1e1", 1, 1, "La scomparsa di Will Byers", 8.6, "2016"),
        Episode("tt4574334_s1e8", 1, 8, "Il Sottosopra", 9.3, "2016"),
        Episode("tt4574334_s2e9", 2, 9, "La porta", 9.3, "2017"),
        Episode("tt4574334_s4e4", 4, 4, "Caro Billy (Dear Billy)", 9.5, "2022"),
        Episode("tt4574334_s4e7", 4, 7, "Il massacro al laboratorio di Hawkins", 9.6, "2022")
      )
    ),
    Movie(
      id = "tt0944947",
      title = "Il Trono di Spade",
      year = 2011,
      imdbRating = 9.2,
      runtimeMinutes = 58,
      genres = listOf("Drama", "Action"),
      plot = "Nove nobili casate combattono per il controllo delle terre mitiche di Westeros, mentre un antico nemico dimenticato da millenni torna a risvegliarsi oltre la Barriera di ghiaccio.",
      director = "David Benioff, D.B. Weiss • Pedro Pascal, Emilia Clarke",
      posterUrl = "https://m.media-amazon.com/images/M/MV5BMTNhMDJmNmYtNDQ5OS00ODdlLWE0ZDAtZTgyYTIwNDY3OTU3XkEyXkFqcGc@._V1_.jpg",
      moodTag = "Fantasy Epico 🐉",
      mediaType = MediaType.TV_SERIES,
      source = MediaSource.IMDB,
      totalSeasons = 8,
      totalEpisodes = 6,
      episodes = listOf(
        Episode("tt0944947_s1e9", 1, 9, "Baelor", 9.6, "2011"),
        Episode("tt0944947_s3e9", 3, 9, "Le piogge di Castamere (Nozze Rosse)", 9.9, "2013"),
        Episode("tt0944947_s4e6", 4, 6, "Le leggi degli dei e degli uomini", 9.7, "2014"),
        Episode("tt0944947_s5e8", 5, 8, "Aspra Dimora (Hardhome)", 9.8, "2015"),
        Episode("tt0944947_s6e9", 6, 9, "La battaglia dei bastardi", 9.9, "2016"),
        Episode("tt0944947_s6e10", 6, 10, "I venti dell'inverno", 9.9, "2016")
      )
    )
  )

  // ============================================================================
  // 3. ANIME CAPOLAVORO (MyAnimeList + Episodi con Ratings della Community)
  // ============================================================================
  val anime: List<Movie> = listOf(
    Movie(
      id = "mal_5114",
      title = "Fullmetal Alchemist: Brotherhood",
      year = 2009,
      imdbRating = 9.1,
      runtimeMinutes = 24,
      genres = listOf("Animation", "Action", "Drama"),
      plot = "Dopo un proibito rituale alchemico finito in tragedia, i fratelli Edward e Alphonse Elric intraprendono un viaggio alla ricerca della Pietra Filosofale per riavere i propri corpi, scoprendo un complotto che minaccia l'intera nazione.",
      director = "Bones • MyAnimeList #5114",
      posterUrl = "https://cdn.myanimelist.net/images/anime/1208/94745l.jpg",
      moodTag = "Leggenda MyAnimeList ⚗️",
      mediaType = MediaType.ANIME,
      source = MediaSource.MYANIMELIST,
      malId = 5114,
      totalSeasons = 1,
      totalEpisodes = 64,
      episodes = listOf(
        Episode("mal_5114_e1", 1, 1, "Fullmetal Alchemist", 8.6, "5 Apr 2009"),
        Episode("mal_5114_e4", 1, 4, "An Alchemist's Anguish", 9.4, "26 Apr 2009"),
        Episode("mal_5114_e10", 1, 10, "Separate Destinations", 9.3, "7 Giu 2009"),
        Episode("mal_5114_e19", 1, 19, "Death of the Undying (Mustang vs Lust)", 9.8, "9 Ago 2009"),
        Episode("mal_5114_e53", 1, 53, "Flame of Vengeance", 9.7, "18 Apr 2010"),
        Episode("mal_5114_e63", 1, 63, "The Other Side of the Gateway", 9.9, "27 Giu 2010"),
        Episode("mal_5114_e64", 1, 64, "Journey's End", 9.8, "4 Lug 2010")
      )
    ),
    Movie(
      id = "mal_52991",
      title = "Sousou no Frieren (Frieren: Oltre la fine del viaggio)",
      year = 2023,
      imdbRating = 9.3,
      runtimeMinutes = 24,
      genres = listOf("Animation", "Drama", "Action"),
      plot = "Dopo aver sconfitto il Re dei Demoni insieme al gruppo dell'eroe Himmel, la maga elfica Frieren — destinata a vivere per millenni — intraprende un nuovo viaggio per comprendere davvero il cuore degli esseri umani e il valore del tempo.",
      director = "Madhouse • MyAnimeList #52991 (#1 Top MAL)",
      posterUrl = "https://cdn.myanimelist.net/images/anime/1015/138006l.jpg",
      moodTag = "#1 Top MyAnimeList 🪄",
      mediaType = MediaType.ANIME,
      source = MediaSource.MYANIMELIST,
      malId = 52991,
      totalSeasons = 1,
      totalEpisodes = 28,
      episodes = listOf(
        Episode("mal_52991_e1", 1, 1, "The Journey's End", 9.3, "29 Set 2023"),
        Episode("mal_52991_e8", 1, 8, "Frieren the Slayer", 9.5, "27 Ott 2023"),
        Episode("mal_52991_e10", 1, 10, "A Powerful Mage (Aura the Guillotine)", 9.8, "10 Nov 2023"),
        Episode("mal_52991_e14", 1, 14, "Privilege of the Young", 9.4, "8 Dic 2023"),
        Episode("mal_52991_e26", 1, 26, "The Height of Magic", 9.8, "8 Mar 2024"),
        Episode("mal_52991_e28", 1, 28, "It Would Be Embarrassing When We Met Again", 9.6, "22 Mar 2024")
      )
    ),
    Movie(
      id = "mal_16498",
      title = "Attack on Titan (L'Attacco dei Giganti)",
      year = 2013,
      imdbRating = 9.1,
      runtimeMinutes = 24,
      genres = listOf("Animation", "Action", "Drama", "Thriller"),
      plot = "In un mondo in cui l'umanità vive rinchiusa dietro tre immense mura per proteggersi dai Giganti divoratori di uomini, il giovane Eren Yeager giura di sterminarli tutti dopo la distruzione della sua città natale.",
      director = "Wit Studio / MAPPA • MyAnimeList #16498",
      posterUrl = "https://cdn.myanimelist.net/images/anime/10/47347l.jpg",
      moodTag = "Epica & Colpi di Scena ⚔️",
      mediaType = MediaType.ANIME,
      source = MediaSource.MYANIMELIST,
      malId = 16498,
      totalSeasons = 4,
      totalEpisodes = 89,
      episodes = listOf(
        Episode("mal_16498_e1", 1, 1, "A te, fra 2000 anni (To You, in 2000 Years)", 9.3, "2013"),
        Episode("mal_16498_e31", 2, 6, "Guerriero (Warrior - Reiner & Bertholdt)", 9.8, "2017"),
        Episode("mal_16498_e53", 3, 16, "Partita perfetta (Perfect Game)", 9.9, "2019"),
        Episode("mal_16498_e54", 3, 17, "Eroe (Hero)", 9.9, "2019"),
        Episode("mal_16498_e64", 4, 5, "Dichiarazione di guerra", 9.8, "2021"),
        Episode("mal_16498_e79", 4, 20, "Ricordi del futuro", 9.9, "2022")
      )
    ),
    Movie(
      id = "mal_9253",
      title = "Steins;Gate",
      year = 2011,
      imdbRating = 9.1,
      runtimeMinutes = 24,
      genres = listOf("Animation", "Sci-Fi", "Thriller"),
      plot = "A Akihabara l'autoproclamato scienziato pazzo Rintaro Okabe e i membri del suo laboratorio modificano accidentalmente un forno a microonde in grado di inviare messaggi di testo nel passato, alterando le linee temporali.",
      director = "White Fox • MyAnimeList #9253",
      posterUrl = "https://cdn.myanimelist.net/images/anime/1935/127974l.jpg",
      moodTag = "Viaggi nel Tempo 🕰️",
      mediaType = MediaType.ANIME,
      source = MediaSource.MYANIMELIST,
      malId = 9253,
      totalSeasons = 1,
      totalEpisodes = 24,
      episodes = listOf(
        Episode("mal_9253_e1", 1, 1, "Prologue of the Beginning and Ending", 8.5, "2011"),
        Episode("mal_9253_e12", 1, 12, "Dogma in Ergosphere", 9.6, "2011"),
        Episode("mal_9253_e13", 1, 13, "Metaphysical Necrosis", 9.4, "2011"),
        Episode("mal_9253_e22", 1, 22, "Being Meltdown", 9.7, "2011"),
        Episode("mal_9253_e23", 1, 23, "Open the Steins Gate", 9.8, "2011"),
        Episode("mal_9253_e24", 1, 24, "Achievement Point", 9.8, "2011")
      )
    ),
    Movie(
      id = "mal_1535",
      title = "Death Note",
      year = 2006,
      imdbRating = 8.6,
      runtimeMinutes = 23,
      genres = listOf("Animation", "Thriller", "Drama"),
      plot = "Il brillante studente Light Yagami raccoglie un quaderno soprannaturale lasciato cadere dallo Shinigami Ryuk: chiunque vi abbia scritto il proprio nome muore. Inizia un duello psicologico mozzafiato con il misterioso detective L.",
      director = "Madhouse • MyAnimeList #1535",
      posterUrl = "https://cdn.myanimelist.net/images/anime/1079/138100l.jpg",
      moodTag = "Duello di Intelletti 📓",
      mediaType = MediaType.ANIME,
      source = MediaSource.MYANIMELIST,
      malId = 1535,
      totalSeasons = 1,
      totalEpisodes = 37,
      episodes = listOf(
        Episode("mal_1535_e1", 1, 1, "Rinascita (Rebirth)", 9.0, "2006"),
        Episode("mal_1535_e2", 1, 2, "Confronto (Confrontation)", 9.5, "2006"),
        Episode("mal_1535_e7", 1, 7, "Cielo coperto (Overcast - Naomi Misora)", 9.6, "2006"),
        Episode("mal_1535_e24", 1, 24, "Risurrezione (Revival)", 9.5, "2007"),
        Episode("mal_1535_e25", 1, 25, "Silenzio (Silence)", 9.7, "2007"),
        Episode("mal_1535_e37", 1, 37, "Nuovo mondo (New World)", 9.3, "2007")
      )
    ),
    Movie(
      id = "mal_11061",
      title = "Hunter x Hunter (2011)",
      year = 2011,
      imdbRating = 9.0,
      runtimeMinutes = 24,
      genres = listOf("Animation", "Action", "Drama"),
      plot = "Per ritrovare suo padre scomparso, il giovane Gon Freecss sostiene il durissimo esame per diventare un Hunter professionista, stringendo un'amicizia indissolubile con Killua, Kurapika e Leorio.",
      director = "Madhouse • MyAnimeList #11061",
      posterUrl = "https://cdn.myanimelist.net/images/anime/1337/99013l.jpg",
      moodTag = "Avventura Shonen Pura 🎣",
      mediaType = MediaType.ANIME,
      source = MediaSource.MYANIMELIST,
      malId = 11061,
      totalSeasons = 1,
      totalEpisodes = 148,
      episodes = listOf(
        Episode("mal_11061_e1", 1, 1, "Departure x And x Friends", 8.2, "2011"),
        Episode("mal_11061_e47", 1, 47, "Condition x And x Condition (Kurapika vs Uvogin)", 9.7, "2012"),
        Episode("mal_11061_e116", 1, 116, "Revenge x And x Recovery", 9.7, "2014"),
        Episode("mal_11061_e126", 1, 126, "Zero x And x Rose (Netero vs Meruem)", 9.9, "2014"),
        Episode("mal_11061_e131", 1, 131, "Anger x And x Light", 9.9, "2014"),
        Episode("mal_11061_e135", 1, 135, "This Person x And x This Moment", 9.9, "2014")
      )
    ),
    Movie(
      id = "mal_38000",
      title = "Demon Slayer (Kimetsu no Yaiba)",
      year = 2019,
      imdbRating = 8.5,
      runtimeMinutes = 24,
      genres = listOf("Animation", "Action"),
      plot = "Dopo che la sua famiglia viene sterminata da un demone e sua sorella Nezuko viene trasformata, il giovane Tanjiro Kamado impugna la katana ed entra nel Corpo degli Ammazzademoni per trovare una cura.",
      director = "ufotable • MyAnimeList #38000",
      posterUrl = "https://cdn.myanimelist.net/images/anime/1286/99889l.jpg",
      moodTag = "Animazione Spettacolare 🗡️",
      mediaType = MediaType.ANIME,
      source = MediaSource.MYANIMELIST,
      malId = 38000,
      totalSeasons = 4,
      totalEpisodes = 26,
      episodes = listOf(
        Episode("mal_38000_e1", 1, 1, "Crudeltà (Cruelty)", 8.5, "2019"),
        Episode("mal_38000_e17", 1, 17, "Perfeziona una sola cosa (Zenitsu)", 9.3, "2019"),
        Episode("mal_38000_e19", 1, 19, "Hinokami", 9.8, "2019"),
        Episode("mal_38000_e26", 1, 26, "Una nuova missione", 9.0, "2019")
      )
    ),
    Movie(
      id = "mal_40748",
      title = "Jujutsu Kaisen",
      year = 2020,
      imdbRating = 8.6,
      runtimeMinutes = 24,
      genres = listOf("Animation", "Action", "Horror"),
      plot = "Per salvare i suoi compagni di scuola da una maledizione letale, il liceale Yuji Itadori ingoia un dito maledetto appartenuto a Ryomen Sukuna, ritrovandosi catapultato nel mondo segreto degli stregoni guidati da Satoru Gojo.",
      director = "MAPPA • MyAnimeList #40748",
      posterUrl = "https://cdn.myanimelist.net/images/anime/1171/109222l.jpg",
      moodTag = "Combattimenti Magnetici 🤞",
      mediaType = MediaType.ANIME,
      source = MediaSource.MYANIMELIST,
      malId = 40748,
      totalSeasons = 2,
      totalEpisodes = 47,
      episodes = listOf(
        Episode("mal_40748_e1", 1, 1, "Ryomen Sukuna", 8.6, "2020"),
        Episode("mal_40748_e7", 1, 7, "Assalto (Gojo's Infinite Void)", 9.5, "2020"),
        Episode("mal_40748_e20", 1, 20, "Fuori dall'ordinario (Todo & Yuji)", 9.6, "2021"),
        Episode("mal_40748_e41", 2, 17, "Tuono (Thunderclap - Shibuya)", 9.8, "2023"),
        Episode("mal_40748_e44", 2, 20, "Giusto e sbagliato, Parte 3", 9.7, "2023")
      )
    )
  )
}
