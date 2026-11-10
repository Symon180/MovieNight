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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.symon.movienight.model.AppTab
import com.symon.movienight.model.Episode
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
    onSelectGenre = viewModel::selectGenre,
    onSearchQueryChange = viewModel::searchMovies,
    onSwipeLike = viewModel::likeCurrentMovie,
    onSwipeSkip = viewModel::skipCurrentMovie,
    onMarkCurrentWatched = { viewModel.markCurrentMovieWatched(null) },
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
  onMarkCurrentWatched: () -> Unit = {},
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
                fontSize = 20.sp
              )
              Text(
                text = when (uiState.selectedTab) {
                  AppTab.DISCOVER -> "Film, Serie TV & Anime MyAnimeList"
                  AppTab.WATCHLIST -> "La tua lista Da Vedere (${uiState.likedMovies.size})"
                  AppTab.WATCHED -> "Diario Già Visti & Ratings Episodi (${uiState.watchedMovies.size})"
                },
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }
        },
        actions = {
          if (uiState.selectedTab == AppTab.DISCOVER) {
            IconButton(onClick = { showSearchBar = !showSearchBar }) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cerca titolo",
                tint = if (showSearchBar) ImdbGold else TextPrimary
              )
            }
          }
          IconButton(
            onClick = { onSelectTab(AppTab.WATCHLIST) },
            modifier = Modifier.padding(end = 8.dp)
          ) {
            BadgedBox(
              badge = {
                if (uiState.likedMovies.isNotEmpty()) {
                  Badge(
                    containerColor = ImdbGold,
                    contentColor = Color.Black
                  ) {
                    Text(
                      text = uiState.likedMovies.size.toString(),
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Lista Da Vedere",
                tint = if (uiState.likedMovies.isNotEmpty()) LikeGreen else TextPrimary
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
    },
    floatingActionButton = {
      if (uiState.selectedTab == AppTab.DISCOVER || uiState.selectedTab == AppTab.WATCHLIST) {
        ExtendedFloatingActionButton(
          onClick = onPickRandom,
          containerColor = ImdbGold,
          contentColor = Color.Black,
          icon = {
            Icon(
              imageVector = Icons.Default.Casino,
              contentDescription = null
            )
          },
          text = {
            val label = if (uiState.likedMovies.isNotEmpty()) {
              "Sorteggia tra i ${uiState.likedMovies.size} Da Vedere!"
            } else {
              "Scegli per me a caso!"
            }
            Text(
              text = label,
              fontWeight = FontWeight.ExtraBold
            )
          }
        )
      }
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
            onSelectGenre = onSelectGenre,
            onSearchQueryChange = onSearchQueryChange,
            onSwipeLike = onSwipeLike,
            onSwipeSkip = onSwipeSkip,
            onMarkCurrentWatched = onMarkCurrentWatched,
            onOpenEpisodes = onOpenEpisodes,
            onReshuffle = onReshuffle,
            onPickRandom = onPickRandom
          )
        }

        AppTab.WATCHLIST -> {
          WatchlistSection(
            watchlist = uiState.likedMovies,
            userProgress = uiState.userProgress,
            onChooseForTonight = onChooseDirectly,
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
      onMarkWatched = {
        onMarkWatched(winner, null)
        onDismissWinner()
      },
      onOpenEpisodes = {
        onDismissWinner()
        onOpenEpisodes(winner)
      }
    )
  }

  // BottomSheet legacy rapido se invocato
  if (uiState.showShortlistSheet) {
    ShortlistBottomSheet(
      likedMovies = uiState.likedMovies,
      onDismiss = { onToggleShortlist(false) },
      onRemoveMovie = onRemoveLiked,
      onChooseMovie = onChooseDirectly,
      onPickRandomFromLikes = {
        onToggleShortlist(false)
        onPickRandom()
      }
    )
  }

  // BottomSheet "Episodi & Ratings" per Serie TV e Anime!
  uiState.selectedMediaForEpisodes?.let { media ->
    EpisodeTrackerSheet(
      media = media,
      isLoading = uiState.isLoadingEpisodes,
      progress = uiState.userProgress[media.id] ?: UserMediaProgress(mediaId = media.id),
      onDismiss = onCloseEpisodes,
      onToggleEpisodeWatched = { epId -> onToggleEpisodeWatched(media, epId) },
      onRateEpisode = { epId, stars -> onRateEpisode(media, epId, stars) },
      onRateWholeSeries = { stars -> onRateMedia(media.id, stars) }
    )
  }
}

/**
 * Barra di navigazione inferiore a 3 sezioni:
 * 1. Scopri (Swipe)
 * 2. Da Vedere (Watchlist)
 * 3. Già Visti & Episodi (Watched)
 */
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
 * Sezione 1: SCOPRI (Swipe stile Tinder per Film, Serie TV e Anime MyAnimeList).
 */
@Composable
private fun DiscoverSection(
  uiState: MovieNightUiState,
  showSearchBar: Boolean,
  onSelectMediaType: (MediaType) -> Unit,
  onSelectGenre: (MovieGenreFilter) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSwipeLike: () -> Unit,
  onSwipeSkip: () -> Unit,
  onMarkCurrentWatched: () -> Unit,
  onOpenEpisodes: (Movie) -> Unit,
  onReshuffle: () -> Unit,
  onPickRandom: () -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    // Selettore principale: Film 🎬 | Serie TV 📺 | Anime ⛩️
    MediaTypeSelectorRow(
      selectedType = uiState.selectedMediaType,
      onSelectType = onSelectMediaType
    )

    // Barra di ricerca live (IMDb per Film/Serie, MyAnimeList Jikan per Anime)
    AnimatedVisibility(visible = showSearchBar) {
      val searchPlaceholder = when (uiState.selectedMediaType) {
        MediaType.MOVIE -> "Cerca film o attore su IMDb..."
        MediaType.TV_SERIES -> "Cerca serie TV su IMDb..."
        MediaType.ANIME -> "Cerca anime su MyAnimeList..."
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

    // Barra orizzontale con i filtri di genere
    GenreFilterRow(
      selectedGenre = uiState.selectedGenre,
      onSelectGenre = onSelectGenre
    )

    // Area centrale con il mazzo di schede (Swipe Deck)
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
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
              onOpenEpisodes = {},
              modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 84.dp)
                .scale(0.94f)
                .alpha(0.55f)
            )
          }

          // Carta attiva in primo piano trascinabile col dito
          val activeMovie = uiState.currentMovie!!
          SwipeableMovieCard(
            movie = activeMovie,
            onSwipedRight = onSwipeLike,
            onSwipedLeft = onSwipeSkip,
            onOpenEpisodes = { onOpenEpisodes(activeMovie) },
            modifier = Modifier
              .fillMaxSize()
              .padding(bottom = 84.dp)
          )

          // Pulsanti fisici "Salta" (✖️), "Già Visto" (✅) e "Da Vedere" (❤️) in basso
          ActionButtonsRow(
            remainingCount = uiState.remainingInDeck,
            onSkip = onSwipeSkip,
            onMarkWatched = onMarkCurrentWatched,
            onLike = onSwipeLike,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 8.dp)
          )
        }

        else -> {
          EmptyDeckState(
            likedCount = uiState.likedMovies.size,
            onReshuffle = onReshuffle,
            onPickRandom = onPickRandom
          )
        }
      }
    }
  }
}

