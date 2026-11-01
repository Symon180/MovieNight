package com.symon.movienight.model

/**
 * Rappresenta un film del catalogo IMDb.
 *
 * In Kotlin, una `data class` genera automaticamente metodi utilissimi come:
 * - `equals()` e `hashCode()` per confrontare due film
 * - `toString()` per stampare i dati del film nei log
 * - `copy()` per creare una copia modificata
 */
data class Movie(
  val id: String,              // Codice univoco IMDb, es. "tt1375666"
  val title: String,           // Titolo del film
  val year: Int,               // Anno di uscita
  val imdbRating: Double,      // Voto medio IMDb su 10 (es. 8.8)
  val runtimeMinutes: Int,     // Durata in minuti (es. 148)
  val genres: List<String>,    // Elenco generi (es. ["Sci-Fi", "Action"])
  val plot: String,            // Trama / sinossi in italiano
  val director: String,        // Regista e attori principali
  val posterUrl: String,       // URL della locandina ufficiale IMDb/Amazon Media
  val moodTag: String = ""     // Etichetta rapida per la serata (es. "Mind-bending", "Adrenalina")
) {
  /**
   * Restituisce la durata formattata in ore e minuti (es. "2h 28m").
   */
  val formattedRuntime: String
    get() {
      if (runtimeMinutes <= 0) return "N/D"
      val hours = runtimeMinutes / 60
      val mins = runtimeMinutes % 60
      return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

  /**
   * URL diretto alla pagina ufficiale del film su IMDb.
   */
  val imdbPageUrl: String
    get() = "https://www.imdb.com/title/$id/"
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
  ANIMATION("Animazione 🎨", "Animation"),
  HORROR("Horror 👻", "Horror")
}
