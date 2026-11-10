package com.symon.movienight.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.symon.movienight.data.CuratedImdbCatalog
import com.symon.movienight.data.DefaultMovieRepository
import com.symon.movienight.data.SharedPreferencesLibraryStorage
import com.symon.movienight.model.AnimeFormatFilter
import com.symon.movienight.model.AppTab
import com.symon.movienight.model.CastMember
import com.symon.movienight.model.EraFilter
import com.symon.movienight.model.MediaSource
import com.symon.movienight.model.MediaType
import com.symon.movienight.model.Movie
import com.symon.movienight.model.MovieGenreFilter
import com.symon.movienight.model.UserMediaProgress
import com.symon.movienight.ui.theme.CinemaBackground
import com.symon.movienight.ui.theme.CinemaCardSurface
import com.symon.movienight.ui.theme.CinemaSurface
import com.symon.movienight.ui.theme.ImdbGold
import com.symon.movienight.ui.theme.LikeGreen
import com.symon.movienight.ui.theme.MalBlue
import com.symon.movienight.ui.theme.MalBlueLight
import com.symon.movienight.ui.theme.MovieNightTheme
import com.symon.movienight.ui.theme.SkipRed
import com.symon.movienight.ui.theme.TextPrimary
import com.symon.movienight.ui.theme.TextSecondary
import com.symon.movienight.viewmodel.MovieNightUiState
import com.symon.movienight.viewmodel.MovieNightViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Schermata principale collegata al ViewModel e alla memoria persistente del telefono.
 */
@Composable
fun MovieNightRoute() {
  val context = LocalContext.current
  val viewModel: MovieNightViewModel = viewModel(
    factory = remember(context) {
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return MovieNightViewModel(
            repository = DefaultMovieRepository(),
            storage = SharedPreferencesLibraryStorage(context)
          ) as T
        }
      }
    }
  )
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  MovieNightScreen(
    uiState = uiState,
    onSelectTab = viewModel::selectTab,
    onSelectMediaType = viewModel::selectMediaType,
    onSelectAnimeFormat = viewModel::selectAnimeFormat,
    onSelectGenre = viewModel::selectGenre,
    onSelectEra = viewModel::selectEra,
    onLoadMoreDeck = viewModel::loadMoreDeck,
    onSearchQueryChange = viewModel::searchMovies,
    onSwipeLike = viewModel::likeCurrentMovie,
    onSwipeSkip = viewModel::skipCurrentMovie,
    onSwipeUpWatched = { viewModel.markCurrentMovieWatched(null) },
    onSwipeDownPool = viewModel::putBackInPool,
    onOpenDetails = viewModel::openProgramDetails,
    onCloseDetails = viewModel::closeProgramDetails,
    onAddToWatchlist = viewModel::addToWatchlist,
    onMarkWatched = viewModel::markAsWatched,
    onMoveToWatchlist = viewModel::moveBackToWatchlist,
    onRemoveWatched = viewModel::removeFromWatched,
    onRateMedia = viewModel::rateMedia,
    onOpenEpisodes = viewModel::openEpisodeTracker,
    onCloseEpisodes = viewModel::closeEpisodeTracker,
    onToggleEpisodeWatched = viewModel::toggleEpisodeWatched,
    onRateEpisode = viewModel::rateEpisode,
    onImportMal = viewModel::importFromMyAnimeList,
    onDismissMalMessage = viewModel::dismissMalSyncMessage,
    onPickRandom = viewModel::pickRandomForTonight,
    onReshuffle = viewModel::reshuffleDeck,
    onToggleShortlist = viewModel::toggleShortlistSheet,
    onRemoveLiked = viewModel::removeFromLiked,
    onChooseDirectly = viewModel::chooseMovieDirectly,
    onDismissWinner = viewModel::dismissWinner
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieNightScreen(
  uiState: MovieNightUiState,
  onSelectGenre: (MovieGenreFilter) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSwipeLike: () -> Unit,
  onSwipeSkip: () -> Unit,
  onPickRandom: () -> Unit,
  onReshuffle: () -> Unit,
  onToggleShortlist: (Boolean) -> Unit,
  onRemoveLiked: (Movie) -> Unit,
  onChooseDirectly: (Movie) -> Unit,
  onDismissWinner: () -> Unit,
  modifier: Modifier = Modifier,
  onSelectTab: (AppTab) -> Unit = {},
  onSelectMediaType: (MediaType) -> Unit = {},
  onSelectAnimeFormat: (AnimeFormatFilter) -> Unit = {},
  onSelectEra: (EraFilter) -> Unit = {},
  onLoadMoreDeck: () -> Unit = {},
  onSwipeUpWatched: () -> Unit = {},
  onSwipeDownPool: () -> Unit = {},
  onOpenDetails: (Movie) -> Unit = {},
  onCloseDetails: () -> Unit = {},
  onAddToWatchlist: (Movie) -> Unit = {},
  onMarkWatched: (Movie, Int?) -> Unit = { _, _ -> },
  onMoveToWatchlist: (Movie) -> Unit = {},
  onRemoveWatched: (Movie) -> Unit = {},
  onRateMedia: (String, Int) -> Unit = { _, _ -> },
  onOpenEpisodes: (Movie) -> Unit = {},
  onCloseEpisodes: () -> Unit = {},
  onToggleEpisodeWatched: (Movie, String) -> Unit = { _, _ -> },
  onRateEpisode: (Movie, String, Int) -> Unit = { _, _, _ -> },
  onImportMal: (String) -> Unit = {},
  onDismissMalMessage: () -> Unit = {}
) {
  var showSearchBar by remember { mutableStateOf(false) }
  val isAnimeMode = uiState.selectedMediaType == MediaType.ANIME

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = CinemaBackground,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = CinemaBackground,
          titleContentColor = TextPrimary
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = if (isAnimeMode) MalBlue else ImdbGold,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.padding(end = 10.dp)
            ) {
              Text(
                text = if (isAnimeMode) "MAL" else "IMDb",
                color = if (isAnimeMode) Color.White else Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Column {
              Text(
                text = "MovieNight",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
              )
              Text(
                text = when (uiState.selectedTab) {
                  AppTab.DISCOVER -> "Tocca una scheda per tutti i dettagli"
                  AppTab.WATCHLIST -> "La tua lista Da Vedere (${uiState.likedMovies.size})"
                  AppTab.WATCHED -> "Diario Già Visti (${uiState.watchedMovies.size})"
                },
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }
        },
        actions = {
          // Pulsante "Sorteggia 🎲" posizionato in alto così NON copre mai i tasti dello Swipe!
          Surface(
            color = ImdbGold,
            shape = RoundedCornerShape(50),
            modifier = Modifier
              .padding(end = 4.dp)
              .clickable { onPickRandom() }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = "Sorteggia",
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Sorteggia",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
              )
            }
          }

          if (uiState.selectedTab == AppTab.DISCOVER) {
            IconButton(onClick = { showSearchBar = !showSearchBar }) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cerca titolo",
                tint = if (showSearchBar) ImdbGold else TextPrimary
              )
            }
          }
        }
      )
    },
    bottomBar = {
      MovieNightBottomBar(
        selectedTab = uiState.selectedTab,
        watchlistCount = uiState.likedMovies.size,
        watchedCount = uiState.watchedMovies.size,
        onSelectTab = onSelectTab
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (uiState.selectedTab) {
        AppTab.DISCOVER -> {
          DiscoverSection(
            uiState = uiState,
            showSearchBar = showSearchBar,
            onSelectMediaType = onSelectMediaType,
            onSelectAnimeFormat = onSelectAnimeFormat,
            onSelectGenre = onSelectGenre,
            onSelectEra = onSelectEra,
            onLoadMoreDeck = onLoadMoreDeck,
            onSearchQueryChange = onSearchQueryChange,
            onSwipeRightLike = onSwipeLike,
            onSwipeLeftDislike = onSwipeSkip,
            onSwipeUpWatched = onSwipeUpWatched,
            onSwipeDownPool = onSwipeDownPool,
            onOpenDetails = onOpenDetails,
            onOpenEpisodes = onOpenEpisodes,
            onReshuffle = onReshuffle,
            onPickRandom = onPickRandom
          )
        }

        AppTab.WATCHLIST -> {
          WatchlistSection(
            watchlist = uiState.likedMovies,
            userProgress = uiState.userProgress,
            onOpenDetails = onOpenDetails,
            onPickRandom = onPickRandom,
            onMarkWatched = { movie -> onMarkWatched(movie, null) },
            onOpenEpisodes = onOpenEpisodes,
            onRemoveFromWatchlist = onRemoveLiked,
            onGoToDiscover = { onSelectTab(AppTab.DISCOVER) }
          )
        }

        AppTab.WATCHED -> {
          WatchedSection(
            watchedMovies = uiState.watchedMovies,
            userProgress = uiState.userProgress,
            totalEpisodesWatched = uiState.totalEpisodesWatched,
            malUsername = uiState.malUsername,
            isSyncingMal = uiState.isSyncingMal,
            malSyncMessage = uiState.malSyncMessage,
            onOpenDetails = onOpenDetails,
            onRateMedia = onRateMedia,
            onOpenEpisodes = onOpenEpisodes,
            onMoveToWatchlist = onMoveToWatchlist,
            onRemoveWatched = onRemoveWatched,
            onImportMal = onImportMal,
            onDismissMalMessage = onDismissMalMessage,
            onGoToDiscover = { onSelectTab(AppTab.DISCOVER) }
          )
        }
      }
    }
  }

  // Finestra modale celebrativa quando viene estratto il titolo della serata!
  uiState.pickedWinner?.let { winner ->
    WinnerMovieDialog(
      movie = winner,
      onDismiss = onDismissWinner,
      onPickAnother = onPickRandom,
      onOpenDetails = {
        onDismissWinner()
        onOpenDetails(winner)
      },
      onMarkWatched = {
        onMarkWatched(winner, null)
        onDismissWinner()
      }
    )
  }

  // SCHEDA DETTAGLIO PROGRAMMA APRIBILE (Trama, Attori, Dove guardarlo con Redirect, Lingue, Simili)
  uiState.selectedMediaForDetails?.let { media ->
    ProgramDetailSheet(
      media = media,
      isInWatchlist = uiState.likedMovies.any { it.id == media.id },
      isWatched = uiState.watchedMovies.any { it.id == media.id },
      onDismiss = onCloseDetails,
      onAddToWatchlist = { onAddToWatchlist(media) },
      onMarkWatched = { onMarkWatched(media, null) },
      onOpenEpisodes = {
        onCloseDetails()
        onOpenEpisodes(media)
      },
      onSelectSimilar = { similar -> onOpenDetails(similar) }
    )
  }

  // BottomSheet "Episodi & Ratings" per Serie TV e Anime (senza limite di 6!)
  uiState.selectedMediaForEpisodes?.let { media ->
    EpisodeTrackerSheet(
      media = media,
      isLoading = uiState.isLoadingEpisodes,
      progress = uiState.userProgress[media.id] ?: UserMediaProgress(mediaId = media.id),
      onDismiss = onCloseEpisodes,
      onToggleEpisodeWatched = { epId -> onToggleEpisodeWatched(media, epId) },
      onRateEpisode = { epId, stars -> onRateEpisode(media, epId, stars) }
    )
  }
}

