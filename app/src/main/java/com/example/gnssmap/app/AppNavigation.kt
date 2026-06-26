package com.example.gnssmap.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gnssmap.data.viewmodel.GNSSViewModel
import com.example.gnssmap.data.viewmodel.LocationViewModel
import com.example.gnssmap.data.viewmodel.NMEAViewModel
import com.example.gnssmap.locationdeny.LocationDenyScreen
import com.example.gnssmap.mainscreen.MainScreen
import com.example.gnssmap.scene3d.ui.Satellite3DScreen
import com.example.gnssmap.satellitescreen.SatelliteInfoMainScreen

@Composable
fun AppNavigation(
    hasPermission: Boolean,
    onRequestClick: () -> Unit
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
            startDestination = "LocationMainScreen"
        ) {
            composable("LocationMainScreen") {
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
            composable("SatelliteInfoMainScreen") {
                SatelliteInfoMainScreen(
                    navController = navController,
                    gnssViewModel = gnssViewModel
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
                    onRequestPermission = onRequestClick
                )
            }
        }
    }
}
