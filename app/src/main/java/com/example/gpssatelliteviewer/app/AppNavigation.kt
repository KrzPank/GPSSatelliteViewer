package com.example.gpssatelliteviewer.app

import android.Manifest
import android.app.Application
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.data.viewmodel.LocationViewModel
import com.example.gpssatelliteviewer.data.viewmodel.NMEAViewModel
import com.example.gpssatelliteviewer.locationdeny.LocationDenyScreen
import com.example.gpssatelliteviewer.mainscreen.MainScreen
import com.example.gpssatelliteviewer.scene3d.ui.Satellite3DScreen
import com.example.gpssatelliteviewer.statisticscreen.MainStatisticsScreen

@Composable
fun AppNavigation(
    hasPermission: Boolean,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    val navController = rememberNavController()

    if (hasPermission) {
        val context = LocalContext.current
        val app = context.applicationContext as Application

        val gnssViewModel: GNSSViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )
        val nmeaViewModel: NMEAViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )
        val locationViewModel: LocationViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )

        gnssViewModel.startGNSSInfo()
        gnssViewModel.loadGNSSHardwareInfo()
        nmeaViewModel.startNMEAInfo()
        locationViewModel.startLocationListenerInfo()

        NavHost(
            navController = navController,
            startDestination = "MainScreen"
        ) {
            composable("MainScreen") {
                MainScreen(
                    navController = navController,
                    gnssViewModel =  gnssViewModel,
                    nmeaViewModel = nmeaViewModel,
                    locationViewModel = locationViewModel
                )
            }
            composable("Satellite3DScreen") {
                val locationNMEA by nmeaViewModel.locationNMEA.collectAsState()
                Satellite3DScreen(
                    navController = navController,
                    gnssViewModel = gnssViewModel,
                    locationNMEA = locationNMEA,
                    locationViewModel = locationViewModel
                )
            }
            composable("MainStatisticsScreen") {
                val nmeaMessageStatistics by nmeaViewModel.messageStatistics.collectAsState()
                MainStatisticsScreen(
                    navController = navController,
                    gnssViewModel = gnssViewModel,
                    nmeaMessageStatistics = nmeaMessageStatistics
                )
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = "LocationDenyScreen"
        ) {
            composable("LocationDenyScreen") {
                LocationDenyScreen(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )
            }
        }
    }
}
