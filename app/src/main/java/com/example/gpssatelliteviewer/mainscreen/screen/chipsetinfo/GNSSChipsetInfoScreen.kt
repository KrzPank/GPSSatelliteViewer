package com.example.gpssatelliteviewer.mainscreen.screen.chipsetinfo

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.data.GNSSHardwareInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GNSSChipsetInfoScreen(
    gnssHardwareInfo: GNSSHardwareInfo,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GNSSChipsetInfoCard(gnssHardwareInfo)
        }

        item {
            GNSSChipsetCapabilitiesCard(gnssHardwareInfo)
        }
    }
}