/**
 * Selettore a 3 pulsanti per scegliere tra Film 🎬, Serie TV 📺 e Anime ⛩️ (MyAnimeList).
 */
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
          modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp)
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
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
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
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
 * Gestisce il gesto di trascinamento (Swipe a destra o a sinistra) con fisica a molla.
 */
@Composable
fun SwipeableMovieCard(
  movie: Movie,
  onSwipedRight: () -> Unit,
  onSwipedLeft: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenEpisodes: () -> Unit = {}
) {
  val scope = rememberCoroutineScope()
  val offsetX = remember(movie.id) { Animatable(0f) }
  val swipeThreshold = 260f

  Box(
    modifier = modifier
      .offset { IntOffset(offsetX.value.roundToInt(), 0) }
      .rotate(offsetX.value / 28f)
      .pointerInput(movie.id) {
        detectDragGestures(
          onDragEnd = {
            scope.launch {
              when {
                offsetX.value > swipeThreshold -> {
                  offsetX.animateTo(1200f, tween(220))
                  onSwipedRight()
                }
                offsetX.value < -swipeThreshold -> {
                  offsetX.animateTo(-1200f, tween(220))
                  onSwipedLeft()
                }
                else -> {
                  offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                      dampingRatio = Spring.DampingRatioMediumBouncy,
                      stiffness = Spring.StiffnessLow
                    )
                  )
                }
              }
            }
          },
          onDragCancel = {
            scope.launch { offsetX.animateTo(0f, tween(200)) }
          },
          onDrag = { change, dragAmount ->
            change.consume()
            scope.launch {
              offsetX.snapTo(offsetX.value + dragAmount.x)
            }
          }
        )
      }
  ) {
    MovieCardContent(
      movie = movie,
      dragOffsetX = offsetX.value,
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
      // Sfondo decorativo + Locandina ufficiale IMDb / MyAnimeList caricata con Coil
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

      // Sfumatura scura dal basso per rendere sempre leggibili titolo, voto e trama
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.0f to Color.Black.copy(alpha = 0.30f),
              0.35f to Color.Transparent,
              0.60f to Color.Black.copy(alpha = 0.85f),
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
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
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

      // Timbro visivo "DA VEDERE ❤️" o "SALTA ✖" durante il trascinamento
      val swipeProgress = (abs(dragOffsetX) / 220f).coerceIn(0f, 1f)
      if (dragOffsetX > 40f) {
        Surface(
          color = LikeGreen.copy(alpha = 0.2f * swipeProgress),
          border = BorderStroke(3.dp, LikeGreen.copy(alpha = swipeProgress)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = 72.dp, start = 24.dp)
            .rotate(-12f)
        ) {
          Text(
            text = "DA VEDERE ❤️",
            color = LikeGreen.copy(alpha = swipeProgress),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }
      } else if (dragOffsetX < -40f) {
        Surface(
          color = SkipRed.copy(alpha = 0.2f * swipeProgress),
          border = BorderStroke(3.dp, SkipRed.copy(alpha = swipeProgress)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 72.dp, end = 24.dp)
            .rotate(12f)
        ) {
          Text(
            text = "SALTA ✖",
            color = SkipRed.copy(alpha = swipeProgress),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }
      }

      // Dettagli del titolo nella parte inferiore della scheda
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text(
          text = movie.title,
          color = TextPrimary,
          fontSize = 24.sp,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "${movie.year}  •  ${movie.formattedRuntime}  •  ${movie.director}",
          color = if (isMal) MalBlueLight else ImdbGold,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pillole dei generi + Pulsante rapido "Episodi & Ratings" per Serie TV e Anime
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
            movie.genres.take(3).forEach { genre ->
              Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = genre,
                  color = TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }
          }

          if (movie.isEpisodic) {
            Surface(
              color = ImdbGold.copy(alpha = 0.18f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, ImdbGold.copy(alpha = 0.7f)),
              modifier = Modifier
                .padding(start = 8.dp)
                .clickable { onOpenEpisodes() }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                  contentDescription = null,
                  tint = ImdbGold,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Episodi ⭐",
                  color = ImdbGold,
                  fontSize = 12.sp,
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
          lineHeight = 19.sp,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun ActionButtonsRow(
  remainingCount: Int,
  onSkip: () -> Unit,
  onMarkWatched: () -> Unit,
  onLike: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Bottone SALTA (Sinistra)
    FilledIconButton(
      onClick = onSkip,
      modifier = Modifier.size(58.dp),
      shape = CircleShape,
      colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = CinemaCardSurface,
        contentColor = SkipRed
      )
    ) {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Salta questo titolo",
        modifier = Modifier.size(28.dp)
      )
    }

    // Bottone centrale rapido "Già Visto ✅" + contatore mazzo
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      OutlinedButton(
        onClick = onMarkWatched,
        border = BorderStroke(1.dp, ImdbGold.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(50)
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = ImdbGold,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Già visto", color = ImdbGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
      Text(
        text = "$remainingCount nel mazzo",
        color = TextSecondary,
        fontSize = 11.sp
      )
    }

    // Bottone DA VEDERE / MI ISPIRA (Destra)
    FilledIconButton(
      onClick = onLike,
      modifier = Modifier.size(58.dp),
      shape = CircleShape,
      colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = LikeGreen,
        contentColor = Color.Black
      )
    ) {
      Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = "Aggiungi a Da Vedere!",
        modifier = Modifier.size(26.dp)
      )
    }
  }
}

/**
 * Sezione 2: DA VEDERE (Watchlist).
 * Mostra tutti i Film, Serie TV e Anime che l'utente vuole guardare, con filtri per categoria,
 * accesso ai Ratings degli Episodi e pulsante per segnarli come "Già Visti".
 */
@Composable
private fun WatchlistSection(
  watchlist: List<Movie>,
  userProgress: Map<String, UserMediaProgress>,
  onChooseForTonight: (Movie) -> Unit,
  onMarkWatched: (Movie) -> Unit,
  onOpenEpisodes: (Movie) -> Unit,
  onRemoveFromWatchlist: (Movie) -> Unit,
  onGoToDiscover: () -> Unit
) {
  var filterType by remember { mutableStateOf<MediaType?>(null) }
  val filteredList = remember(watchlist, filterType) {
    if (filterType == null) watchlist else watchlist.filter { it.mediaType == filterType }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Filtri rapidi per categoria (Tutti / Film / Serie TV / Anime)
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 8.dp)
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
            text = "La tua lista 'Da Vedere' è vuota",
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Scorri le schede a destra nella sezione Scopri o importa la tua lista da MyAnimeList!",
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
        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
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
              .clickable { onChooseForTonight(movie) }
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Mini-locandina
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
 * Sezione 3: GIÀ VISTI & EPISODI (Watched + Voti Personali + Tracker Episodi + Sync MyAnimeList).
 */
@Composable
private fun WatchedSection(
  watchedMovies: List<Movie>,
  userProgress: Map<String, UserMediaProgress>,
  totalEpisodesWatched: Int,
  malUsername: String,
  isSyncingMal: Boolean,
  malSyncMessage: String?,
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
    // 1. Riepilogo Statistiche Utente + Pulsante MyAnimeList
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

          // Bottone per aprire il collegamento a MyAnimeList (MAL)
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

    // 2. Filtri per categoria (Tutti / Film / Serie TV / Anime)
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

    // 3. Lista dei titoli Già Visti (con Voto Personale ⭐ e progresso Episodi)
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
            text = "Premi 'Già visto' sulle schede, spunta gli episodi di una Serie TV / Anime oppure importa il tuo profilo MyAnimeList!",
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
          onRateMedia = { stars -> onRateMedia(movie.id, stars) },
          onOpenEpisodes = { onOpenEpisodes(movie) },
          onMoveToWatchlist = { onMoveToWatchlist(movie) },
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

/**
 * Scheda di un Film, Serie TV o Anime nella sezione "Già Visti":
 * permette di dare il proprio voto personale (1..10 ⭐), aprire il tracker degli episodi
 * o aprire direttamente la pagina su IMDb / MyAnimeList.
 */
@Composable
private fun WatchedMediaCard(
  movie: Movie,
  progress: UserMediaProgress,
  onRateMedia: (Int) -> Unit,
  onOpenEpisodes: () -> Unit,
  onMoveToWatchlist: () -> Unit,
  onRemove: () -> Unit
) {
  val context = LocalContext.current
  var showRatingPicker by remember { mutableStateOf(false) }
  val isMal = movie.source == MediaSource.MYANIMELIST
  val totalEpCount = if (movie.episodes.isNotEmpty()) movie.episodes.size else movie.totalEpisodes
  val watchedEpCount = progress.watchedEpisodeIds.size

  Surface(
    color = CinemaCardSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
    modifier = Modifier.fillMaxWidth()
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

          // Riga pulsanti: Voto personale ⭐ + Episodi & Ratings + Link esterno IMDb/MAL
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
                  "Il tuo voto: ⭐ ${progress.userRating}/10"
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
          IconButton(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.externalPageUrl))
              context.startActivity(intent)
            }
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = "Apri su ${movie.source.badgeText}",
              tint = if (isMal) MalBlueLight else ImdbGold,
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

      // Barra di avanzamento episodi per Serie TV e Anime
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

      // Selettore interattivo di voto personale (da 1 a 10 stelle ⭐)
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

/**
 * Barra numerata interattiva da 1 a 10 per assegnare un voto con un semplice tocco.
 */
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
 * Pannello Modale "Episodi & Ratings" per Serie TV e Anime:
 * - Mostra il voto medio ufficiale della community (IMDb / MyAnimeList) per ogni singolo episodio
 * - Permette di segnare ogni episodio come "Visto" (☑️)
 * - Permette di dare il proprio voto personale (1..10 ⭐) a ogni singolo episodio!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeTrackerSheet(
  media: Movie,
  isLoading: Boolean,
  progress: UserMediaProgress,
  onDismiss: () -> Unit,
  onToggleEpisodeWatched: (String) -> Unit,
  onRateEpisode: (String, Int) -> Unit,
  onRateWholeSeries: (Int) -> Unit
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
      // Intestazione Serie TV / Anime
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

            // Colore stile Heatmap per il voto ufficiale dell'episodio
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
                  // Spunta "Visto" (☑️)
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

                  // Codice + Titolo episodio
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

                  // Voto Ufficiale Community + Pulsante Tuo Voto
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

                // Selettore voto episodio (1..10) a comparsa
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
  onPickRandom: () -> Unit
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
      text = "Hai visto tutte le schede di questo mazzo!",
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      color = TextPrimary,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = if (likedCount > 0) {
        "Hai salvato $likedCount titoli in 'Da Vedere'. Lascia che il dado scelga il vincitore della serata!"
      } else {
        "Rimescola il mazzo o cambia categoria (Film / Serie TV / Anime) per scoprire nuovi titoli."
      },
      color = TextSecondary,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      OutlinedButton(
        onClick = onReshuffle,
        border = BorderStroke(1.dp, ImdbGold)
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, tint = ImdbGold)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Rimescola", color = ImdbGold)
      }
      if (likedCount > 0) {
        Button(
          onClick = onPickRandom,
          colors = ButtonDefaults.buttonColors(
            containerColor = ImdbGold,
            contentColor = Color.Black
          )
        ) {
          Icon(Icons.Default.Casino, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Estrai il vincitore!", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Dialog che annuncia il titolo estratto a sorte per la serata (Film, Serie TV o Anime).
 */
@Composable
fun WinnerMovieDialog(
  movie: Movie,
  onDismiss: () -> Unit,
  onPickAnother: () -> Unit,
  onMarkWatched: () -> Unit = {},
  onOpenEpisodes: () -> Unit = {}
) {
  val context = LocalContext.current
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
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.externalPageUrl))
            context.startActivity(intent)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isMal) MalBlue else ImdbGold,
            contentColor = if (isMal) Color.White else Color.Black
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isMal) "Apri scheda su MyAnimeList" else "Apri scheda su IMDb",
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

          if (movie.isEpisodic) {
            OutlinedButton(
              onClick = onOpenEpisodes,
              modifier = Modifier.weight(1f),
              border = BorderStroke(1.dp, ImdbGold)
            ) {
              Text("Episodi ⭐", color = ImdbGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onPickAnother,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
          ) {
            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pesca altro")
          }
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
          ) {
            Text("Chiudi")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortlistBottomSheet(
  likedMovies: List<Movie>,
  onDismiss: () -> Unit,
  onRemoveMovie: (Movie) -> Unit,
  onChooseMovie: (Movie) -> Unit,
  onPickRandomFromLikes: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = CinemaSurface,
    contentColor = TextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "I tuoi candidati Da Vedere ❤️",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp
          )
          Text(
            text = "${likedMovies.size} titoli selezionati",
            color = TextSecondary,
            fontSize = 13.sp
          )
        }
        if (likedMovies.size >= 2) {
          Button(
            onClick = onPickRandomFromLikes,
            colors = ButtonDefaults.buttonColors(
              containerColor = ImdbGold,
              contentColor = Color.Black
            )
          ) {
            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sorteggia!", fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (likedMovies.isEmpty()) {
        Text(
          text = "Non hai ancora salvato nessun titolo. Scorri le schede a destra o premi il cuore verde per aggiungerli qui!",
          color = TextSecondary,
          modifier = Modifier.padding(vertical = 24.dp)
        )
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 32.dp)
        ) {
          items(likedMovies, key = { it.id }) { movie ->
            Surface(
              color = CinemaCardSurface,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onChooseMovie(movie) }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = movie.title,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                  )
                  Text(
                    text = "${movie.year} • ${movie.formattedRuntime} • ⭐ ${movie.imdbRating} ${movie.source.badgeText}",
                    color = ImdbGold,
                    fontSize = 12.sp
                  )
                }
                IconButton(onClick = { onRemoveMovie(movie) }) {
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
