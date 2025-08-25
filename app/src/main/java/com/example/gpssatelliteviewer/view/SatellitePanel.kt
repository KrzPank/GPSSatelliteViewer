package com.example.gpssatelliteviewer.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.viewModel.SatelliteViewModel

@Composable
fun SatellitePanel(
    navController: NavController,
    viewModel: SatelliteViewModel = viewModel()
) {
    // zbieramy listę aktualnych danych z ViewModel (odświeżanych w gnssCallback)
    val satellites = viewModel.satelliteList.collectAsState()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(satellites.value) { satInfo ->
            Text(
                text = satInfo,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
