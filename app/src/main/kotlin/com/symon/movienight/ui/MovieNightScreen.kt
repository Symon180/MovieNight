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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.symon.movienight.data.CuratedImdbCatalog
import com.symon.movienight.model.Movie
import com.symon.movienight.model.MovieGenreFilter
import com.symon.movienight.ui.theme.CinemaBackground
import com.symon.movienight.ui.theme.CinemaCardSurface
import com.symon.movienight.ui.theme.CinemaSurface
import com.symon.movienight.ui.theme.ImdbGold
import com.symon.movienight.ui.theme.LikeGreen
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
 * Schermata principale collegata al ViewModel.
 */
@Composable
fun MovieNightRoute(
  viewModel: MovieNightViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  MovieNightScreen(
    uiState = uiState,
    onSelectGenre = viewModel::selectGenre,
    onSearchQueryChange = viewModel::searchMovies,
    onSwipeLike = viewModel::likeCurrentMovie,
    onSwipeSkip = viewModel::skipCurrentMovie,
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
  modifier: Modifier = Modifier
) {
  var showSearchBar by remember { mutableStateOf(false) }

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
              color = ImdbGold,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.padding(end = 10.dp)
            ) {
              Text(
                text = "IMDb",
                color = Color.Black,
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
                text = "Cosa guardiamo stasera?",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }
        },
        actions = {
          IconButton(onClick = { showSearchBar = !showSearchBar }) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Cerca su IMDb",
              tint = if (showSearchBar) ImdbGold else TextPrimary
            )
          }
          IconButton(
            onClick = { onToggleShortlist(true) },
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
                contentDescription = "Film che mi ispirano",
                tint = if (uiState.likedMovies.isNotEmpty()) LikeGreen else TextPrimary
              )
            }
          }
        }
      )
    },
    floatingActionButton = {
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
            "Scegli tra i ${uiState.likedMovies.size} salvati!"
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
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Barra di ricerca live IMDb (a comparsa)
      AnimatedVisibility(visible = showSearchBar) {
        OutlinedTextField(
          value = uiState.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Cerca titolo o attore su IMDb...") },
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
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
          .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        when {
          uiState.isLoading -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = ImdbGold)
              Spacer(modifier = Modifier.height(12.dp))
              Text("Caricamento catalogo IMDb...", color = TextSecondary)
            }
          }

          uiState.currentMovie != null -> {
            // Carta sottostante per dare profondità al mazzo
            uiState.nextMovieInDeck?.let { nextMovie ->
              MovieCardContent(
                movie = nextMovie,
                dragOffsetX = 0f,
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
              modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 84.dp)
            )

            // Pulsanti fisici "Salta" (✖️) e "Mi ispira" (❤️) in basso
            ActionButtonsRow(
              remainingCount = uiState.remainingInDeck,
              onSkip = onSwipeSkip,
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

  // Finestra modale celebrativa quando viene estratto il film della serata!
  uiState.pickedWinner?.let { winner ->
    WinnerMovieDialog(
      movie = winner,
      onDismiss = onDismissWinner,
      onPickAnother = onPickRandom
    )
  }

  // BottomSheet con la lista dei film salvati con "Mi ispira"
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
}

@Composable
private fun GenreFilterRow(
  selectedGenre: MovieGenreFilter,
  onSelectGenre: (MovieGenreFilter) -> Unit
) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  // Usiamo movie.id come chiave: quando cambia film, l'offset torna a 0!
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
      modifier = Modifier.fillMaxSize()
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieCardContent(
  movie: Movie,
  dragOffsetX: Float,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = CinemaCardSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Sfondo decorativo + Locandina ufficiale IMDb caricata con Coil
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
              0.0f to Color.Black.copy(alpha = 0.25f),
              0.38f to Color.Transparent,
              0.62f to Color.Black.copy(alpha = 0.82f),
              1.0f to Color(0xFF0A0C10)
            )
          )
      )

      // Etichetta Mood in alto a sinistra + Voto IMDb in alto a destra
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (movie.moodTag.isNotBlank()) {
          Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, ImdbGold.copy(alpha = 0.5f))
          ) {
            Text(
              text = movie.moodTag,
              color = ImdbGold,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        } else {
          Spacer(modifier = Modifier.width(1.dp))
        }

        Surface(
          color = ImdbGold,
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${movie.imdbRating}",
              color = Color.Black,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp
            )
            Text(
              text = "/10",
              color = Color.Black.copy(alpha = 0.7f),
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            )
          }
        }
      }

      // Timbro visivo "MI ISPIRA" o "SALTA" durante il trascinamento
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
            text = "MI ISPIRA ❤️",
            color = LikeGreen.copy(alpha = swipeProgress),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
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
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }
      }

      // Dettagli del film nella parte inferiore della scheda
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text(
          text = movie.title,
          color = TextPrimary,
          fontSize = 26.sp,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "${movie.year}  •  ${movie.formattedRuntime}  •  ${movie.director}",
          color = ImdbGold,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pillole dei generi
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          movie.genres.forEach { genre ->
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

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = movie.plot,
          color = TextSecondary,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          maxLines = 4,
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
      modifier = Modifier.size(60.dp),
      shape = CircleShape,
      colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = CinemaCardSurface,
        contentColor = SkipRed
      )
    ) {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Salta questo film",
        modifier = Modifier.size(30.dp)
      )
    }

    Text(
      text = "$remainingCount film nel mazzo",
      color = TextSecondary,
      fontSize = 12.sp
    )

    // Bottone MI ISPIRA (Destra)
    FilledIconButton(
      onClick = onLike,
      modifier = Modifier.size(60.dp),
      shape = CircleShape,
      colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = LikeGreen,
        contentColor = Color.Black
      )
    ) {
      Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = "Mi ispira!",
        modifier = Modifier.size(28.dp)
      )
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
        "Hai salvato $likedCount film che ti ispirano. Lascia che il dado scelga il vincitore della serata!"
      } else {
        "Rimescola il mazzo o cambia genere per scoprire altri film."
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
 * Dialog che annuncia il film estratto a sorte per la serata.
 */
@Composable
fun WinnerMovieDialog(
  movie: Movie,
  onDismiss: () -> Unit,
  onPickAnother: () -> Unit
) {
  val context = LocalContext.current

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = CinemaSurface),
      border = BorderStroke(2.dp, ImdbGold),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(20.dp)
      ) {
        Surface(
          color = ImdbGold,
          shape = RoundedCornerShape(50)
        ) {
          Text(
            text = "🍿 STASERA SI GUARDA!",
            color = Color.Black,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
          modifier = Modifier
            .height(210.dp)
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
          fontSize = 22.sp,
          fontWeight = FontWeight.ExtraBold,
          color = TextPrimary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "${movie.year} • ${movie.formattedRuntime} • ⭐ ${movie.imdbRating}/10 IMDb",
          color = ImdbGold,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = movie.plot,
          color = TextSecondary,
          fontSize = 13.sp,
          textAlign = TextAlign.Center,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.imdbPageUrl))
            context.startActivity(intent)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = ImdbGold,
            contentColor = Color.Black
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Apri scheda su IMDb", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

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
            Text("Pesca un altro")
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
            text = "I tuoi candidati per stasera ❤️",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp
          )
          Text(
            text = "${likedMovies.size} film selezionati",
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
          text = "Non hai ancora salvato nessun film. Scorri le schede a destra o premi il cuore verde per aggiungerli qui!",
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
                    text = "${movie.year} • ${movie.formattedRuntime} • ⭐ ${movie.imdbRating}",
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
        likedMovies = CuratedImdbCatalog.movies.take(2)
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