@Composable
private fun MovieNightBottomBar(
  selectedTab: AppTab,
  watchlistCount: Int,
  watchedCount: Int,
  onSelectTab: (AppTab) -> Unit
) {
  NavigationBar(
    containerColor = CinemaSurface,
    contentColor = TextPrimary
  ) {
    NavigationBarItem(
      selected = selectedTab == AppTab.DISCOVER,
      onClick = { onSelectTab(AppTab.DISCOVER) },
      icon = {
        Icon(imageVector = Icons.Default.Style, contentDescription = "Scopri")
      },
      label = { Text("Scopri", fontWeight = FontWeight.SemiBold) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.Black,
        selectedTextColor = ImdbGold,
        indicatorColor = ImdbGold,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
      )
    )

    NavigationBarItem(
      selected = selectedTab == AppTab.WATCHLIST,
      onClick = { onSelectTab(AppTab.WATCHLIST) },
      icon = {
        BadgedBox(
          badge = {
            if (watchlistCount > 0) {
              Badge(containerColor = LikeGreen, contentColor = Color.Black) {
                Text(watchlistCount.toString(), fontWeight = FontWeight.Bold)
              }
            }
          }
        ) {
          Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Da Vedere")
        }
      },
      label = { Text("Da Vedere", fontWeight = FontWeight.SemiBold) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.Black,
        selectedTextColor = ImdbGold,
        indicatorColor = ImdbGold,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
      )
    )

    NavigationBarItem(
      selected = selectedTab == AppTab.WATCHED,
      onClick = { onSelectTab(AppTab.WATCHED) },
      icon = {
        BadgedBox(
          badge = {
            if (watchedCount > 0) {
              Badge(containerColor = ImdbGold, contentColor = Color.Black) {
                Text(watchedCount.toString(), fontWeight = FontWeight.Bold)
              }
            }
          }
        ) {
          Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Già Visti")
        }
      },
      label = { Text("Già Visti", fontWeight = FontWeight.SemiBold) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.Black,
        selectedTextColor = ImdbGold,
        indicatorColor = ImdbGold,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
      )
    )
  }
}

/**
 * Sezione 1: SCOPRI (Swipe a 4 direzioni + Scheda apribile al tocco).
 */
