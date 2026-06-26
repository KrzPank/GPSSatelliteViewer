package com.example.gnssmap.satellitescreen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.gnssmap.data.viewmodel.GNSSViewModel
import com.example.gnssmap.satellitescreen.satelliteInfo.SatelliteInfoScreen
import com.example.gnssmap.satellitescreen.constellationInfo.ConstellationInfoScreen
import com.example.gnssmap.utils.NavigationTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteInfoMainScreen(
    navController: NavController,
    gnssViewModel: GNSSViewModel
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    val topAppBarLabel = when (pagerState.currentPage) {
        0 -> "Satellite Info"
        1 -> "Constellation Info"
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
                    "Location Info" to { navController.navigate("LocationMainScreen") },
                )
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.Companion
                .padding(innerPadding),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> SatelliteInfoScreen(gnssViewModel = gnssViewModel)
                1 -> ConstellationInfoScreen(gnssViewModel = gnssViewModel)
            }
        }
    }
}
