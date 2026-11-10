package com.symon.movienight.model

import java.net.URLEncoder

/**
 * Indica il tipo di contenuto multimediale: Film, Serie TV o Anime.
 * NOTA: I Film Anime risiedono esclusivamente nella sezione `ANIME`
 * (filtrabili con `AnimeFormatFilter.MOVIE`).
 */
enum class MediaType(val label: String, val badgeLabel: String) {
  MOVIE("Film 🎬", "FILM"),
  TV_SERIES("Serie TV 📺", "SERIE TV"),
  ANIME("Anime ⛩️", "ANIME")
}

/**
 * Sotto-filtro esclusivo per la sezione Anime:
 * permette di dividere gli Anime in **Serie Anime** e **Film Anime**!
 */
enum class AnimeFormatFilter(val label: String, val jikanType: String?) {
  ALL("Tutti ⛩️", null),
  SERIES("Serie Anime 📺", "tv"),
  MOVIE("Film Anime 🎬", "movie")
}

/**
 * Filtro per Epoca / Decennio di uscita:
 * permette di scoprire sia le ultime uscite sia i grandi classici degli anni '90, '80, '70 e oltre!
 */
enum class EraFilter(
  val label: String,
  val minYear: Int?,
  val maxYear: Int?
) {
  ALL("Tutte le epoche 🕰️", null, null),
  YEARS_2020S("2020 - Oggi", 2020, 2035),
  YEARS_2010S("Anni 2010", 2010, 2019),
  YEARS_2000S("Anni 2000", 2000, 2009),
  YEARS_90S("Anni '90", 1990, 1999),
  YEARS_80S("Anni '80", 1980, 1989),
  CLASSICS("Classici (< '80)", 1900, 1979);

  fun matches(year: Int): Boolean {
    if (minYear != null && year < minYear) return false
    if (maxYear != null && year > maxYear) return false
    return true
  }
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
 * Rappresenta un Attore, Doppiatore o Personaggio con Nome, Ruolo e Foto!
 */
data class CastMember(
  val name: String,
  val role: String = "",
  val photoUrl: String = ""
) {
  val initials: String
    get() = name
      .split(" ")
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "🎭" }
}

