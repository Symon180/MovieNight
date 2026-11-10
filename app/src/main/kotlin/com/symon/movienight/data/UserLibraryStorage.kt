package com.symon.movienight.data

import android.content.Context
import com.symon.movienight.model.Episode
import com.symon.movienight.model.MediaSource
import com.symon.movienight.model.MediaType
import com.symon.movienight.model.Movie
import com.symon.movienight.model.UserMediaProgress
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fotografia completa dei dati salvati dall'utente sul telefono:
 * - `watchlist`: i titoli "Da Vedere" (salvati con Swipe a destra o importati da MAL "Plan to Watch")
 * - `watched`: i titoli "Già Visti" (o in corso di visione)
 * - `progress`: mappa `mediaId -> UserMediaProgress` con il voto personale al titolo,
 *   gli episodi spuntati come visti e i voti dati ai singoli episodi
 * - `malUsername`: l'eventuale username MyAnimeList collegato dall'utente
 */
data class LibrarySnapshot(
  val watchlist: List<Movie> = emptyList(),
  val watched: List<Movie> = emptyList(),
  val progress: Map<String, UserMediaProgress> = emptyMap(),
  val malUsername: String = ""
)

/**
 * Interfaccia per il salvataggio persistente della libreria personale.
 * Usare un'interfaccia ci permette di avere:
 * - `SharedPreferencesLibraryStorage` sul telefono Android reale
 * - `InMemoryLibraryStorage` nei test unitari veloci su PC
 */
interface LibraryStorage {
  fun load(): LibrarySnapshot
  fun save(snapshot: LibrarySnapshot)
}

/**
 * Implementazione in memoria (usata di default nei test o quando non c'è Context Android).
 */
class InMemoryLibraryStorage(
  initialSnapshot: LibrarySnapshot = LibrarySnapshot()
) : LibraryStorage {
  private var current: LibrarySnapshot = initialSnapshot

  override fun load(): LibrarySnapshot = current

  override fun save(snapshot: LibrarySnapshot) {
    current = snapshot
  }
}

/**
 * Salva in modo permanente su disco nel telefono Android tramite `SharedPreferences` in formato JSON.
 * Così quando chiudi e riapri MovieNight ritrovi tutti i tuoi film/serie/anime "Da vedere",
 * "Già visti", i tuoi voti e gli episodi spuntati!
 */
class SharedPreferencesLibraryStorage(context: Context) : LibraryStorage {

  private val prefs = context.applicationContext.getSharedPreferences(
    "movienight_user_library_v2",
    Context.MODE_PRIVATE
  )

  override fun load(): LibrarySnapshot {
    val rawJson = prefs.getString(KEY_SNAPSHOT_JSON, null) ?: return LibrarySnapshot()
    return try {
      val root = JSONObject(rawJson)
      val watchlist = parseMoviesArray(root.optJSONArray("watchlist"))
      val watched = parseMoviesArray(root.optJSONArray("watched"))
      val progress = parseProgressMap(root.optJSONObject("progress"))
      val malUser = root.optString("malUsername", "")
      LibrarySnapshot(
        watchlist = watchlist,
        watched = watched,
        progress = progress,
        malUsername = malUser
      )
    } catch (_: Exception) {
      LibrarySnapshot()
    }
  }

  override fun save(snapshot: LibrarySnapshot) {
    try {
      val root = JSONObject().apply {
        put("watchlist", serializeMoviesArray(snapshot.watchlist))
        put("watched", serializeMoviesArray(snapshot.watched))
        put("progress", serializeProgressMap(snapshot.progress))
        put("malUsername", snapshot.malUsername)
      }
      prefs.edit().putString(KEY_SNAPSHOT_JSON, root.toString()).apply()
    } catch (_: Exception) {
      // Ignora errori di scrittura disco in ambienti non standard
    }
  }

  private fun serializeMoviesArray(movies: List<Movie>): JSONArray {
    val arr = JSONArray()
    for (m in movies) {
      val obj = JSONObject().apply {
        put("id", m.id)
        put("title", m.title)
        put("year", m.year)
        put("imdbRating", m.imdbRating)
        put("runtimeMinutes", m.runtimeMinutes)
        put("genres", JSONArray(m.genres))
        put("plot", m.plot)
        put("director", m.director)
        put("posterUrl", m.posterUrl)
        put("moodTag", m.moodTag)
        put("mediaType", m.mediaType.name)
        put("source", m.source.name)
        if (m.malId != null) put("malId", m.malId)
        put("totalSeasons", m.totalSeasons)
        put("totalEpisodes", m.totalEpisodes)
        val epArray = JSONArray()
        for (ep in m.episodes) {
          epArray.put(
            JSONObject().apply {
              put("id", ep.id)
              put("seasonNumber", ep.seasonNumber)
              put("episodeNumber", ep.episodeNumber)
              put("title", ep.title)
              put("communityRating", ep.communityRating)
              put("airDate", ep.airDate)
            }
          )
        }
        put("episodes", epArray)
      }
      arr.put(obj)
    }
    return arr
  }

