# 🍿 MovieNight v2.0 (Film, Serie TV & Anime MyAnimeList)

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android%20(minSdk%2024)-3DDC84?logo=android&logoColor=white)
![IMDb](https://img.shields.io/badge/Data-Free%20IMDb%20API-F5C518?logo=imdb&logoColor=black)
![MyAnimeList](https://img.shields.io/badge/Anime-MyAnimeList%20(Jikan%20v4)-2E51A2)
[![CI](https://github.com/Symon180/MovieNight/actions/workflows/ci.yml/badge.svg)](https://github.com/Symon180/MovieNight/actions/workflows/ci.yml)

**MovieNight** è un'applicazione Android nativa sviluppata in **Kotlin** e **Jetpack Compose** per scegliere in pochi secondi cosa guardare la sera (**Film 🎬, Serie TV 📺 e Anime ⛩️**), organizzare la propria lista **Da Vedere**, tenere il diario dei titoli **Già Visti** e consultare/votare i **singoli episodi** con collegamento diretto a **IMDb** e **MyAnimeList (MAL)**.

---

## ✨ Caratteristiche Principali (v2.0)

- **🎴 Sezione Scopri (Swipe stile Tinder per Film, Serie TV & Anime):**
  - Selettore rapido in alto tra **Film 🎬**, **Serie TV 📺** e **Anime ⛩️**.
  - Trascina la scheda a **destra** (o premi ❤️) per salvare il titolo nella sezione **Da Vedere**.
  - Trascina la scheda a **sinistra** (o premi ✖️) per scartarlo, oppure premi **Già visto ✅** per aggiungerlo direttamente al tuo diario.
- **📌 Sezione "Da Vedere" (Watchlist Persistente):**
  - Raccoglie tutti i Film, Serie TV e Anime che ti ispirano, filtrabili per categoria.
  - Pulsante **🎲 "Sorteggia tra i Da Vedere!"** per estrarre a sorte cosa guardare stasera.
- **✅ Sezione "Già Visti & Episodi" (Diario Personale):**
  - Statistiche aggiornate su **Titoli Visti**, **Episodi Visti** e **Tuo Voto Medio ⭐**.
  - Assegna il tuo **voto personale da 1 a 10 stelle** a qualsiasi Film, Serie TV o Anime.
- **📊 Ratings degli Episodi & Tracker Avanzamento:**
  - Per ogni **Serie TV** e **Anime**, apri il pannello **Episodi & Ratings** per vedere:
    - Il **voto medio ufficiale della community** (IMDb / MyAnimeList) per ogni singolo episodio con badge colorato stile heatmap.
    - La spunta **Visto ☑️** per tenere traccia dell'episodio a cui sei arrivato (con barra di progresso).
    - Il **tuo voto personale (1-10 ⭐)** per ogni singolo episodio!
- **⛩️ Integrazione MyAnimeList (MAL):**
  - Catalogo Top Anime, ricerca live ed episodi alimentati dalla **Jikan REST API v4** (`api.jikan.moe/v4`, gratuita e senza registrazione), con badge ufficiale **MAL** e apertura diretta della scheda su `myanimelist.net`.
  - **Import Profilo MyAnimeList:** inserisci il tuo username MAL per sincronizzare automaticamente i tuoi anime (*Plan to Watch* ➡️ *Da Vedere*, *Completed/Watching* ➡️ *Già Visti* con voti ed episodi).
- **💾 Salvataggio Persistente Locale:**
  - Tutte le tue liste, i voti personali e i progressi degli episodi vengono salvati automaticamente nella memoria del telefono (`SharedPreferencesLibraryStorage`).

---

## 🛠️ Stack Tecnologico e Architettura

- **Linguaggio:** Kotlin 2.3 (JVM Toolchain 17)
- **Interfaccia Utente:** Jetpack Compose + Material Design 3 (tema scuro *Cinema, IMDb Gold & MyAnimeList Blue*)
- **Architettura:** MVVM (`Model - View - ViewModel`) con `StateFlow` reattivo e persistenza JSON locale
- **Fonti Dati:** Free IMDb API (`api.imdbapi.dev`, `v3.sg.media-imdb.com`) + MyAnimeList Jikan API v4 (`api.jikan.moe/v4`) + Catalogo curato offline
- **Caricamento Immagini:** Coil (`io.coil-kt:coil-compose`)
- **Test:** JUnit 4 + `kotlinx-coroutines-test` (9 test unitari)

```text
app/src/main/kotlin/com/symon/movienight/
├── MainActivity.kt                  # Entry point dell'applicazione Android
├── model/
│   └── Movie.kt                     # Modelli Movie, MediaType, Episode, UserMediaProgress
├── data/
│   ├── MovieRepository.kt           # Client IMDb + MyAnimeList (Jikan v4) + Catalogo curato
│   └── UserLibraryStorage.kt        # Persistenza JSON su SharedPreferences
├── viewmodel/
│   └── MovieNightViewModel.kt       # Logica MVVM (Swipe, Watchlist, Già Visti, Episodi, Sync MAL)
└── ui/
    ├── MovieNightScreen.kt          # Navigazione a 3 tab, Swipe, Watchlist, Watched, EpisodeTrackerSheet
    └── theme/                       # Tema scuro Cinema con accenti IMDb Gold e MAL Blue
```

---

## 🚀 Come Compilare ed Eseguire

```bash
# Eseguire la suite di test unitari (9 test)
./gradlew testDebugUnitTest

# Compilare il pacchetto installabile APK (in app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug
```

---

## 👤 Autore & Licenza

* **Autore:** [Simone Mariotti](https://github.com/Symon180)
* **Licenza:** Rilasciato sotto licenza [MIT](LICENSE).
