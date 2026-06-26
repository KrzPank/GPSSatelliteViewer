package com.example.gnssmap.app

import android.Manifest
import android.app.Activity
import android.content.Intent
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
import com.example.gnssmap.app.theme.GNSSMapTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.gnssmap.utils.SetupDarkSystemUI
import android.net.Uri
import android.provider.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplashVisible = true
        splashScreen.setKeepOnScreenCondition { keepSplashVisible }

        setContent {
            GNSSMapTheme {
                SetupDarkSystemUI()
                val context = LocalContext.current
                val activity = context as Activity

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

                val onPermissionRequestClick = {
                    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )

                    if (shouldShowRationale) {
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
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

                        true -> AppNavigation(hasPermission = true, onRequestClick = {})
                        false -> AppNavigation(
                            hasPermission = false,
                            onRequestClick = onPermissionRequestClick // Pass our smart logic down
                        )
                    }
                }
            }
        }
    }
}
