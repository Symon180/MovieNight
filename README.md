# 🍿 MovieNight (PickAMovie)

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android%20(minSdk%2024)-3DDC84?logo=android&logoColor=white)
![IMDb](https://img.shields.io/badge/Data-Free%20IMDb%20API-F5C518?logo=imdb&logoColor=black)

**MovieNight** è un'applicazione Android nativa sviluppata in **Kotlin** e **Jetpack Compose** pensata per risolvere il classico dilemma serale: *"Cosa guardiamo stasera?"*.

Attraverso un'interfaccia a schede scorrevoli (in stile *Tinder*) e un sorteggiatore intelligente, permette di esplorare i film del catalogo **IMDb**, filtrare per genere, salvare i titoli che ispirano di più ed estrarre a sorte il vincitore della serata.

---

## ✨ Caratteristiche Principali

- **🎴 Mazzo di Schede con Gesto Swipe:**
  - Trascina la scheda a **destra** (o premi ❤️) per aggiungere il film alla lista *"Mi ispira"* della serata.
  - Trascina la scheda a **sinistra** (o premi ✖️) per scartarlo e passare al successivo.
- **🎲 Pulsante "Scegli per me a caso!":**
  - Se hai salvato dei film nei preferiti della serata, estrae a sorte il vincitore tra i tuoi candidati; altrimenti pesca direttamente dal mazzo attivo filtrato per genere.
- **🎭 Filtri Rapidi per Genere & Ricerca Live IMDb:**
  - Filtra con un tocco tra *Tutti, Sci-Fi, Azione, Thriller, Commedia, Dramma, Animazione, Horror* oppure cerca in tempo reale qualsiasi film o attore su IMDb.
- **🌐 Architettura Ibrida (Free IMDb API + Catalogo Offline):**
  - Interroga le API pubbliche gratuite di IMDb (`api.imdbapi.dev` e `v3.sg.media-imdb.com/suggestion`) senza bisogno di registrazione o chiavi API, ed è affiancata da un catalogo curato Top IMDb integrato per funzionare istantaneamente anche offline.

---

## 🛠️ Stack Tecnologico e Architettura

- **Linguaggio:** Kotlin 2.3 (JVM Toolchain 17)
- **Interfaccia Utente:** Jetpack Compose + Material Design 3 (tema scuro *Cinema & IMDb Gold*)
- **Architettura:** MVVM (`Model - View - ViewModel`) con `StateFlow` reattivo
- **Caricamento Immagini:** Coil (`io.coil-kt:coil-compose`)
- **Test:** JUnit 4 + `kotlinx-coroutines-test`

```text
app/src/main/kotlin/com/symon/movienight/
├── MainActivity.kt                  # Entry point dell'applicazione Android
├── model/
│   └── Movie.kt                     # Data class Movie e filtri per genere
├── data/
│   └── MovieRepository.kt           # Client Free IMDb API + Catalogo curato integrato
├── viewmodel/
│   └── MovieNightViewModel.kt       # Logica di stato (mazzo, swipe, filtri, sorteggio)
└── ui/
    ├── MovieNightScreen.kt          # Componenti Compose (SwipeableMovieCard, WinnerDialog)
    └── theme/                       # Tema scuro Cinema e palette colori IMDb
```

---

## 🚀 Come Compilare ed Eseguire

```bash
# Eseguire la suite di test unitari
./gradlew testDebugUnitTest

# Compilare il pacchetto installabile APK (in app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug
```

---

## 👤 Autore & Licenza

* **Autore:** [Simone Mariotti](https://github.com/Symon180)
* **Licenza:** Rilasciato sotto licenza [MIT](LICENSE).