@Composable
private fun DiscoverSection(
  uiState: MovieNightUiState,
  showSearchBar: Boolean,
  onSelectMediaType: (MediaType) -> Unit,
  onSelectAnimeFormat: (AnimeFormatFilter) -> Unit,
  onSelectGenre: (MovieGenreFilter) -> Unit,
  onSelectEra: (EraFilter) -> Unit,
  onLoadMoreDeck: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSwipeRightLike: () -> Unit,
  onSwipeLeftDislike: () -> Unit,
  onSwipeUpWatched: () -> Unit,
  onSwipeDownPool: () -> Unit,
  onOpenDetails: (Movie) -> Unit,
  onOpenEpisodes: (Movie) -> Unit,
  onReshuffle: () -> Unit,
  onPickRandom: () -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    MediaTypeSelectorRow(
      selectedType = uiState.selectedMediaType,
      onSelectType = onSelectMediaType
    )

    // Se siamo nella sezione Anime, mostriamo la divisione tra Serie Anime 📺 e Film Anime 🎬!
    AnimatedVisibility(visible = uiState.selectedMediaType == MediaType.ANIME) {
      AnimeFormatSelectorRow(
        selectedFormat = uiState.selectedAnimeFormat,
        onSelectFormat = onSelectAnimeFormat
      )
    }

    AnimatedVisibility(visible = showSearchBar) {
      val searchPlaceholder = when (uiState.selectedMediaType) {
        MediaType.MOVIE -> "Cerca film o attore su IMDb..."
        MediaType.TV_SERIES -> "Cerca serie TV su IMDb..."
        MediaType.ANIME -> "Cerca serie o film anime su MyAnimeList..."
      }
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text(searchPlaceholder) },
        singleLine = true,
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = ImdbGold)
        },
        trailingIcon = {
          if (uiState.searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Default.Close, contentDescription = "Cancella")
            }
          }
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = ImdbGold,
          unfocusedBorderColor = CinemaCardSurface,
          focusedContainerColor = CinemaSurface,
          unfocusedContainerColor = CinemaSurface
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
      )
    }

    GenreFilterRow(
      selectedGenre = uiState.selectedGenre,
      onSelectGenre = onSelectGenre
    )

    // Filtro per Epoca / Decennio (per scoprire anche i grandi classici degli anni '70, '80, '90, 2000!)
    EraFilterRow(
      selectedEra = uiState.selectedEra,
      onSelectEra = onSelectEra
    )

    // Area centrale con il mazzo infinito di schede (senza la riga di emoticon sopra il mazzo!)
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      when {
        uiState.isLoading -> {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
              color = if (uiState.selectedMediaType == MediaType.ANIME) MalBlueLight else ImdbGold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (uiState.selectedMediaType == MediaType.ANIME) {
                "Connessione a MyAnimeList..."
              } else {
                "Caricamento catalogo IMDb..."
              },
              color = TextSecondary
            )
          }
        }

        uiState.currentMovie != null -> {
          // Carta sottostante per dare profondità al mazzo
          uiState.nextMovieInDeck?.let { nextMovie ->
            MovieCardContent(
              movie = nextMovie,
              dragOffsetX = 0f,
              dragOffsetY = 0f,
              onOpenDetails = {},
              onOpenEpisodes = {},
              modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 86.dp)
                .scale(0.94f)
                .alpha(0.55f)
            )
          }

          // Carta attiva in primo piano trascinabile in TUTTE E 4 LE DIREZIONI e cliccabile!
          val activeMovie = uiState.currentMovie!!
          SwipeableMovieCard(
            movie = activeMovie,
            onSwipedRight = onSwipeRightLike,
            onSwipedLeft = onSwipeLeftDislike,
            onSwipedUp = onSwipeUpWatched,
            onSwipedDown = onSwipeDownPool,
            onCardClick = { onOpenDetails(activeMovie) },
            onOpenEpisodes = { onOpenEpisodes(activeMovie) },
            modifier = Modifier
              .fillMaxSize()
              .padding(bottom = 86.dp)
          )

          // I 4 Pulsanti fisici sotto la carta (non più coperti da nessun testo!)
          ActionButtonsRow(
            onDislikeForever = onSwipeLeftDislike,
            onPutBackInPool = onSwipeDownPool,
            onMarkWatched = onSwipeUpWatched,
            onLikeToWatchlist = onSwipeRightLike,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 4.dp)
          )
        }

        else -> {
          EmptyDeckState(
            likedCount = uiState.likedMovies.size,
            onReshuffle = onReshuffle,
            onLoadMore = onLoadMoreDeck,
            onPickRandom = onPickRandom
          )
        }
      }
    }
  }
}

@Composable
private fun MediaTypeSelectorRow(
  selectedType: MediaType,
  onSelectType: (MediaType) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    MediaType.entries.forEach { type ->
      val isSelected = type == selectedType
      val activeColor = if (type == MediaType.ANIME) MalBlue else ImdbGold
      val activeTextColor = if (type == MediaType.ANIME) Color.White else Color.Black

      Surface(
        color = if (isSelected) activeColor else CinemaSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
          1.dp,
          if (isSelected) activeColor else CinemaCardSurface
        ),
        modifier = Modifier
          .weight(1f)
          .clickable { onSelectType(type) }
      ) {
        Text(
          text = type.label,
          color = if (isSelected) activeTextColor else TextSecondary,
          fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
          fontSize = 13.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        )
      }
    }
  }
}

/**
 * Sotto-selettore per la sezione Anime: divide gli Anime in "Tutti ⛩️", "Serie Anime 📺" e "Film Anime 🎬"!
 */
