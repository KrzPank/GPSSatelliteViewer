package com.example.gpssatelliteviewer.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import com.example.gpssatelliteviewer.app.theme.GPSSatelliteViewerTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.gpssatelliteviewer.utils.SetupDarkSystemUI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplashVisible = true
        splashScreen.setKeepOnScreenCondition { keepSplashVisible }

        setContent {
            GPSSatelliteViewerTheme {
                SetupDarkSystemUI()
                val context = LocalContext.current

                // null = not checked yet, true/false = actual state
                var hasPermission by remember { mutableStateOf<Boolean?>(null) }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { granted -> hasPermission = granted }
                )

                LaunchedEffect(Unit) {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    hasPermission = granted

                    if (!granted) {
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }

                    keepSplashVisible = false
                }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (hasPermission) {
                        null -> {
                            Box(
                                modifier = Modifier.Companion.fillMaxSize(),
                                contentAlignment = Alignment.Companion.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        true -> AppNavigation(hasPermission = true, permissionLauncher = launcher)
                        false -> AppNavigation(hasPermission = false, permissionLauncher = launcher)
                    }
                }
            }
        }
    }
}
