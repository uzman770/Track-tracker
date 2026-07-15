package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AdminHistoryScreen
import com.example.ui.screens.AdminHomeScreen
import com.example.ui.screens.NotificationTrayScreen
import com.example.ui.screens.ResidentHomeScreen
import com.example.ui.screens.ResidentOnboardingScreen
import com.example.ui.screens.ResidentSettingsScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { _ -> }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Dynamic permissions check & request on Tiramisu+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          this,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    setContent {
      MyApplicationTheme {
        val viewModel: AppViewModel = viewModel()
        val currentScreen by viewModel.currentScreen.collectAsState()

        // Integrate native physical system back button click
        BackHandler(enabled = currentScreen != AppScreen.RoleSelection) {
          viewModel.navigateBack()
        }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          Crossfade(
            targetState = currentScreen,
            label = "ScreenTransition"
          ) { screen ->
            when (screen) {
              is AppScreen.RoleSelection -> {
                RoleSelectionScreen(
                  onRoleSelected = { role, adminId, adminName ->
                    viewModel.selectRole(role, adminId, adminName)
                  }
                )
              }
              is AppScreen.ResidentOnboarding -> {
                ResidentOnboardingScreen(viewModel = viewModel)
              }
              is AppScreen.ResidentHome -> {
                ResidentHomeScreen(viewModel = viewModel)
              }
              is AppScreen.ResidentSettings -> {
                ResidentSettingsScreen(viewModel = viewModel)
              }
              is AppScreen.NotificationTray -> {
                NotificationTrayScreen(viewModel = viewModel)
              }
              is AppScreen.AdminHome -> {
                AdminHomeScreen(viewModel = viewModel)
              }
              is AppScreen.AdminHistory -> {
                AdminHistoryScreen(viewModel = viewModel, areaId = screen.areaId)
              }
            }
          }
        }
      }
    }
  }
}
