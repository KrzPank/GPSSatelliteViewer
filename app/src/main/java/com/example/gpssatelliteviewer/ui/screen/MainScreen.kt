package com.example.gpssatelliteviewer.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.data.viewmodel.LocationViewModel
import com.example.gpssatelliteviewer.data.viewmodel.NMEAViewModel

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun MainScreen(
    navController: NavController,
    gnssViewModel: GNSSViewModel,
    nmeaViewModel: NMEAViewModel,
    locationViewModel: LocationViewModel
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 2
    ) { page ->
        when (page) {
            0 -> LocationInfoScreen(navController, gnssViewModel, nmeaViewModel, locationViewModel)
            1 -> SatelliteInfoScreen(navController, gnssViewModel)
            2 -> LiveNMEADataScreen(navController, nmeaViewModel)
        }
    }
}