@Composable
private fun AnimeFormatSelectorRow(
  selectedFormat: AnimeFormatFilter,
  onSelectFormat: (AnimeFormatFilter) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    AnimeFormatFilter.entries.forEach { format ->
      val isSelected = format == selectedFormat
      Surface(
        color = if (isSelected) MalBlueLight else CinemaSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
          1.dp,
          if (isSelected) MalBlueLight else CinemaCardSurface
        ),
        modifier = Modifier
          .weight(1f)
          .clickable { onSelectFormat(format) }
      ) {
        Text(
          text = format.label,
          color = if (isSelected) Color.Black else TextSecondary,
          fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
          fontSize = 12.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun GenreFilterRow(
  selectedGenre: MovieGenreFilter,
  onSelectGenre: (MovieGenreFilter) -> Unit
) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 1.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(MovieGenreFilter.entries) { genre ->
      val isSelected = genre == selectedGenre
      FilterChip(
        selected = isSelected,
        onClick = { onSelectGenre(genre) },
        label = {
          Text(
            text = genre.label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = ImdbGold,
          selectedLabelColor = Color.Black,
          containerColor = CinemaSurface,
          labelColor = TextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = isSelected,
          borderColor = CinemaCardSurface,
          selectedBorderColor = ImdbGold
        )
      )
    }
  }
}

/**
 * Barra dei filtri per Epoca / Decennio (2020+, Anni 2010, Anni 2000, Anni '90, Anni '80, Classici < '80).
 */
@Composable
private fun EraFilterRow(
  selectedEra: EraFilter,
  onSelectEra: (EraFilter) -> Unit
) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 1.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(EraFilter.entries) { era ->
      val isSelected = era == selectedEra
      FilterChip(
        selected = isSelected,
        onClick = { onSelectEra(era) },
        label = {
          Text(
            text = era.label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MalBlueLight,
          selectedLabelColor = Color.Black,
          containerColor = CinemaSurface,
          labelColor = TextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = isSelected,
          borderColor = CinemaCardSurface,
          selectedBorderColor = MalBlueLight
        )
      )
    }
  }
}

/**
 * Gestisce il gesto di trascinamento (Swipe in 4 direzioni: Destra, Sinistra, Alto, Basso)
 * e il tocco sulla scheda per aprire i dettagli completi!
 */
@Composable
fun SwipeableMovieCard(
  movie: Movie,
  onSwipedRight: () -> Unit,
  onSwipedLeft: () -> Unit,
  modifier: Modifier = Modifier,
  onSwipedUp: () -> Unit = {},
  onSwipedDown: () -> Unit = {},
  onCardClick: () -> Unit = {},
  onOpenEpisodes: () -> Unit = {}
) {
  val scope = rememberCoroutineScope()
  val offsetX = remember(movie.id) { Animatable(0f) }
  val offsetY = remember(movie.id) { Animatable(0f) }
  val horizontalThreshold = 220f
  val verticalThreshold = 200f

  Box(
    modifier = modifier
      .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
      .rotate(offsetX.value / 30f)
      .clickable { onCardClick() }
      .pointerInput(movie.id) {
        detectDragGestures(
          onDragEnd = {
            scope.launch {
              val dx = offsetX.value
              val dy = offsetY.value
              if (abs(dx) >= abs(dy)) {
                when {
                  dx > horizontalThreshold -> {
                    offsetX.animateTo(1200f, tween(200))
                    onSwipedRight()
                  }
                  dx < -horizontalThreshold -> {
                    offsetX.animateTo(-1200f, tween(200))
                    onSwipedLeft()
                  }
                  else -> {
                    launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
                    launch { offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
                  }
                }
              } else {
                when {
                  dy < -verticalThreshold -> {
                    offsetY.animateTo(-1200f, tween(200))
                    onSwipedUp()
                  }
                  dy > verticalThreshold -> {
                    offsetY.animateTo(1200f, tween(200))
                    onSwipedDown()
                  }
                  else -> {
                    launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
                    launch { offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
                  }
                }
              }
            }
          },
          onDragCancel = {
            scope.launch {
              launch { offsetX.animateTo(0f, tween(180)) }
              launch { offsetY.animateTo(0f, tween(180)) }
            }
          },
          onDrag = { change, dragAmount ->
            change.consume()
            scope.launch {
              offsetX.snapTo(offsetX.value + dragAmount.x)
              offsetY.snapTo(offsetY.value + dragAmount.y)
            }
          }
        )
      }
  ) {
    MovieCardContent(
      movie = movie,
      dragOffsetX = offsetX.value,
      dragOffsetY = offsetY.value,
      onOpenDetails = onCardClick,
      onOpenEpisodes = onOpenEpisodes,
      modifier = Modifier.fillMaxSize()
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieCardContent(
  movie: Movie,
  dragOffsetX: Float,
  modifier: Modifier = Modifier,
  dragOffsetY: Float = 0f,
  onOpenDetails: () -> Unit = {},
  onOpenEpisodes: () -> Unit = {}
) {
  val isMal = movie.source == MediaSource.MYANIMELIST

  Card(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = CinemaCardSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(Color(0xFF2C344B), CinemaSurface, CinemaBackground)
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.LocalMovies,
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.08f),
          modifier = Modifier.size(120.dp)
        )
        AsyncImage(
          model = movie.posterUrl,
          contentDescription = "Locandina di ${movie.title}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.0f to Color.Black.copy(alpha = 0.30f),
              0.35f to Color.Transparent,
              0.58f to Color.Black.copy(alpha = 0.86f),
              1.0f to Color(0xFF0A0C10)
            )
          )
      )

      // Etichetta Mood/Tipo in alto a sinistra + Voto IMDb / MyAnimeList in alto a destra
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = if (isMal) MalBlue else Color.Black.copy(alpha = 0.78f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, if (isMal) MalBlueLight else ImdbGold.copy(alpha = 0.5f))
        ) {
          Text(
            text = if (movie.moodTag.isNotBlank()) movie.moodTag else movie.mediaType.badgeLabel,
            color = if (isMal) Color.White else ImdbGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }

        Surface(
          color = if (isMal) MalBlue else ImdbGold,
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = if (isMal) ImdbGold else Color.Black,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${movie.imdbRating}",
              color = if (isMal) Color.White else Color.Black,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp
            )
            Text(
              text = " ${movie.source.badgeText}",
              color = if (isMal) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.75f),
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            )
          }
        }
      }

      // Timbri visivi per tutte e 4 le direzioni di Swipe!
      val isHorizontalDominant = abs(dragOffsetX) >= abs(dragOffsetY)
      if (isHorizontalDominant && dragOffsetX > 40f) {
        val alpha = (abs(dragOffsetX) / 200f).coerceIn(0f, 1f)
        SwipeStamp(
          text = "IN LISTA ❤️",
          color = LikeGreen,
          alpha = alpha,
          rotation = -12f,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = 72.dp, start = 24.dp)
        )
      } else if (isHorizontalDominant && dragOffsetX < -40f) {
        val alpha = (abs(dragOffsetX) / 200f).coerceIn(0f, 1f)
        SwipeStamp(
          text = "MAI PIÙ ✖",
          color = SkipRed,
          alpha = alpha,
          rotation = 12f,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 72.dp, end = 24.dp)
        )
      } else if (!isHorizontalDominant && dragOffsetY < -40f) {
        val alpha = (abs(dragOffsetY) / 180f).coerceIn(0f, 1f)
        SwipeStamp(
          text = "GIÀ VISTO ✅",
          color = ImdbGold,
          alpha = alpha,
          rotation = 0f,
          modifier = Modifier
            .align(Alignment.Center)
        )
      } else if (!isHorizontalDominant && dragOffsetY > 40f) {
        val alpha = (abs(dragOffsetY) / 180f).coerceIn(0f, 1f)
        SwipeStamp(
          text = "RIMETTI NEL POOL 🔄",
          color = MalBlueLight,
          alpha = alpha,
          rotation = 0f,
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 80.dp)
        )
      }

      // Dettagli del programma nella parte inferiore della scheda
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        Text(
          text = movie.title,
          color = TextPrimary,
          fontSize = 23.sp,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "${movie.year}  •  ${movie.formattedRuntime}  •  ${movie.resolvedDirectorOrStudio}",
          color = if (isMal) MalBlueLight else ImdbGold,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pillole dei generi + Pulsanti "Info & Streaming" e "Episodi"
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
          ) {
            movie.genres.forEach { genre ->
              Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = genre,
                  color = TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (movie.isEpisodic) {
              Surface(
                color = ImdbGold.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ImdbGold),
                modifier = Modifier.clickable { onOpenEpisodes() }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = null,
                    tint = ImdbGold,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Episodi",
                    color = ImdbGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Surface(
              color = CinemaSurface.copy(alpha = 0.9f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
              modifier = Modifier.clickable { onOpenDetails() }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = "Apri scheda",
                  tint = TextPrimary,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Scheda",
                  color = TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = movie.plot,
          color = TextSecondary,
          fontSize = 13.sp,
          lineHeight = 18.sp,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun SwipeStamp(
  text: String,
  color: Color,
  alpha: Float,
  rotation: Float,
  modifier: Modifier = Modifier
) {
  Surface(
    color = color.copy(alpha = 0.22f * alpha),
    border = BorderStroke(3.dp, color.copy(alpha = alpha)),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.rotate(rotation)
  ) {
    Text(
      text = text,
      color = color.copy(alpha = alpha),
      fontWeight = FontWeight.ExtraBold,
      fontSize = 20.sp,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
    )
  }
}

/**
 * Barra inferiore sotto la carta con i 4 pulsanti corrispondenti alle 4 direzioni di Swipe:
 * 1. ✖️ Sinistra = Non farmelo più vedere
 * 2. 🔄 Basso = Rimettilo nel pool
 * 3. ✅ Alto = Già visto
 * 4. ❤️ Destra = Metti in lista (Da Vedere)
 */
@Composable
private fun ActionButtonsRow(
  onDislikeForever: () -> Unit,
  onPutBackInPool: () -> Unit,
  onMarkWatched: () -> Unit,
  onLikeToWatchlist: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. SINISTRA (✖️ Non farmelo più vedere)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      FilledIconButton(
        onClick = onDislikeForever,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
          containerColor = CinemaCardSurface,
          contentColor = SkipRed
        )
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Non mostrare più",
          modifier = Modifier.size(26.dp)
        )
      }
      Text("Mai più", color = SkipRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }

    // 2. BASSO (🔄 Rimettilo nel pool)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      FilledIconButton(
        onClick = onPutBackInPool,
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
          containerColor = CinemaCardSurface,
          contentColor = MalBlueLight
        )
      ) {
        Icon(
          imageVector = Icons.Default.Replay,
          contentDescription = "Rimetti nel pool",
          modifier = Modifier.size(22.dp)
        )
      }
      Text("Nel pool", color = MalBlueLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }

    // 3. ALTO (✅ Già visto)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      FilledIconButton(
        onClick = onMarkWatched,
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
          containerColor = CinemaCardSurface,
          contentColor = ImdbGold
        )
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Già visto",
          modifier = Modifier.size(22.dp)
        )
      }
      Text("Già visto", color = ImdbGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }

    // 4. DESTRA (❤️ Mettiamolo in lista)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      FilledIconButton(
        onClick = onLikeToWatchlist,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
          containerColor = LikeGreen,
          contentColor = Color.Black
        )
      ) {
        Icon(
          imageVector = Icons.Default.Favorite,
          contentDescription = "Metti in lista",
          modifier = Modifier.size(25.dp)
        )
      }
      Text("In lista", color = LikeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
  }
}

