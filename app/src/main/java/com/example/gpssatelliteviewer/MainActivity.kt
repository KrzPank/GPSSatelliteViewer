package com.example.gpssatelliteviewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.gpssatelliteviewer.ui.panels.LocationDeny
import com.example.gpssatelliteviewer.ui.panels.LocationInfoPanel
import com.example.gpssatelliteviewer.ui.panels.Satellite3DPanel


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AppNavigation()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (hasPermission) {
        NavHost(
            navController = navController,
            startDestination = "LocationInfoPanel"
        ) {
            composable("LocationInfoPanel") {
                LocationInfoPanel(navController)
            }
            composable("Satellite3DPanel") {
                Satellite3DPanel(navController)
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