/**
 * Rappresenta un singolo episodio di una Serie TV o di una Serie Anime.
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
 * Conserva i progressi e i voti personali dell'utente per un titolo (Film, Serie TV o Anime).
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
  val totalEpisodes: Int = 1,                  // Numero totale reale di episodi (1 = Film / Film Anime, >1 = Serie)
  val episodes: List<Episode> = emptyList(),   // Lista degli episodi con i voti della community
  val cast: List<String> = emptyList(),        // Attori principali o personaggi/doppiatori
  val castMembers: List<CastMember> = emptyList(), // Attori/personaggi completi di foto
  val streamingPlatforms: List<String> = emptyList(), // Piattaforme principali (es. ["Netflix", "Prime Video"])
  val availableLanguages: String = ""          // Lingue audio e sottotitoli disponibili
) {
  /**
   * Indica se è un Film Anime (nella sezione Anime ma lungometraggio singolo).
   */
  val isAnimeMovie: Boolean
    get() = mediaType == MediaType.ANIME && totalEpisodes <= 1 && episodes.size <= 1

  /**
   * Indica se è una Serie Anime (nella sezione Anime con più episodi).
   */
  val isAnimeSeries: Boolean
    get() = mediaType == MediaType.ANIME && !isAnimeMovie

  /**
   * Indica se questo titolo ha episodi tracciabili (Serie TV o Serie Anime, NON i Film singoli).
   */
  val isEpisodic: Boolean
    get() = when (mediaType) {
      MediaType.MOVIE -> false
      MediaType.TV_SERIES -> true
      MediaType.ANIME -> totalEpisodes > 1 || episodes.size > 1
    }

  /**
   * Restituisce l'elenco dei nomi degli attori/cast.
   */
  val resolvedCast: List<String>
    get() {
      if (cast.isNotEmpty()) return cast
      if (castMembers.isNotEmpty()) return castMembers.map { it.name }
      val parts = director.split("•")
      return if (parts.size > 1) {
        parts.last().split(",").map { it.trim() }.filter { it.isNotBlank() }
      } else {
        listOf(director)
      }
    }

  /**
   * Restituisce l'elenco completo di `CastMember` con Nome, Ruolo e URL Foto!
   */
  val resolvedCastMembers: List<CastMember>
    get() {
      if (castMembers.isNotEmpty()) return castMembers
      return resolvedCast.map { rawEntry ->
        ActorPhotoDirectory.resolveCastMember(rawEntry, posterUrl)
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

      return links + StreamingProvider(
        name = "JustWatch 🇮🇹",
        url = "https://www.justwatch.com/it/cerca?q=$encodedTitle",
        badgeColorHex = 0xFFD4A500
      )
    }

  /**
   * Restituisce la durata o il formato in modo leggibile:
   * - Per i Film: "2h 28m"
   * - Per i Film Anime: "Film Anime • 2h 05m"
   * - Per le Serie TV: "5 stagioni • 62 ep."
   * - Per le Serie Anime: "64 episodi • 24m/ep"
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
          if (epCount > 1) {
            "$epCount episodi • ${runtimeMinutes}m/ep"
          } else {
            val hours = runtimeMinutes / 60
            val mins = runtimeMinutes % 60
            val dur = if (hours > 0) "${hours}h ${mins}m" else "${runtimeMinutes}m"
            "Film Anime • $dur"
          }
        }
      }
    }

  /**
   * Etichetta del badge per distinguere anche "FILM ANIME" e "SERIE ANIME".
   */
  val detailedBadgeLabel: String
    get() = when {
      isAnimeMovie -> "FILM ANIME 🎬"
      isAnimeSeries -> "SERIE ANIME 📺"
      else -> mediaType.badgeLabel
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
 * Include anche l'ID numerico ufficiale del genere su MyAnimeList (Jikan API v4).
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
  ANIMATION("Fantasy / Avventura 🐉", "Animation", 10),
  HORROR("Horror / Dark 👻", "Horror", 14)
}

/**
 * Directory fotografica degli attori e personaggi principali (con fallback automatico),
 * usata insieme al caricamento live da Jikan Characters API e Wikipedia/IMDb API.
 */
object ActorPhotoDirectory {
  private val knownPhotos: Map<String, String> = mapOf(
    // Attori Cinema & Serie TV (Foto ufficiali Wikimedia / IMDb CDN)
    "leonardo dicaprio" to "https://upload.wikimedia.org/wikipedia/commons/thumb/4/46/Leonardo_Dicaprio_Cannes_2019.jpg/440px-Leonardo_Dicaprio_Cannes_2019.jpg",
    "cillian murphy" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Cillian_Murphy-2014.jpg/440px-Cillian_Murphy-2014.jpg",
    "joseph gordon-levitt" to "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Joseph_Gordon-Levitt_TechCrunch_Disrupt_NY_2016_%28cropped%29.jpg/440px-Joseph_Gordon-Levitt_TechCrunch_Disrupt_NY_2016_%28cropped%29.jpg",
    "tom hardy" to "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Tom_Hardy_by_Gage_Skidmore.jpg/440px-Tom_Hardy_by_Gage_Skidmore.jpg",
    "marion cotillard" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Marion_Cotillard_Cannes_2018.jpg/440px-Marion_Cotillard_Cannes_2018.jpg",
    "matthew mcconaughey" to "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Matthew_McConaughey_2019_%2848648344772%29.jpg/440px-Matthew_McConaughey_2019_%2848648344772%29.jpg",
    "anne hathaway" to "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bd/Anne_Hathaway_in_2017.png/440px-Anne_Hathaway_in_2017.png",
    "jessica chastain" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/Jessica_Chastain_Cannes_2017.jpg/440px-Jessica_Chastain_Cannes_2017.jpg",
    "michael caine" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/34/Michael_Caine_-_Viennale_2012_g_%28cropped%29.jpg/440px-Michael_Caine_-_Viennale_2012_g_%28cropped%29.jpg",
    "matt damon" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Damon_cropped.jpg/440px-Damon_cropped.jpg",
    "christian bale" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Christian_Bale-7837.jpg/440px-Christian_Bale-7837.jpg",
    "heath ledger" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Heath_Ledger_%282%29.jpg/440px-Heath_Ledger_%282%29.jpg",
    "gary oldman" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/89/Gary_Oldman_Cannes_2018.jpg/440px-Gary_Oldman_Cannes_2018.jpg",
    "morgan freeman" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e4/Morgan_Freeman_Deauville_2018.jpg/440px-Morgan_Freeman_Deauville_2018.jpg",
    "john travolta" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a8/John_Travolta_Cannes_2018.jpg/440px-John_Travolta_Cannes_2018.jpg",
    "samuel l. jackson" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Samuel_L._Jackson_2019_by_Glenn_Francis.jpg/440px-Samuel_L._Jackson_2019_by_Glenn_Francis.jpg",
    "uma thurman" to "https://upload.wikimedia.org/wikipedia/commons/thumb/4/42/Uma_Thurman_Cannes_2017_%28cropped%29.jpg/440px-Uma_Thurman_Cannes_2017_%28cropped%29.jpg",
    "bruce willis" to "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c4/Bruce_Willis_by_Gage_Skidmore_3.jpg/440px-Bruce_Willis_by_Gage_Skidmore_3.jpg",
    "keanu reeves" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/33/Reuni%C3%A3o_com_o_ator_norte-americano_Keanu_Reeves_%2846806576944%29_%28cropped%29.jpg/440px-Reuni%C3%A3o_com_o_ator_norte-americano_Keanu_Reeves_%2846806576944%29_%28cropped%29.jpg",
    "laurence fishburne" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/85/Laurence_Fishburne_2009_-_cropped.jpg/440px-Laurence_Fishburne_2009_-_cropped.jpg",
    "brad pitt" to "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/Brad_Pitt_2019_by_Glenn_Francis.jpg/440px-Brad_Pitt_2019_by_Glenn_Francis.jpg",
    "edward norton" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/35/Ed_Norton_Shankbone_Metropolitan_Opera_2009.jpg/440px-Ed_Norton_Shankbone_Metropolitan_Opera_2009.jpg",
    "helena bonham carter" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6e/Helena_Bonham_Carter_2011_AA.jpg/440px-Helena_Bonham_Carter_2011_AA.jpg",
    "tim robbins" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/61/Tim_Robbins_Cannes_2012.jpg/440px-Tim_Robbins_Cannes_2012.jpg",
    "ralph fiennes" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Ralph_Fiennes_from_%22The_White_Crow%22_at_Opening_Ceremony_of_ the_Tokyo_International_Film_Festival_2018_%2831747095048%29.jpg/440px-Ralph_Fiennes_from_%22The_White_Crow%22_at_Opening_Ceremony_of_the_Tokyo_International_Film_Festival_2018_%2831747095048%29.jpg",
    "willem dafoe" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/14/Willem_Dafoe_Cannes_2019.jpg/440px-Willem_Dafoe_Cannes_2019.jpg",
    "adrien brody" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Adrien_Brody_Cannes_2019.jpg/440px-Adrien_Brody_Cannes_2019.jpg",
    "bill murray" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/Bill_Murray_Cannes_2019.jpg/440px-Bill_Murray_Cannes_2019.jpg",
    "ryan gosling" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f6/Ryan_Gosling_in_2018.jpg/440px-Ryan_Gosling_in_2018.jpg",
    "harrison ford" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/34/Harrison_Ford_by_Gage_Skidmore_3.jpg/440px-Harrison_Ford_by_Gage_Skidmore_3.jpg",
    "ana de armas" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/33/Ana_de_Armas_in_2023.jpg/440px-Ana_de_Armas_in_2023.jpg",
    "miles teller" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f8/Miles_Teller_2014.jpg/440px-Miles_Teller_2014.jpg",
    "j.k. simmons" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/J._K._Simmons_2016.jpg/440px-J._K._Simmons_2016.jpg",
    "russell crowe" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Russell_Crowe_%2833994020424%29.jpg/440px-Russell_Crowe_%2833994020424%29.jpg",
    "joaquin phoenix" to "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d1/Joaquin_Phoenix_in_2018.jpg/440px-Joaquin_Phoenix_in_2018.jpg",
    "emily blunt" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Emily_Blunt_SAG_Awards_2019.png/440px-Emily_Blunt_SAG_Awards_2019.png",
    "robert downey jr." to "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Robert_Downey_Jr_2014_Comic_Con_%28cropped%29.jpg/440px-Robert_Downey_Jr_2014_Comic_Con_%28cropped%29.jpg",
    "florence pugh" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Florence_Pugh_in_2020.jpg/440px-Florence_Pugh_in_2020.jpg",
    "timothée chalamet" to "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Timoth%C3%A9e_Chalamet_2017_Berlinale.jpg/440px-Timoth%C3%A9e_Chalamet_2017_Berlinale.jpg",
    "zendaya" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/28/Zendaya_-_2019_by_Glenn_Francis.jpg/440px-Zendaya_-_2019_by_Glenn_Francis.jpg",
    "rebecca ferguson" to "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Rebecca_Ferguson_in_2018.jpg/440px-Rebecca_Ferguson_in_2018.jpg",
    "javier bardem" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Javier_Bardem_Cannes_2018.jpg/440px-Javier_Bardem_Cannes_2018.jpg",
    "austin butler" to "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d0/Austin_Butler_Cannes_2022.jpg/440px-Austin_Butler_Cannes_2022.jpg",
    "jonah hill" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0e/Jonah_Hill-4939_%28cropped%29.jpg/440px-Jonah_Hill-4939_%28cropped%29.jpg",
    "michael cera" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Michael_Cera_2012_%28cropped%29.jpg/440px-Michael_Cera_2012_%28cropped%29.jpg",
    "emma stone" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Emma_Stone_at_Maniac_UK_premiere_%28cropped%29.jpg/440px-Emma_Stone_at_Maniac_UK_premiere_%28cropped%29.jpg",
    "seth rogen" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/61/Seth_Rogen_at_SXSW_2019.jpg/440px-Seth_Rogen_at_SXSW_2019.jpg",
    "bill hader" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Bill_Hader_2018.jpg/440px-Bill_Hader_2018.jpg",
    "bradley cooper" to "https://upload.wikimedia.org/wikipedia/commons/thumb/b/Light/Bradley_Cooper_avp_2014.jpg/440px-Bradley_Cooper_avp_2014.jpg",
    "zach galifianakis" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Zach_Galifianakis_2012.jpg/440px-Zach_Galifianakis_2012.jpg",
    "marlon brando" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Marlon_Brando_by_Edward_Cronenweth%2C_1955.jpg/440px-Marlon_Brando_by_Edward_Cronenweth%2C_1955.jpg",
    "al pacino" to "https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/Al_Pacino.jpg/440px-Al_Pacino.jpg",
    "robert de niro" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/Robert_De_Niro_Cannes_2016.jpg/440px-Robert_De_Niro_Cannes_2016.jpg",
    "clint eastwood" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7e/Clint_Eastwood_at_2010_New_York_Film_Festival.jpg/440px-Clint_Eastwood_at_2010_New_York_Film_Festival.jpg",
    "michael j. fox" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/20/Michael_J._Fox_2020.jpg/440px-Michael_J._Fox_2020.jpg",
    "christopher lloyd" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7f/Christopher_Lloyd_2015.jpg/440px-Christopher_Lloyd_2015.jpg",
    "sigourney weaver" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/Sigourney_Weaver_by_Gage_Skidmore_4.jpg/440px-Sigourney_Weaver_by_Gage_Skidmore_4.jpg",
    "jack nicholson" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Jack_Nicholson_2002.jpg/440px-Jack_Nicholson_2002.jpg",
    "anthony hopkins" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Anthony_Hopkins-Tuscan_Sun_Festival.jpg/440px-Anthony_Hopkins-Tuscan_Sun_Festival.jpg",
    "jodie foster" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0c/Jodie_Foster_Cannes_2016.jpg/440px-Jodie_Foster_Cannes_2016.jpg",
    "ray liotta" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f9/Ray_Liotta_Deauville_2014.jpg/440px-Ray_Liotta_Deauville_2014.jpg",
    "joe pesci" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/37/JoePesci-2009.jpg/440px-JoePesci-2009.jpg",
    "henry fonda" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0c/Henry_Fonda_in_Warlock.jpg/440px-Henry_Fonda_in_Warlock.jpg",
    // Serie TV
    "bryan cranston" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bryan_Cranston_at_the_2018_Berlin_Film_Festival_2.jpg/440px-Bryan_Cranston_at_the_2018_Berlin_Film_Festival_2.jpg",
    "aaron paul" to "https://upload.wikimedia.org/wikipedia/commons/thumb/9/93/Aaron_Paul_by_Gage_Skidmore_3.jpg/440px-Aaron_Paul_by_Gage_Skidmore_3.jpg",
    "bob odenkirk" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/Bob_Odenkirk_by_Gage_Skidmore_2.jpg/440px-Bob_Odenkirk_by_Gage_Skidmore_2.jpg",
    "giancarlo esposito" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f9/Giancarlo_Esposito_by_Gage_Skidmore_3.jpg/440px-Giancarlo_Esposito_by_Gage_Skidmore_3.jpg",
    "jared harris" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Jared_Harris_2014_Comic_Con.jpg/440px-Jared_Harris_2014_Comic_Con.jpg",
    "stellan skarsgård" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/22/Stellan_Skarsg%C3%A5rd_Berlinale_2017.jpg/440px-Stellan_Skarsg%C3%A5rd_Berlinale_2017.jpg",
    "hailee steinfeld" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Hailee_Steinfeld_by_Gage_Skidmore.jpg/440px-Hailee_Steinfeld_by_Gage_Skidmore.jpg",
    "ella purnell" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/Ella_Purnell_2016.jpg/440px-Ella_Purnell_2016.jpg",
    "adam scott" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Adam_Scott_by_Gage_Skidmore.jpg/440px-Adam_Scott_by_Gage_Skidmore.jpg",
    "patricia arquette" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Patricia_Arquette_2015.jpg/440px-Patricia_Arquette_2015.jpg",
    "john turturro" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/John_Turturro_Cannes_2015.jpg/440px-John_Turturro_Cannes_2015.jpg",
    "jeremy allen white" to "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Jeremy_Allen_White_2023.jpg/440px-Jeremy_Allen_White_2023.jpg",
    "ayo edebiri" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Ayo_Edebiri_at_SXSW_2023.jpg/440px-Ayo_Edebiri_at_SXSW_2023.jpg",
    "millie bobby brown" to "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/Millie_Bobby_Brown_2022.jpg/440px-Millie_Bobby_Brown_2022.jpg",
    "winona ryder" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1c/Winona_Ryder_2010_TIFF.jpg/440px-Winona_Ryder_2010_TIFF.jpg",
    "david harbour" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/71/David_Harbour_by_Gage_Skidmore_2.jpg/440px-David_Harbour_by_Gage_Skidmore_2.jpg",
    "peter dinklage" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/54/Peter_Dinklage_by_Gage_Skidmore.jpg/440px-Peter_Dinklage_by_Gage_Skidmore.jpg",
    "emilia clarke" to "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d5/Emilia_Clarke_by_Gage_Skidmore_2_%28cropped%29.jpg/440px-Emilia_Clarke_by_Gage_Skidmore_2_%28cropped%29.jpg",
    "kit harington" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/32/Kit_Harington_by_Gage_Skidmore.jpg/440px-Kit_Harington_by_Gage_Skidmore.jpg",
    "steve carell" to "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Steve_Carell_November_2016.jpg/440px-Steve_Carell_November_2016.jpg",
    "john krasinski" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/John_Krasinski_2018.png/440px-John_Krasinski_2018.png",
    "rainn wilson" to "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b7/Rainn_Wilson_by_Gage_Skidmore.jpg/440px-Rainn_Wilson_by_Gage_Skidmore.jpg",
    "james gandolfini" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6c/James_Gandolfini_2011.jpg/440px-James_Gandolfini_2011.jpg",
    "kyle maclachlan" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/50/Kyle_MacLachlan_Cannes_2017.jpg/440px-Kyle_MacLachlan_Cannes_2017.jpg",
    "jennifer aniston" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/JenniferAnistonHWoFFeb2012.jpg/440px-JenniferAnistonHWoFFeb2012.jpg",
    "david duchovny" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/David_Duchovny_by_Gage_Skidmore_2.jpg/440px-David_Duchovny_by_Gage_Skidmore_2.jpg",
    "gillian anderson" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Gillian_Anderson_by_Gage_Skidmore_3.jpg/440px-Gillian_Anderson_by_Gage_Skidmore_3.jpg",
    "peter falk" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f8/Peter_Falk_Columbo_1973.JPG/440px-Peter_Falk_Columbo_1973.JPG",
    // Personaggi Anime (MyAnimeList CDN)
    "edward elric" to "https://cdn.myanimelist.net/images/characters/9/72533.jpg",
    "alphonse elric" to "https://cdn.myanimelist.net/images/characters/5/54265.jpg",
    "roy mustang" to "https://cdn.myanimelist.net/images/characters/11/510223.jpg",
    "frieren" to "https://cdn.myanimelist.net/images/characters/7/525105.jpg",
    "fern" to "https://cdn.myanimelist.net/images/characters/13/525106.jpg",
    "eren yeager" to "https://cdn.myanimelist.net/images/characters/10/216895.jpg",
    "mikasa ackerman" to "https://cdn.myanimelist.net/images/characters/9/215563.jpg",
    "levi ackerman" to "https://cdn.myanimelist.net/images/characters/2/241413.jpg",
    "rintaro okabe" to "https://cdn.myanimelist.net/images/characters/6/122643.jpg",
    "kurisu makise" to "https://cdn.myanimelist.net/images/characters/10/114399.jpg",
    "light yagami" to "https://cdn.myanimelist.net/images/characters/6/63870.jpg",
    "l lawliet" to "https://cdn.myanimelist.net/images/characters/10/249647.jpg",
    "killua zoldyck" to "https://cdn.myanimelist.net/images/characters/2/327920.jpg",
    "gon freecss" to "https://cdn.myanimelist.net/images/characters/11/174517.jpg",
    "tanjiro kamado" to "https://cdn.myanimelist.net/images/characters/6/386735.jpg",
    "nezuko kamado" to "https://cdn.myanimelist.net/images/characters/2/378254.jpg",
    "yuji itadori" to "https://cdn.myanimelist.net/images/characters/6/467646.jpg",
    "satoru gojo" to "https://cdn.myanimelist.net/images/characters/15/422168.jpg",
    "saitama" to "https://cdn.myanimelist.net/images/characters/11/294388.jpg",
    "genos" to "https://cdn.myanimelist.net/images/characters/9/297329.jpg",
    "anya forger" to "https://cdn.myanimelist.net/images/characters/4/457933.jpg",
    "loid forger" to "https://cdn.myanimelist.net/images/characters/2/457747.jpg",
    "yor forger" to "https://cdn.myanimelist.net/images/characters/11/457934.jpg",
    "kaguya shinomiya" to "https://cdn.myanimelist.net/images/characters/3/371540.jpg",
    "miyuki shirogane" to "https://cdn.myanimelist.net/images/characters/13/371541.jpg",
    "spike spiegel" to "https://cdn.myanimelist.net/images/characters/4/50197.jpg",
    "shinji ikari" to "https://cdn.myanimelist.net/images/characters/3/205923.jpg",
    "rei ayanami" to "https://cdn.myanimelist.net/images/characters/11/314932.jpg",
    "asuka langley" to "https://cdn.myanimelist.net/images/characters/12/195221.jpg",
    "son goku" to "https://cdn.myanimelist.net/images/characters/15/72546.jpg",
    "vegeta" to "https://cdn.myanimelist.net/images/characters/14/86185.jpg",
    "chihiro ogino" to "https://cdn.myanimelist.net/images/characters/12/488321.jpg",
    "haku" to "https://cdn.myanimelist.net/images/characters/10/108090.jpg",
    "mitsuha miyamizu" to "https://cdn.myanimelist.net/images/characters/5/316679.jpg",
    "taki tachibana" to "https://cdn.myanimelist.net/images/characters/3/316678.jpg",
    "san" to "https://cdn.myanimelist.net/images/characters/11/255655.jpg",
    "ashitaka" to "https://cdn.myanimelist.net/images/characters/7/255657.jpg",
    "howl" to "https://cdn.myanimelist.net/images/characters/4/79871.jpg",
    "sophie hatter" to "https://cdn.myanimelist.net/images/characters/5/55653.jpg",
    "shoya ishida" to "https://cdn.myanimelist.net/images/characters/15/302314.jpg",
    "shoko nishimiya" to "https://cdn.myanimelist.net/images/characters/5/302315.jpg",
    "shotaro kaneda" to "https://cdn.myanimelist.net/images/characters/10/72227.jpg",
    "tetsuo shima" to "https://cdn.myanimelist.net/images/characters/14/72228.jpg",
    "lupin iii" to "https://cdn.myanimelist.net/images/characters/14/350541.jpg",
    "kenshiro" to "https://cdn.myanimelist.net/images/characters/15/58495.jpg"
  )

  fun resolveCastMember(raw: String, fallbackPosterUrl: String = ""): CastMember {
    val clean = raw.trim()
    val namePart: String
    val rolePart: String
    if (clean.contains("(") && clean.endsWith(")")) {
      namePart = clean.substringBefore("(").trim()
      rolePart = clean.substringAfter("(").removeSuffix(")").trim()
    } else {
      namePart = clean
      rolePart = ""
    }

    val lowerName = namePart.lowercase()
    val lowerRole = rolePart.lowercase()
    val photo = knownPhotos[lowerName]
      ?: knownPhotos[lowerRole]
      ?: knownPhotos.entries.firstOrNull { lowerName.contains(it.key) || it.key.contains(lowerName) }?.value
      ?: fallbackPosterUrl

    return CastMember(
      name = namePart,
      role = rolePart,
      photoUrl = photo
    )
  }
}