/**
 * SCHEDA DETTAGLIO PROGRAMMA COMPLETA (`ProgramDetailSheet`):
 * Mostra Trama integrale, Attori/Cast, Dove guardarlo con Link Redirect diretti,
 * Lingue disponibili (Audio e Sottotitoli) e Programmi Simili cliccabili!
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProgramDetailSheet(
  media: Movie,
  isInWatchlist: Boolean,
  isWatched: Boolean,
  onDismiss: () -> Unit,
  onAddToWatchlist: () -> Unit,
  onMarkWatched: () -> Unit,
  onOpenEpisodes: () -> Unit,
  onSelectSimilar: (Movie) -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isMal = media.source == MediaSource.MYANIMELIST
  val similarPrograms = remember(media.id) { CuratedImdbCatalog.findSimilarPrograms(media) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = CinemaSurface,
    contentColor = TextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      // 1. Header con Locandina, Titolo, Voto e Azioni Rapide
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        AsyncImage(
          model = media.posterUrl,
          contentDescription = media.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(width = 110.dp, height = 160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CinemaCardSurface)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = if (isMal) MalBlue else ImdbGold,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "${media.mediaType.badgeLabel} • ${media.source.badgeText}",
                color = if (isMal) Color.White else Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
            Text(
              text = "⭐ ${media.imdbRating}/10",
              color = ImdbGold,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = media.title,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = TextPrimary
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "${media.year} • ${media.formattedRuntime}",
            color = TextSecondary,
            fontSize = 13.sp
          )

          Text(
            text = "Regia / Studio: ${media.resolvedDirectorOrStudio}",
            color = if (isMal) MalBlueLight else ImdbGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )

          Spacer(modifier = Modifier.height(8.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            media.genres.forEach { g ->
              Surface(
                color = CinemaCardSurface,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = g,
                  color = TextPrimary,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Pulsanti azione ("In Lista ❤️", "Già Visto ✅", "Episodi 📊")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onAddToWatchlist,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isInWatchlist) CinemaCardSurface else LikeGreen,
            contentColor = if (isInWatchlist) LikeGreen else Color.Black
          ),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isInWatchlist) "Già in Lista" else "Metti in Lista",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }

        Button(
          onClick = onMarkWatched,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isWatched) CinemaCardSurface else ImdbGold,
            contentColor = if (isWatched) ImdbGold else Color.Black
          ),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isWatched) "Già Visto ✓" else "Segna Visto",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }
      }

      if (media.isEpisodic) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onOpenEpisodes,
          border = BorderStroke(1.dp, ImdbGold),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, tint = ImdbGold)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Apri tutti i ${maxOf(media.episodes.size, media.totalEpisodes)} Episodi & Ratings ⭐",
            color = ImdbGold,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 2. TRAMA COMPLETA
      Text(
        text = "📖 Trama",
        color = ImdbGold,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Surface(
        color = CinemaCardSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = media.plot,
          color = TextPrimary,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          modifier = Modifier.padding(14.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 3. DOVE GUARDARLO (CON LINK REDIRECT DIRETTI!)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = LikeGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Dove guardarlo (Tocca per aprire)",
          color = LikeGreen,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        media.watchLinks.forEach { provider ->
          Surface(
            color = Color(provider.badgeColorHex),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.clickable {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(provider.url))
              context.startActivity(intent)
            }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = provider.name,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }

        // Link diretto anche alla scheda ufficiale IMDb / MyAnimeList
        Surface(
          color = if (isMal) MalBlue else ImdbGold,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(media.externalPageUrl))
            context.startActivity(intent)
          }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Text(
              text = "Scheda ${media.source.badgeText}",
              color = if (isMal) Color.White else Color.Black,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = null,
              tint = if (isMal) Color.White else Color.Black,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 4. ATTORI / CAST PRINCIPALE CON FOTO RITRATTO!
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Person, contentDescription = null, tint = ImdbGold, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (media.mediaType == MediaType.ANIME) {
            "Personaggi, Doppiatori & Foto"
          } else {
            "Attori & Cast Principale"
          },
          color = ImdbGold,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
      ) {
        items(media.resolvedCastMembers, key = { it.name + it.role }) { member ->
          CastMemberPhotoCard(
            member = member,
            isMal = isMal
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 5. LINGUE DISPONIBILI (AUDIO & SOTTOTITOLI)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Language, contentDescription = null, tint = MalBlueLight, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Lingue e Sottotitoli disponibili",
          color = MalBlueLight,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Surface(
        color = CinemaCardSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = media.resolvedLanguages,
          color = TextPrimary,
          fontSize = 13.sp,
          modifier = Modifier.padding(12.dp)
        )
      }

      // 6. PROGRAMMI SIMILI CONSIGLIATI
      if (similarPrograms.isNotEmpty()) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text = "✨ Programmi simili consigliati",
          color = ImdbGold,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 28.dp)
        ) {
          items(similarPrograms, key = { it.id }) { sim ->
            Surface(
              color = CinemaCardSurface,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
              modifier = Modifier
                .width(126.dp)
                .clickable { onSelectSimilar(sim) }
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                AsyncImage(
                  model = sim.posterUrl,
                  contentDescription = sim.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CinemaSurface)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = sim.title,
                  color = TextPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "⭐ ${sim.imdbRating} • ${sim.year}",
                  color = ImdbGold,
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      } else {
        Spacer(modifier = Modifier.height(28.dp))
      }
    }
  }
}

/**
 * Scheda fotografica di un Attore o Personaggio con ritratto, nome e ruolo!
 */
