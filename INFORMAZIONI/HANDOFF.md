# Handoff unico - MovieNight (PickAMovie)

**Stato:** Documento unico di continuità del progetto
**Ultimo aggiornamento:** 01-11-2026 (MVP completo, testato e APK compilato)
**Versione sorgente:** 0.1.0
**Branch:** `main`

---

## 1. Obiettivo del Progetto
Realizzare **MovieNight (PickAMovie)**, un'app Android nativa in **Kotlin** e **Jetpack Compose** che aiuta a scegliere quale film guardare la sera tramite uno scorrimento di schede in stile Tinder (*"Mi ispira"* / *"Salta"*) e un pulsante *"Scegli per me a caso"* (🎲), utilizzando gratuitamente il catalogo ufficiale IMDb.

---

## 2. Cosa è stato fatto finora (Fasi 0, 1, 2, 3, 4 completate)
- [x] **Ambiente Android:** Installato `android` CLI (`~/.local/bin/android`), Android SDK Platform 36 (`~/Android/Sdk`) e OpenJDK 21 (`~/.local/jdk21`).
- [x] **Modello Dati (`model/Movie.kt`):** Creata la `data class Movie` con calcolo automatico della durata formattata (`2h 28m`), link diretto alla pagina IMDb e filtri per genere (`MovieGenreFilter`).
- [x] **Repository IMDb (`data/MovieRepository.kt`):** Implementato `DefaultMovieRepository` con supporto a `api.imdbapi.dev`, ricerca live sull'endpoint pubblico IMDb (`v3.sg.media-imdb.com/suggestion`) e catalogo curato Top IMDb integrato (`CuratedImdbCatalog`) con trame in italiano e poster ufficiali.
- [x] **Logica MVVM (`viewmodel/MovieNightViewModel.kt`):** Gestione reattiva con `StateFlow` del mazzo di film, swipe a destra (`likeCurrentMovie`), swipe a sinistra (`skipCurrentMovie`), filtro per genere, ricerca live e sorteggio casuale (`pickRandomForTonight`).
- [x] **Interfaccia Jetpack Compose (`ui/MovieNightScreen.kt`):**
  - Tema scuro *Cinema & IMDb Gold* (`#F5C518`).
  - Scheda film trascinabile col dito (`SwipeableMovieCard`) con fisica a molla e timbro dinamico `"MI ISPIRA ❤️"` / `"SALTA ✖"`.
  - Finestra modale celebrativa del film estratto (`WinnerMovieDialog`) con apertura scheda IMDb e pannello riepilogativo dei candidati della serata (`ShortlistBottomSheet`).
- [x] **Verifica e Build:**
  - Tutti i 5 test unitari in `MainScreenViewModelTest.kt` passano al 100% (`./gradlew testDebugUnitTest`).
  - Compilato con successo il pacchetto installabile `app/build/outputs/apk/debug/app-debug.apk` (`./gradlew assembleDebug`).

---

## 3. Azioni Aperte / Prossimi Passi Consigliati
- [ ] Visualizzare e ispezionare la schermata in **Android Compose Preview** o avviare un emulatore in **Android Device Hub**.
- [ ] Eventuale salvataggio persistente della lista *"Mi ispira"* su memoria locale del telefono (`DataStore` o `Room`) così i film salvati restano anche chiudendo l'app.
- [ ] Aggiungere modalità *"Sfida a 2"* (passa il telefono al partner e mostra i match in comune).
