package com.symon.movienight.data

import com.symon.movienight.model.Movie
import com.symon.movienight.model.MovieGenreFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Interfaccia del Repository dei film.
 * Separare l'interfaccia dall'implementazione ci permette di testare facilmente
 * il ViewModel nei test unitari (Fase 2).
 */
interface MovieRepository {
  suspend fun getCatalog(genreFilter: MovieGenreFilter = MovieGenreFilter.ALL): List<Movie>
  suspend fun searchLiveImdb(query: String): List<Movie>
}

/**
 * Implementazione principale del Repository IMDb.
 *
 * Unisce:
 * 1. Il collegamento live alle API pubbliche e gratuite di IMDb (senza API key):
 *    - `api.imdbapi.dev` (Free IMDb REST API)
 *    - `v3.sg.media-imdb.com/suggestion` (Endpoint JSON ufficiale IMDb per ricerca live e locandine)
 * 2. Un ricco catalogo curato Top IMDb integrato (`CuratedImdbCatalog`) che garantisce
 *    trame in italiano, voti, durate e funzionamento immediato anche offline!
 */
class DefaultMovieRepository : MovieRepository {

  override suspend fun getCatalog(genreFilter: MovieGenreFilter): List<Movie> =
    withContext(Dispatchers.IO) {
      val remoteMovies = fetchFromFreeImdbApi(genreFilter.apiGenre)
      val baseList = if (remoteMovies.isNotEmpty()) {
        // Uniamo eventuali risultati remoti con il catalogo curato evitando duplicati per ID IMDb
        (remoteMovies + CuratedImdbCatalog.movies).distinctBy { it.id }
      } else {
        CuratedImdbCatalog.movies
      }

      val filtered = if (genreFilter.apiGenre == null) {
        baseList
      } else {
        baseList.filter { movie ->
          movie.genres.any { it.equals(genreFilter.apiGenre, ignoreCase = true) }
        }
      }
      filtered.shuffled()
    }

  override suspend fun searchLiveImdb(query: String): List<Movie> =
    withContext(Dispatchers.IO) {
      val cleanQuery = query.trim()
      if (cleanQuery.isEmpty()) return@withContext CuratedImdbCatalog.movies

      // 1. Cerchiamo prima anche nel nostro catalogo curato locale
      val localMatches = CuratedImdbCatalog.movies.filter {
        it.title.contains(cleanQuery, ignoreCase = true) ||
          it.director.contains(cleanQuery, ignoreCase = true) ||
          it.plot.contains(cleanQuery, ignoreCase = true)
      }

      // 2. Interroghiamo in tempo reale l'endpoint pubblico ufficiale di IMDb (senza chiave API)
      val liveMatches = fetchFromImdbSuggestionApi(cleanQuery)

      (localMatches + liveMatches).distinctBy { it.id }
    }

  /**
   * Tenta di scaricare i film da `https://api.imdbapi.dev/titles`.
   * Se il servizio esterno non è raggiungibile, cattura l'eccezione in sicurezza
   * e restituisce una lista vuota lasciando spazio al catalogo integrato.
   */
  private fun fetchFromFreeImdbApi(genre: String?): List<Movie> {
    return try {
      val urlString = buildString {
        append("https://api.imdbapi.dev/titles?types=MOVIE&sortBy=SORT_BY_POPULARITY&sortOrder=ASC")
        if (!genre.isNullOrBlank()) {
          append("&genres=").append(URLEncoder.encode(genre, "UTF-8"))
        }
      }
      val jsonText = httpGet(urlString) ?: return emptyList()
      parseImdbApiDevJson(jsonText)
    } catch (_: Exception) {
      emptyList()
    }
  }

  /**
   * Interroga l'endpoint JSON pubblico ufficiale di IMDb (`v3.sg.media-imdb.com`).
   * Funziona senza alcuna registrazione o API key e restituisce film reali con poster Amazon/IMDb.
   */
  private fun fetchFromImdbSuggestionApi(query: String): List<Movie> {
    return try {
      val encoded = URLEncoder.encode(query.lowercase(), "UTF-8")
      val firstChar = encoded.firstOrNull { it.isLetterOrDigit() } ?: 'x'
      val urlString = "https://v3.sg.media-imdb.com/suggestion/$firstChar/$encoded.json"
      val jsonText = httpGet(urlString) ?: return emptyList()
      parseImdbSuggestionJson(jsonText)
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
      setRequestProperty("User-Agent", "MovieNightAndroid/1.0")
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
    fun parseImdbApiDevJson(jsonText: String): List<Movie> {
      val root = JSONObject(jsonText)
      val titlesArray = root.optJSONArray("titles") ?: return emptyList()
      val result = mutableListOf<Movie>()

      for (i in 0 until titlesArray.length()) {
        val item = titlesArray.optJSONObject(i) ?: continue
        val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
        val title = item.optString("primaryTitle").ifBlank { item.optString("originalTitle") }
        if (title.isBlank()) continue

        val year = item.optInt("startYear", 2020)
        val runtime = item.optInt("runtimeMinutes", 120)
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

        result.add(
          Movie(
            id = id,
            title = title,
            year = year,
            imdbRating = rating,
            runtimeMinutes = runtime,
            genres = genresList,
            plot = plot,
            director = "Catalogo IMDb Live",
            posterUrl = posterUrl,
            moodTag = "IMDb Live 🌐"
          )
        )
      }
      return result
    }

    /**
     * Effettua il parsing del JSON pubblico ufficiale di IMDb (`v3.sg.media-imdb.com/suggestion`).
     */
    fun parseImdbSuggestionJson(jsonText: String): List<Movie> {
      val root = JSONObject(jsonText)
      val dArray = root.optJSONArray("d") ?: return emptyList()
      val result = mutableListOf<Movie>()

      for (i in 0 until dArray.length()) {
        val item = dArray.optJSONObject(i) ?: continue
        val qid = item.optString("qid")
        if (qid != "movie" && qid != "feature") continue

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

        result.add(
          Movie(
            id = id,
            title = title,
            year = year,
            imdbRating = estimatedRating,
            runtimeMinutes = 120,
            genres = listOf("Drama", "Action"),
            plot = "Risultato in tempo reale dal database ufficiale IMDb ($id). Cast principale: $stars.",
            director = stars,
            posterUrl = posterUrl,
            moodTag = "Ricerca Live IMDb 🔎"
          )
        )
      }
      return result
    }
  }
}

/**
 * Catalogo curato dei capolavori e film più amati su IMDb, con poster ufficiali
 * Amazon/IMDb CDN, voti reali, durata, generi e sinossi in italiano.
 */
object CuratedImdbCatalog {
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
}