@Composable
private fun CastMemberPhotoCard(
  member: CastMember,
  isMal: Boolean
) {
  Surface(
    color = CinemaCardSurface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    modifier = Modifier.width(106.dp)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(width = 88.dp, height = 98.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(
                if (isMal) MalBlue else Color(0xFF3A3218),
                CinemaSurface
              )
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        // Iniziali eleganti di fallback se la foto sta ancora caricando
        Text(
          text = member.initials,
          color = if (isMal) MalBlueLight else ImdbGold,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 22.sp
        )

        if (member.photoUrl.isNotBlank()) {
          AsyncImage(
            model = member.photoUrl,
            contentDescription = "Foto di ${member.name}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = member.name,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        lineHeight = 14.sp
      )

      if (member.role.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = member.role,
          color = if (isMal) MalBlueLight else ImdbGold,
          fontWeight = FontWeight.Medium,
          fontSize = 10.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

/**
 * Sezione 2: DA VEDERE (Lista Generale Illimitata di Film, Serie TV e Anime che vuoi guardare).
 */
@Composable
private fun WatchlistSection(
  watchlist: List<Movie>,
  userProgress: Map<String, UserMediaProgress>,
  onOpenDetails: (Movie) -> Unit,
  onPickRandom: () -> Unit,
  onMarkWatched: (Movie) -> Unit,
  onOpenEpisodes: (Movie) -> Unit,
  onRemoveFromWatchlist: (Movie) -> Unit,
  onGoToDiscover: () -> Unit
) {
  var filterType by remember { mutableStateOf<MediaType?>(null) }
  var filterQuery by remember { mutableStateOf("") }

  val filteredList = remember(watchlist, filterType, filterQuery) {
    watchlist.filter { item ->
      val matchType = filterType == null || item.mediaType == filterType
      val matchQuery = filterQuery.isBlank() || item.title.contains(filterQuery, ignoreCase = true)
      matchType && matchQuery
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Filtri rapidi per categoria (Tutti / Film / Serie TV / Anime)
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 6.dp)
    ) {
      item {
        FilterChip(
          selected = filterType == null,
          onClick = { filterType = null },
          label = { Text("Tutti (${watchlist.size})") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ImdbGold,
            selectedLabelColor = Color.Black,
            containerColor = CinemaSurface,
            labelColor = TextSecondary
          )
        )
      }
      items(MediaType.entries) { type ->
        val count = watchlist.count { it.mediaType == type }
        FilterChip(
          selected = filterType == type,
          onClick = { filterType = type },
          label = { Text("${type.label} ($count)") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (type == MediaType.ANIME) MalBlue else ImdbGold,
            selectedLabelColor = if (type == MediaType.ANIME) Color.White else Color.Black,
            containerColor = CinemaSurface,
            labelColor = TextSecondary
          )
        )
      }
    }

    if (watchlist.size >= 2) {
      OutlinedTextField(
        value = filterQuery,
        onValueChange = { filterQuery = it },
        placeholder = { Text("Filtra nella tua lista Da Vedere...") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ImdbGold) },
        trailingIcon = {
          if (filterQuery.isNotEmpty()) {
            IconButton(onClick = { filterQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = null)
            }
          }
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = ImdbGold,
          unfocusedBorderColor = CinemaCardSurface,
          focusedContainerColor = CinemaSurface,
          unfocusedContainerColor = CinemaSurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
      )
    }

    if (filteredList.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            tint = ImdbGold,
            modifier = Modifier.size(56.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Nessun programma in questa vista",
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Fai swipe a destra (❤️) nella sezione Scopri per costruire la tua lista personale senza alcun limite!",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = onGoToDiscover,
            colors = ButtonDefaults.buttonColors(
              containerColor = ImdbGold,
              contentColor = Color.Black
            )
          ) {
            Text("Esplora Film, Serie & Anime", fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(filteredList, key = { it.id }) { movie ->
          val prog = userProgress[movie.id]
          val watchedEpCount = prog?.watchedEpisodeIds?.size ?: 0

          Surface(
            color = CinemaCardSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenDetails(movie) }
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(width = 70.dp, height = 100.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(CinemaSurface)
              )

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    color = if (movie.source == MediaSource.MYANIMELIST) MalBlue else ImdbGold,
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = movie.mediaType.badgeLabel,
                      color = if (movie.source == MediaSource.MYANIMELIST) Color.White else Color.Black,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 10.sp,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Text(
                    text = "⭐ ${movie.imdbRating} ${movie.source.badgeText}",
                    color = ImdbGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                  text = movie.title,
                  fontWeight = FontWeight.ExtraBold,
                  color = TextPrimary,
                  fontSize = 16.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Text(
                  text = "${movie.year} • ${movie.formattedRuntime}",
                  color = TextSecondary,
                  fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                    onClick = { onMarkWatched(movie) },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = LikeGreen,
                      contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Già Visto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }

                  if (movie.isEpisodic) {
                    OutlinedButton(
                      onClick = { onOpenEpisodes(movie) },
                      border = BorderStroke(1.dp, ImdbGold.copy(alpha = 0.6f)),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.height(32.dp)
                    ) {
                      Text(
                        text = if (watchedEpCount > 0) "Ep ($watchedEpCount)" else "Episodi ⭐",
                        color = ImdbGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }

              IconButton(onClick = { onRemoveFromWatchlist(movie) }) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Rimuovi",
                  tint = TextSecondary
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Sezione 3: GIÀ VISTI & EPISODI.
 */
@Composable
private fun WatchedSection(
  watchedMovies: List<Movie>,
  userProgress: Map<String, UserMediaProgress>,
  totalEpisodesWatched: Int,
  malUsername: String,
  isSyncingMal: Boolean,
  malSyncMessage: String?,
  onOpenDetails: (Movie) -> Unit,
  onRateMedia: (String, Int) -> Unit,
  onOpenEpisodes: (Movie) -> Unit,
  onMoveToWatchlist: (Movie) -> Unit,
  onRemoveWatched: (Movie) -> Unit,
  onImportMal: (String) -> Unit,
  onDismissMalMessage: () -> Unit,
  onGoToDiscover: () -> Unit
) {
  var malInput by remember(malUsername) { mutableStateOf(malUsername) }
  var showMalCard by remember { mutableStateOf(false) }
  var filterType by remember { mutableStateOf<MediaType?>(null) }

  val filteredWatched = remember(watchedMovies, filterType) {
    if (filterType == null) watchedMovies else watchedMovies.filter { it.mediaType == filterType }
  }

  val ratedValues = userProgress.values.mapNotNull { it.userRating }
  val avgUserRating = if (ratedValues.isNotEmpty()) {
    ((ratedValues.average() * 10.0).roundToInt() / 10.0).toString()
  } else {
    "—"
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
  ) {
    item {
      Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ImdbGold.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            StatBadge(value = watchedMovies.size.toString(), label = "Titoli Visti 🎬")
            StatBadge(value = totalEpisodesWatched.toString(), label = "Episodi Visti 📺")
            StatBadge(value = "⭐ $avgUserRating", label = "Tuo Voto Medio")
          }

          Spacer(modifier = Modifier.height(12.dp))

          Surface(
            color = MalBlue.copy(alpha = 0.22f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MalBlueLight),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showMalCard = !showMalCard }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  color = MalBlue,
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "MAL",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = if (malUsername.isNotBlank()) {
                      "Profilo MyAnimeList: @$malUsername"
                    } else {
                      "Collega il tuo profilo MyAnimeList ⛩️"
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  Text(
                    text = "Importa anime visti, voti ed episodi da MAL",
                    color = TextSecondary,
                    fontSize = 11.sp
                  )
                }
              }
              Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Sincronizza MAL",
                tint = MalBlueLight
              )
            }
          }

          AnimatedVisibility(visible = showMalCard || malSyncMessage != null) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = malInput,
                  onValueChange = { malInput = it },
                  placeholder = { Text("Username MyAnimeList...") },
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MalBlueLight,
                    unfocusedBorderColor = CinemaCardSurface,
                    focusedContainerColor = CinemaCardSurface,
                    unfocusedContainerColor = CinemaCardSurface
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f)
                )
                Button(
                  onClick = { onImportMal(malInput.ifBlank { "Symon180" }) },
                  enabled = !isSyncingMal,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MalBlue,
                    contentColor = Color.White
                  )
                ) {
                  if (isSyncingMal) {
                    CircularProgressIndicator(
                      color = Color.White,
                      modifier = Modifier.size(16.dp),
                      strokeWidth = 2.dp
                    )
                  } else {
                    Text("Importa", fontWeight = FontWeight.Bold)
                  }
                }
              }

              malSyncMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  color = LikeGreen.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, LikeGreen.copy(alpha = 0.5f)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismissMalMessage() }
                ) {
                  Text(
                    text = msg,
                    color = LikeGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(10.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = filterType == null,
            onClick = { filterType = null },
            label = { Text("Tutti (${watchedMovies.size})") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ImdbGold,
              selectedLabelColor = Color.Black,
              containerColor = CinemaSurface,
              labelColor = TextSecondary
            )
          )
        }
        items(MediaType.entries) { type ->
          val count = watchedMovies.count { it.mediaType == type }
          FilterChip(
            selected = filterType == type,
            onClick = { filterType = type },
            label = { Text("${type.label} ($count)") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = if (type == MediaType.ANIME) MalBlue else ImdbGold,
              selectedLabelColor = if (type == MediaType.ANIME) Color.White else Color.Black,
              containerColor = CinemaSurface,
              labelColor = TextSecondary
            )
          )
        }
      }
    }

    if (filteredWatched.isEmpty()) {
      item {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = ImdbGold,
            modifier = Modifier.size(52.dp)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Nessun titolo ancora in 'Già Visti'",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Fai swipe verso l'alto (⬆️) sulle schede che hai già visto o spunta gli episodi di una Serie TV / Anime!",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(16.dp))
          OutlinedButton(
            onClick = onGoToDiscover,
            border = BorderStroke(1.dp, ImdbGold)
          ) {
            Text("Vai a Scopri", color = ImdbGold, fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      items(filteredWatched, key = { it.id }) { movie ->
        val prog = userProgress[movie.id] ?: UserMediaProgress(mediaId = movie.id)
        WatchedMediaCard(
          movie = movie,
          progress = prog,
          onOpenDetails = { onOpenDetails(movie) },
          onRateMedia = { stars -> onRateMedia(movie.id, stars) },
          onOpenEpisodes = { onOpenEpisodes(movie) },
          onRemove = { onRemoveWatched(movie) }
        )
      }
    }
  }
}

@Composable
private fun StatBadge(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      color = ImdbGold,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 20.sp
    )
    Text(
      text = label,
      color = TextSecondary,
      fontSize = 11.sp
    )
  }
}

@Composable
private fun WatchedMediaCard(
  movie: Movie,
  progress: UserMediaProgress,
  onOpenDetails: () -> Unit,
  onRateMedia: (Int) -> Unit,
  onOpenEpisodes: () -> Unit,
  onRemove: () -> Unit
) {
  var showRatingPicker by remember { mutableStateOf(false) }
  val isMal = movie.source == MediaSource.MYANIMELIST
  val totalEpCount = maxOf(movie.episodes.size, movie.totalEpisodes)
  val watchedEpCount = progress.watchedEpisodeIds.size

  Surface(
    color = CinemaCardSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onOpenDetails() }
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
          model = movie.posterUrl,
          contentDescription = movie.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(width = 68.dp, height = 96.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CinemaSurface)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              color = if (isMal) MalBlue else ImdbGold,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "${movie.mediaType.badgeLabel} • ${movie.source.badgeText}",
                color = if (isMal) Color.White else Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Text(
              text = "Ufficiale: ⭐ ${movie.imdbRating}",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = movie.title,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Text(
            text = "${movie.year} • ${movie.formattedRuntime}",
            color = TextSecondary,
            fontSize = 12.sp
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = if (progress.userRating != null) ImdbGold else CinemaSurface,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, ImdbGold),
              modifier = Modifier.clickable { showRatingPicker = !showRatingPicker }
            ) {
              Text(
                text = if (progress.userRating != null) {
                  "Tuo voto: ⭐ ${progress.userRating}/10"
                } else {
                  "Dai un voto ⭐"
                },
                color = if (progress.userRating != null) Color.Black else ImdbGold,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }

            if (movie.isEpisodic) {
              Surface(
                color = CinemaSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, LikeGreen.copy(alpha = 0.7f)),
                modifier = Modifier.clickable { onOpenEpisodes() }
              ) {
                Text(
                  text = "📊 Episodi ($watchedEpCount/$totalEpCount)",
                  color = LikeGreen,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          IconButton(onClick = onOpenDetails) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Dettagli programma",
              tint = ImdbGold,
              modifier = Modifier.size(20.dp)
            )
          }
          IconButton(onClick = onRemove) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Rimuovi da Già Visti",
              tint = TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      if (movie.isEpisodic && totalEpCount > 0) {
        Spacer(modifier = Modifier.height(10.dp))
        val ratio = (watchedEpCount.toFloat() / totalEpCount.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(
          progress = { ratio },
          color = LikeGreen,
          trackColor = CinemaSurface,
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
        )
      }

      AnimatedVisibility(visible = showRatingPicker) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          Text(
            text = "Seleziona il tuo voto personale (da 1 a 10):",
            color = TextSecondary,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          RatingBar1To10(
            currentRating = progress.userRating,
            onSelectRating = { stars ->
              onRateMedia(stars)
              showRatingPicker = false
            }
          )
        }
      }
    }
  }
}

@Composable
private fun RatingBar1To10(
  currentRating: Int?,
  onSelectRating: (Int) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    (1..10).forEach { num ->
      val isSelected = currentRating == num
      Surface(
        color = if (isSelected) ImdbGold else CinemaSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
          1.dp,
          if (isSelected) ImdbGold else TextSecondary.copy(alpha = 0.3f)
        ),
        modifier = Modifier
          .size(30.dp)
          .clickable { onSelectRating(num) }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = num.toString(),
            color = if (isSelected) Color.Black else TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }
      }
    }
  }
}

