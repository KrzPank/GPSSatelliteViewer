package com.example.gpssatelliteviewer.mainscreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.data.viewmodel.LocationViewModel
import com.example.gpssatelliteviewer.data.viewmodel.NMEAViewModel
import com.example.gpssatelliteviewer.mainscreen.screen.chipsetinfo.GNSSChipsetInfoScreen
import com.example.gpssatelliteviewer.mainscreen.screen.livenmea.LiveNMEADataScreen
import com.example.gpssatelliteviewer.mainscreen.screen.locationinfo.LocationInfoScreen
import com.example.gpssatelliteviewer.mainscreen.screen.satelliteinfo.SatelliteInfoScreen
import com.example.gpssatelliteviewer.utils.NavigationTopAppBar
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    gnssViewModel: GNSSViewModel,
    nmeaViewModel: NMEAViewModel,
    locationViewModel: LocationViewModel
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 4 })
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    val gnssChipsetInfo by gnssViewModel.gnssHardwareInfo.collectAsState()

    // Determine label based on current page
    val topAppBarLabel = when (pagerState.currentPage) {
        0 -> "GNSS Chipset Info"
        1 -> "Location Info"
        2 -> "Satellite Info"
        3 -> "Live NMEA Messages"
        else -> ""
    }

    Scaffold(
        topBar = {
            NavigationTopAppBar(
                label = topAppBarLabel,
                //navController = navController,
                expandedMap = expandedMap,
                menuKey = "mainMenu",
                menuItems = listOf(
                    "Satellite 3D View" to { navController.navigate("Satellite3DScreen") },
                    "Statistics" to { navController.navigate("MainStatisticsScreen") }
                )
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.Companion
                .padding(innerPadding),
            beyondViewportPageCount = 3
        ) { page ->
            when (page) {
                0 -> GNSSChipsetInfoScreen(gnssChipsetInfo)
                1 -> LocationInfoScreen(gnssViewModel, nmeaViewModel, locationViewModel)
                2 -> SatelliteInfoScreen(gnssViewModel)
                3 -> LiveNMEADataScreen(nmeaViewModel)
            }
        }
    }
}