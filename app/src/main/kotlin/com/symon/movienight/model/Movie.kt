package com.symon.movienight.model

import java.net.URLEncoder

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
 * Rappresenta una piattaforma di streaming su cui guardare il programma,
 * con link diretto di redirect.
 */
data class StreamingProvider(
  val name: String,
  val url: String,
  val badgeColorHex: Long = 0xFFE50914
)

/**
 * Rappresenta un singolo episodio di una Serie TV o di un Anime.
 */
data class Episode(
  val id: String,                 // Identificativo univoco (es. "tt0903747_s1_e1" o "mal_5114_e1")
  val seasonNumber: Int = 1,      // Numero della stagione
  val episodeNumber: Int,         // Numero progressivo dell'episodio
  val title: String,              // Titolo dell'episodio
  val communityRating: Double,    // Voto medio ufficiale IMDb / MyAnimeList (su 10.0)
  val airDate: String = ""        // Data di uscita o info aggiuntive
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
 */
data class Movie(
  val id: String,                              // Codice univoco (es. "tt1375666" o "mal_5114")
  val title: String,                           // Titolo dell'opera
  val year: Int,                               // Anno di uscita
  val imdbRating: Double,                      // Voto medio ufficiale su 10 (IMDb o MyAnimeList Score)
  val runtimeMinutes: Int,                     // Durata in minuti (totale per film, per episodio per serie/anime)
  val genres: List<String>,                    // Elenco generi reali (es. ["Sci-Fi", "Action"])
  val plot: String,                            // Trama completa
  val director: String,                        // Regista o Studio di animazione (es. "Christopher Nolan", "MAPPA")
  val posterUrl: String,                       // URL della locandina ufficiale IMDb o MyAnimeList CDN
  val moodTag: String = "",                    // Etichetta rapida per la serata
  val mediaType: MediaType = MediaType.MOVIE,  // Tipo: Film, Serie TV o Anime
  val source: MediaSource = MediaSource.IMDB,  // Fonte dati: IMDb o MyAnimeList (MAL)
  val malId: Int? = null,                      // ID numerico ufficiale su MyAnimeList (se è un Anime)
  val totalSeasons: Int = 1,                   // Numero di stagioni (per Serie TV)
  val totalEpisodes: Int = 1,                  // Numero totale reale di episodi (per Serie TV e Anime)
  val episodes: List<Episode> = emptyList(),   // Lista degli episodi con i voti della community
  val cast: List<String> = emptyList(),        // Attori principali o personaggi/doppiatori
  val streamingPlatforms: List<String> = emptyList(), // Piattaforme principali (es. ["Netflix", "Prime Video"])
  val availableLanguages: String = ""          // Lingue audio e sottotitoli disponibili
) {
  /**
   * Indica se questo titolo ha episodi tracciabili (Serie TV o Anime).
   */
  val isEpisodic: Boolean
    get() = mediaType == MediaType.TV_SERIES || mediaType == MediaType.ANIME || episodes.isNotEmpty() || totalEpisodes > 1

  /**
   * Restituisce l'elenco degli attori/cast (se `cast` non è specificato, lo estrae da `director`).
   */
  val resolvedCast: List<String>
    get() {
      if (cast.isNotEmpty()) return cast
      val parts = director.split("•")
      return if (parts.size > 1) {
        parts.last().split(",").map { it.trim() }.filter { it.isNotBlank() }
      } else {
        listOf(director)
      }
    }

  /**
   * Restituisce il nome del regista o studio principale.
   */
  val resolvedDirectorOrStudio: String
    get() = director.split("•").firstOrNull()?.trim().orEmpty().ifBlank { "Produzione Ufficiale" }

  /**
   * Restituisce le lingue disponibili (Audio e Sottotitoli) in base al tipo di programma.
   */
  val resolvedLanguages: String
    get() = availableLanguages.ifBlank {
      when (mediaType) {
        MediaType.ANIME -> "🇯🇵 Giapponese (Originale), 🇮🇹 Italiano, 🇬🇧 Inglese • Sottotitoli: Italiano, Inglese"
        MediaType.TV_SERIES -> "🇮🇹 Italiano (Doppiato), 🇬🇧 Inglese (Originale), 🇪🇸 Spagnolo • Sottotitoli: ITA, ENG"
        MediaType.MOVIE -> "🇮🇹 Italiano (Dolby 5.1), 🇬🇧 Lingua Originale • Sottotitoli: Italiano, Inglese"
      }
    }

  /**
   * Genera i pulsanti di redirect diretto per guardare il programma sulle piattaforme di streaming!
   */
  val watchLinks: List<StreamingProvider>
    get() {
      val encodedTitle = try {
        URLEncoder.encode(title, "UTF-8")
      } catch (_: Exception) {
        title.replace(" ", "+")
      }
      val defaultPlatforms = if (streamingPlatforms.isNotEmpty()) {
        streamingPlatforms
      } else {
        when (mediaType) {
          MediaType.ANIME -> listOf("Crunchyroll", "Netflix", "Prime Video")
          MediaType.TV_SERIES -> listOf("Netflix", "Prime Video", "Disney+", "Now / Sky")
          MediaType.MOVIE -> listOf("Netflix", "Prime Video", "Disney+", "Apple TV+")
        }
      }

      val links = defaultPlatforms.map { platformName ->
        when {
          platformName.contains("Netflix", ignoreCase = true) -> StreamingProvider(
            name = "Netflix",
            url = "https://www.netflix.com/search?q=$encodedTitle",
            badgeColorHex = 0xFFE50914
          )
          platformName.contains("Prime", ignoreCase = true) -> StreamingProvider(
            name = "Prime Video",
            url = "https://www.primevideo.com/search/ref=atv_nb_sr?phrase=$encodedTitle",
            badgeColorHex = 0xFF00A8E1
          )
          platformName.contains("Disney", ignoreCase = true) -> StreamingProvider(
            name = "Disney+",
            url = "https://www.disneyplus.com/it-it/search?q=$encodedTitle",
            badgeColorHex = 0xFF113CCF
          )
          platformName.contains("Crunchyroll", ignoreCase = true) -> StreamingProvider(
            name = "Crunchyroll",
            url = "https://www.crunchyroll.com/it/search?q=$encodedTitle",
            badgeColorHex = 0xFFF47521
          )
          platformName.contains("Apple", ignoreCase = true) -> StreamingProvider(
            name = "Apple TV+",
            url = "https://tv.apple.com/it/search?term=$encodedTitle",
            badgeColorHex = 0xFF333333
          )
          else -> StreamingProvider(
            name = platformName,
            url = "https://www.justwatch.com/it/cerca?q=$encodedTitle",
            badgeColorHex = 0xFF2ECC71
          )
        }
      }

      // Aggiungiamo sempre JustWatch Italia per vedere istantaneamente tutte le offerte streaming
      return links + StreamingProvider(
        name = "JustWatch 🇮🇹",
        url = "https://www.justwatch.com/it/cerca?q=$encodedTitle",
        badgeColorHex = 0xFFD4A500
      )
    }

  /**
   * Restituisce la durata o il formato in modo leggibile:
   * - Per i Film: "2h 28m"
   * - Per le Serie TV: "5 stagioni • 62 ep."
   * - Per gli Anime: "64 episodi • 24m/ep"
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
          val epCount = maxOf(episodes.size, totalEpisodes)
          val seasonLabel = if (totalSeasons > 1) "$totalSeasons stagioni • " else "1 stagione • "
          "${seasonLabel}$epCount ep."
        }
        MediaType.ANIME -> {
          val epCount = maxOf(episodes.size, totalEpisodes)
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

  val imdbPageUrl: String
    get() = externalPageUrl
}

/**
 * Categorie di filtro rapido per scegliere il genere della serata.
 * Include anche l'ID numerico ufficiale del genere su MyAnimeList (Jikan API v4)
 * così i filtri per gli Anime sono precisi al 100%!
 */
enum class MovieGenreFilter(
  val label: String,
  val apiGenre: String?,
  val malGenreId: Int?
) {
  ALL("Tutti ✨", null, null),
  SCI_FI("Sci-Fi 🚀", "Sci-Fi", 24),
  ACTION("Azione 💥", "Action", 1),
  THRILLER("Thriller 🕵️", "Thriller", 41),
  COMEDY("Commedia 😂", "Comedy", 4),
  DRAMA("Dramma 🎭", "Drama", 8),
  ANIMATION("Fantasy / Animazione 🐉", "Animation", 10),
  HORROR("Horror / Dark 👻", "Horror", 14)
}
