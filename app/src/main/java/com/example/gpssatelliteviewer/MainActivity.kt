package com.example.gpssatelliteviewer

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gpssatelliteviewer.ui.panels.LiveNMEADataPanel

import com.example.gpssatelliteviewer.ui.panels.LocationDeny
import com.example.gpssatelliteviewer.ui.panels.LocationInfoPanel
import com.example.gpssatelliteviewer.ui.panels.Satellite3DPanel
import com.example.gpssatelliteviewer.utils.SetOrientation
import com.example.gpssatelliteviewer.viewModel.GNSSViewModel


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
            }

            MaterialTheme {
                Surface {
                    when (hasPermission) {
                        null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        true -> AppNavigation(true)
                        false -> AppNavigation(false)
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AppNavigation(hasPermission: Boolean) {
    val navController = rememberNavController()

    if (hasPermission) {
        val viewModel: GNSSViewModel = viewModel()
        NavHost(
            navController = navController,
            startDestination = "LocationInfoPanel"
        ) {
            composable("LocationInfoPanel") {
                LocationInfoPanel(navController, viewModel)
            }
            composable("Satellite3DPanel") {
                Satellite3DPanel(navController, viewModel)
            }
            composable("LiveNMEADataPanel") {
                LiveNMEADataPanel(navController, viewModel)
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = "LocationDeny"
        ) {
            composable("LocationDeny") {
                LocationDeny(navController)
            }
        }
    }
}