/**
 * Pannello Modale "Episodi & Ratings" per Serie TV e Anime (tutti gli episodi disponibili!).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeTrackerSheet(
  media: Movie,
  isLoading: Boolean,
  progress: UserMediaProgress,
  onDismiss: () -> Unit,
  onToggleEpisodeWatched: (String) -> Unit,
  onRateEpisode: (String, Int) -> Unit
) {
  val context = LocalContext.current
  val isAnime = media.mediaType == MediaType.ANIME
  var expandedEpisodeForRating by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = CinemaSurface,
    contentColor = TextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              color = if (media.source == MediaSource.MYANIMELIST) MalBlue else ImdbGold,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = media.source.badgeText,
                color = if (media.source == MediaSource.MYANIMELIST) Color.White else Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Text(
              text = media.title,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 19.sp,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Episodi visti: ${progress.watchedEpisodeIds.size}/${media.episodes.size} • Media ufficiale: ⭐ ${media.imdbRating}",
            color = TextSecondary,
            fontSize = 12.sp
          )
        }

        OutlinedButton(
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(media.externalPageUrl))
            context.startActivity(intent)
          },
          border = BorderStroke(1.dp, if (isAnime) MalBlueLight else ImdbGold),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = if (isAnime) MalBlueLight else ImdbGold,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = media.source.badgeText,
            color = if (isAnime) MalBlueLight else ImdbGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = ImdbGold)
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 36.dp)
        ) {
          items(media.episodes, key = { it.id }) { ep ->
            val isWatched = ep.id in progress.watchedEpisodeIds
            val userEpRating = progress.episodeRatings[ep.id]
            val isRatingExpanded = expandedEpisodeForRating == ep.id

            val badgeColor = when {
              ep.communityRating >= 9.6 -> ImdbGold
              ep.communityRating >= 9.0 -> LikeGreen
              else -> Color(0xFF3B445E)
            }
            val badgeTextColor = if (ep.communityRating >= 9.0) Color.Black else TextPrimary

            Surface(
              color = CinemaCardSurface,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(
                1.dp,
                if (isWatched) LikeGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  IconButton(
                    onClick = { onToggleEpisodeWatched(ep.id) },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = "Segna episodio visto",
                      tint = if (isWatched) LikeGreen else TextSecondary.copy(alpha = 0.35f),
                      modifier = Modifier.size(26.dp)
                    )
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(
                        text = ep.formattedCode(isAnime),
                        color = ImdbGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                      )
                      if (ep.airDate.isNotBlank()) {
                        Text(
                          text = "• ${ep.airDate}",
                          color = TextSecondary,
                          fontSize = 11.sp
                        )
                      }
                    }
                    Text(
                      text = ep.title,
                      color = TextPrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp
                    )
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  Column(horizontalAlignment = Alignment.End) {
                    Surface(
                      color = badgeColor,
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text(
                        text = "⭐ ${ep.communityRating}",
                        color = badgeTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                      color = if (userEpRating != null) ImdbGold.copy(alpha = 0.2f) else CinemaSurface,
                      shape = RoundedCornerShape(6.dp),
                      border = BorderStroke(1.dp, ImdbGold.copy(alpha = 0.6f)),
                      modifier = Modifier.clickable {
                        expandedEpisodeForRating = if (isRatingExpanded) null else ep.id
                      }
                    ) {
                      Text(
                        text = if (userEpRating != null) "Tuo: $userEpRating/10 ⭐" else "Vota ep ⭐",
                        color = ImdbGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                  }
                }

                AnimatedVisibility(visible = isRatingExpanded) {
                  Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                      text = "Quanto ti è piaciuto questo episodio (1-10)?",
                      color = TextSecondary,
                      fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    RatingBar1To10(
                      currentRating = userEpRating,
                      onSelectRating = { stars ->
                        onRateEpisode(ep.id, stars)
                        expandedEpisodeForRating = null
                      }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyDeckState(
  likedCount: Int,
  onReshuffle: () -> Unit,
  onPickRandom: () -> Unit,
  onLoadMore: () -> Unit = {}
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(24.dp)
  ) {
    Icon(
      imageVector = Icons.Default.LocalMovies,
      contentDescription = null,
      tint = ImdbGold,
      modifier = Modifier.size(64.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Vuoi vedere altri titoli?",
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      color = TextPrimary,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Puoi caricare subito nuove pagine dal database online oppure rimescolare l'intero catalogo senza limiti!",
      color = TextSecondary,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Button(
        onClick = onLoadMore,
        colors = ButtonDefaults.buttonColors(
          containerColor = MalBlueLight,
          contentColor = Color.Black
        )
      ) {
        Icon(Icons.Default.Sync, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Carica altri titoli", fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = onReshuffle,
        border = BorderStroke(1.dp, ImdbGold)
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, tint = ImdbGold)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Rimescola tutto", color = ImdbGold)
      }
    }
    if (likedCount > 0) {
      Spacer(modifier = Modifier.height(10.dp))
      Button(
        onClick = onPickRandom,
        colors = ButtonDefaults.buttonColors(
          containerColor = ImdbGold,
          contentColor = Color.Black
        )
      ) {
        Icon(Icons.Default.Casino, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Sorteggia dalla lista ($likedCount)", fontWeight = FontWeight.Bold)
      }
    }
  }
}

/**
 * Dialog che annuncia il titolo estratto a sorte per la serata.
 */