  private fun parseMoviesArray(arr: JSONArray?): List<Movie> {
    if (arr == null) return emptyList()
    val result = mutableListOf<Movie>()
    for (i in 0 until arr.length()) {
      val obj = arr.optJSONObject(i) ?: continue
      val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
      val title = obj.optString("title").takeIf { it.isNotBlank() } ?: continue
      val genresArr = obj.optJSONArray("genres")
      val genres = mutableListOf<String>()
      if (genresArr != null) {
        for (g in 0 until genresArr.length()) {
          val gStr = genresArr.optString(g)
          if (gStr.isNotBlank()) genres.add(gStr)
        }
      }
      val epArr = obj.optJSONArray("episodes")
      val episodes = mutableListOf<Episode>()
      if (epArr != null) {
        for (e in 0 until epArr.length()) {
          val epObj = epArr.optJSONObject(e) ?: continue
          episodes.add(
            Episode(
              id = epObj.optString("id", "${id}_e${e + 1}"),
              seasonNumber = epObj.optInt("seasonNumber", 1),
              episodeNumber = epObj.optInt("episodeNumber", e + 1),
              title = epObj.optString("title", "Episodio ${e + 1}"),
              communityRating = epObj.optDouble("communityRating", 8.0),
              airDate = epObj.optString("airDate", "")
            )
          )
        }
      }
      val mediaTypeName = obj.optString("mediaType", MediaType.MOVIE.name)
      val sourceName = obj.optString("source", MediaSource.IMDB.name)
      val malIdVal = if (obj.has("malId")) obj.optInt("malId") else null

      result.add(
        Movie(
          id = id,
          title = title,
          year = obj.optInt("year", 2020),
          imdbRating = obj.optDouble("imdbRating", 8.0),
          runtimeMinutes = obj.optInt("runtimeMinutes", 120),
          genres = genres,
          plot = obj.optString("plot", ""),
          director = obj.optString("director", ""),
          posterUrl = obj.optString("posterUrl", ""),
          moodTag = obj.optString("moodTag", ""),
          mediaType = runCatching { MediaType.valueOf(mediaTypeName) }.getOrDefault(MediaType.MOVIE),
          source = runCatching { MediaSource.valueOf(sourceName) }.getOrDefault(MediaSource.IMDB),
          malId = malIdVal,
          totalSeasons = obj.optInt("totalSeasons", 1),
          totalEpisodes = obj.optInt("totalEpisodes", 1),
          episodes = episodes
        )
      )
    }
    return result
  }

  private fun serializeProgressMap(map: Map<String, UserMediaProgress>): JSONObject {
    val root = JSONObject()
    for ((mediaId, prog) in map) {
      val obj = JSONObject().apply {
        if (prog.userRating != null) put("userRating", prog.userRating)
        put("watchedEpisodeIds", JSONArray(prog.watchedEpisodeIds.toList()))
        val epRatingsObj = JSONObject()
        for ((epId, rating) in prog.episodeRatings) {
          epRatingsObj.put(epId, rating)
        }
        put("episodeRatings", epRatingsObj)
      }
      root.put(mediaId, obj)
    }
    return root
  }

  private fun parseProgressMap(root: JSONObject?): Map<String, UserMediaProgress> {
    if (root == null) return emptyMap()
    val result = mutableMapOf<String, UserMediaProgress>()
    val keys = root.keys()
    while (keys.hasNext()) {
      val mediaId = keys.next()
      val obj = root.optJSONObject(mediaId) ?: continue
      val userRating = if (obj.has("userRating")) obj.optInt("userRating") else null
      val watchedSet = mutableSetOf<String>()
      val watchedArr = obj.optJSONArray("watchedEpisodeIds")
      if (watchedArr != null) {
        for (i in 0 until watchedArr.length()) {
          val epId = watchedArr.optString(i)
          if (epId.isNotBlank()) watchedSet.add(epId)
        }
      }
      val epRatingsMap = mutableMapOf<String, Int>()
      val epRatingsObj = obj.optJSONObject("episodeRatings")
      if (epRatingsObj != null) {
        val epKeys = epRatingsObj.keys()
        while (epKeys.hasNext()) {
          val epId = epKeys.next()
          epRatingsMap[epId] = epRatingsObj.optInt(epId, 8)
        }
      }
      result[mediaId] = UserMediaProgress(
        mediaId = mediaId,
        userRating = userRating,
        watchedEpisodeIds = watchedSet,
        episodeRatings = epRatingsMap
      )
    }
    return result
  }

  companion object {
    private const val KEY_SNAPSHOT_JSON = "snapshot_json"
  }
}
