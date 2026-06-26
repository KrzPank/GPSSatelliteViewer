package com.example.gnssmap.mainscreen

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
import com.example.gnssmap.data.viewmodel.GNSSViewModel
import com.example.gnssmap.data.viewmodel.LocationViewModel
import com.example.gnssmap.data.viewmodel.NMEAViewModel
import com.example.gnssmap.mainscreen.screens.chipsetinfo.GNSSChipsetInfoScreen
import com.example.gnssmap.mainscreen.screens.livenmea.LiveNMEADataScreen
import com.example.gnssmap.mainscreen.screens.locationinfo.LocationInfoScreen
import com.example.gnssmap.utils.NavigationTopAppBar
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    gnssViewModel: GNSSViewModel,
    nmeaViewModel: NMEAViewModel,
    locationViewModel: LocationViewModel
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    val gnssChipsetInfo by gnssViewModel.gnssHardwareInfo.collectAsState()

    // Determine label based on current page
    val topAppBarLabel = when (pagerState.currentPage) {
        0 -> "GNSS Chipset Info"
        1 -> "Location Info"
        2 -> "Live NMEA Messages"
        else -> ""
    }

    Scaffold(
        topBar = {
            NavigationTopAppBar(
                label = topAppBarLabel,
                expandedMap = expandedMap,
                menuKey = "mainMenu",
                menuItems = listOf(
                    "Satellite 3D View" to { navController.navigate("Satellite3DScreen") },
                    "Satellite Info" to { navController.navigate("SatelliteInfoMainScreen") }
                )
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.Companion
                .padding(innerPadding),
            beyondViewportPageCount = 2 //3
        ) { page ->
            when (page) {
                0 -> GNSSChipsetInfoScreen(gnssChipsetInfo)
                1 -> LocationInfoScreen(gnssViewModel, nmeaViewModel, locationViewModel)
                2 -> LiveNMEADataScreen(nmeaViewModel)
            }
        }
    }
}
