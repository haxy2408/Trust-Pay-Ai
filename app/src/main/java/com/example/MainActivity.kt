package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AuthScreen
import com.example.security.BiometricAuthManager
import com.example.ui.LoginScreen
import com.example.ui.RegisterScreen
import com.example.ui.TrustPayScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TrustPayViewModel

class MainActivity : FragmentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val biometricAuthManager = BiometricAuthManager(this)

    setContent {
      val viewModel: TrustPayViewModel = viewModel()
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          when (uiState.currentScreen) {
            AuthScreen.REGISTER -> {
              RegisterScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
              )
            }
            AuthScreen.LOGIN -> {
              LoginScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
              )
            }
            AuthScreen.DASHBOARD -> {
              TrustPayScreen(
                viewModel = viewModel,
                biometricManager = biometricAuthManager,
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Kept for test compatibility.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

