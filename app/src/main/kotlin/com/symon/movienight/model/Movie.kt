package com.symon.movienight.model

/**
 * Indica il tipo di contenuto multimediale: Film, Serie TV o Anime.
 */
enum class MediaType(val label: String, val badgeLabel: String) {
  MOVIE("Film 🎬", "FILM"),
  TV_SERIES("Serie TV 📺", "SERIE TV"),
  ANIME("Anime ⛩️", "ANIME")
}

/**
 * Indica da quale piattaforma provengono i dati e il voto ufficiale.
 */
enum class MediaSource(val badgeText: String) {
  IMDB("IMDb"),
  MYANIMELIST("MAL")
}

/**
 * Le 3 sezioni principali della barra di navigazione in basso.
 */
enum class AppTab(val label: String) {
  DISCOVER("Scopri"),
  WATCHLIST("Da Vedere"),
  WATCHED("Già Visti")
}

/**
 * Rappresenta un singolo episodio di una Serie TV o di un Anime.
 *
 * Contiene sia il voto medio ufficiale della community (IMDb o MyAnimeList)
 * sia i metadati dell'episodio (stagione, numero, titolo).
 */
data class Episode(
  val id: String,                 // Identificativo univoco (es. "tt0903747_s1_e1" o "mal_5114_e1")
  val seasonNumber: Int = 1,      // Numero della stagione (1 per la maggior parte degli anime)
  val episodeNumber: Int,         // Numero progressivo dell'episodio
  val title: String,              // Titolo dell'episodio
  val communityRating: Double,    // Voto medio ufficiale IMDb / MyAnimeList (su 10.0)
  val airDate: String = ""        // Data di uscita o durata (opzionale)
) {
  /**
   * Codice compatto dell'episodio (es. "S01E01" per le serie TV o "EP 01" per gli anime).
   */
  fun formattedCode(isAnime: Boolean = false): String {
    return if (isAnime && seasonNumber <= 1) {
      "EP %02d".format(episodeNumber)
    } else {
      "S%02dE%02d".format(seasonNumber, episodeNumber)
    }
  }
}

/**
 * Conserva i progressi e i voti personali dell'utente per un titolo (Film, Serie TV o Anime):
 * - Il voto complessivo dato dall'utente (`userRating`, da 1 a 10 stelle)
 * - Gli episodi spuntati come "Visti" (`watchedEpisodeIds`)
 * - Il voto personale assegnato a ciascun singolo episodio (`episodeRatings`, da 1 a 10)
 */
data class UserMediaProgress(
  val mediaId: String,
  val userRating: Int? = null,
  val watchedEpisodeIds: Set<String> = emptySet(),
  val episodeRatings: Map<String, Int> = emptyMap()
)

/**
 * Rappresenta un contenuto del catalogo (Film, Serie TV o Anime da IMDb / MyAnimeList).
 *
 * Manteniamo il nome `Movie` per compatibilità con tutta l'architettura esistente,
 * arricchendolo con il supporto a Serie TV, Anime, MyAnimeList ed Episodi.
 */
data class Movie(
  val id: String,                              // Codice univoco (es. "tt1375666" o "mal_5114")
  val title: String,                           // Titolo dell'opera
  val year: Int,                               // Anno di uscita
  val imdbRating: Double,                      // Voto medio ufficiale su 10 (IMDb o MyAnimeList Score)
  val runtimeMinutes: Int,                     // Durata in minuti (totale per film, per episodio per serie/anime)
  val genres: List<String>,                    // Elenco generi (es. ["Sci-Fi", "Action"])
  val plot: String,                            // Trama / sinossi
  val director: String,                        // Regista/Cast oppure Studio di animazione (es. "MAPPA", "Madhouse")
  val posterUrl: String,                       // URL della locandina ufficiale IMDb o MyAnimeList CDN
  val moodTag: String = "",                    // Etichetta rapida per la serata
  val mediaType: MediaType = MediaType.MOVIE,  // Tipo: Film, Serie TV o Anime
  val source: MediaSource = MediaSource.IMDB,  // Fonte dati: IMDb o MyAnimeList (MAL)
  val malId: Int? = null,                      // ID numerico ufficiale su MyAnimeList (se è un Anime)
  val totalSeasons: Int = 1,                   // Numero di stagioni (per Serie TV)
  val totalEpisodes: Int = 1,                  // Numero totale di episodi (per Serie TV e Anime)
  val episodes: List<Episode> = emptyList()    // Lista degli episodi con i voti della community
) {
  /**
   * Indica se questo titolo ha episodi tracciabili (Serie TV o Anime).
   */
  val isEpisodic: Boolean
    get() = mediaType == MediaType.TV_SERIES || mediaType == MediaType.ANIME || episodes.isNotEmpty() || totalEpisodes > 1

  /**
   * Restituisce la durata o il formato in modo leggibile:
   * - Per i Film: "2h 28m"
   * - Per le Serie TV: "5 stagioni • 62 ep. (48m/ep)"
   * - Per gli Anime: "64 ep. • 24m/ep"
   */
  val formattedRuntime: String
    get() {
      return when (mediaType) {
        MediaType.MOVIE -> {
          if (runtimeMinutes <= 0) return "N/D"
          val hours = runtimeMinutes / 60
          val mins = runtimeMinutes % 60
          if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
        MediaType.TV_SERIES -> {
          val epCount = if (episodes.size > totalEpisodes) episodes.size else totalEpisodes
          val seasonLabel = if (totalSeasons > 1) "$totalSeasons stagioni • " else ""
          "${seasonLabel}$epCount ep."
        }
        MediaType.ANIME -> {
          val epCount = if (episodes.size > totalEpisodes) episodes.size else totalEpisodes
          if (epCount > 1) "$epCount episodi • ${runtimeMinutes}m/ep" else "Film Anime • ${runtimeMinutes}m"
        }
      }
    }

  /**
   * URL diretto alla scheda ufficiale:
   * - Su MyAnimeList (`https://myanimelist.net/anime/{malId}`) per gli Anime
   * - Su IMDb (`https://www.imdb.com/title/{id}/`) per Film e Serie TV
   */
  val externalPageUrl: String
    get() = if (source == MediaSource.MYANIMELIST && malId != null) {
      "https://myanimelist.net/anime/$malId"
    } else if (id.startsWith("mal_")) {
      val cleanId = id.removePrefix("mal_")
      "https://myanimelist.net/anime/$cleanId"
    } else {
      "https://www.imdb.com/title/$id/"
    }

  /**
   * Mantenuto per retrocompatibilità: restituisce il link esterno (IMDb o MyAnimeList).
   */
  val imdbPageUrl: String
    get() = externalPageUrl
}

/**
 * Categorie di filtro rapido per scegliere il genere della serata.
 */
enum class MovieGenreFilter(val label: String, val apiGenre: String?) {
  ALL("Tutti ✨", null),
  SCI_FI("Sci-Fi 🚀", "Sci-Fi"),
  ACTION("Azione 💥", "Action"),
  THRILLER("Thriller 🕵️", "Thriller"),
  COMEDY("Commedia 😂", "Comedy"),
  DRAMA("Dramma 🎭", "Drama"),
  ANIMATION("Animazione / Fantasy 🐉", "Animation"),
  HORROR("Horror / Dark 👻", "Horror")
}