@Composable
fun WinnerMovieDialog(
  movie: Movie,
  onDismiss: () -> Unit,
  onPickAnother: () -> Unit,
  onOpenDetails: () -> Unit = {},
  onMarkWatched: () -> Unit = {}
) {
  val isMal = movie.source == MediaSource.MYANIMELIST

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = CinemaSurface),
      border = BorderStroke(2.dp, if (isMal) MalBlueLight else ImdbGold),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(20.dp)
      ) {
        Surface(
          color = if (isMal) MalBlue else ImdbGold,
          shape = RoundedCornerShape(50)
        ) {
          Text(
            text = "🍿 STASERA SI GUARDA!",
            color = if (isMal) Color.White else Color.Black,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
          modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CinemaCardSurface)
            .clickable { onOpenDetails() }
        ) {
          AsyncImage(
            model = movie.posterUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = movie.title,
          fontSize = 21.sp,
          fontWeight = FontWeight.ExtraBold,
          color = TextPrimary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "${movie.year} • ${movie.formattedRuntime} • ⭐ ${movie.imdbRating}/10 ${movie.source.badgeText}",
          color = ImdbGold,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = movie.plot,
          color = TextSecondary,
          fontSize = 13.sp,
          textAlign = TextAlign.Center,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = onOpenDetails,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isMal) MalBlue else ImdbGold,
            contentColor = if (isMal) Color.White else Color.Black
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Apri Scheda & Dove Guardarlo 📺",
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onMarkWatched,
            colors = ButtonDefaults.buttonColors(
              containerColor = LikeGreen,
              contentColor = Color.Black
            ),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Segna Visto", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = onPickAnother,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
          ) {
            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pesca altro", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun MovieNightScreenPreview() {
  MovieNightTheme {
    MovieNightScreen(
      uiState = MovieNightUiState(
        isLoading = false,
        deck = CuratedImdbCatalog.movies,
        currentIndex = 0,
        likedMovies = CuratedImdbCatalog.movies.take(2),
        watchedMovies = CuratedImdbCatalog.anime.take(1)
      ),
      onSelectGenre = {},
      onSearchQueryChange = {},
      onSwipeLike = {},
      onSwipeSkip = {},
      onPickRandom = {},
      onReshuffle = {},
      onToggleShortlist = {},
      onRemoveLiked = {},
      onChooseDirectly = {},
      onDismissWinner = {}
    )
  }
}
