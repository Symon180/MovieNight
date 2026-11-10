package com.symon.movienight

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.symon.movienight.ui.MovieNightRoute
import com.symon.movienight.ui.theme.MovieNightTheme

class MainActivity : ComponentActivity() {

  private var incomingOAuthUri by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleOAuthIntent(intent)

    enableEdgeToEdge()
    setContent {
      MovieNightTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MovieNightRoute(
            incomingOAuthUri = incomingOAuthUri,
            onConsumeOAuthUri = { incomingOAuthUri = null }
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleOAuthIntent(intent)
  }

  private fun handleOAuthIntent(intent: Intent?) {
    val data = intent?.data ?: return
    if (data.scheme == "movienight" && data.host == "mal-oauth") {
      incomingOAuthUri = data.toString()
    }
  }
}
