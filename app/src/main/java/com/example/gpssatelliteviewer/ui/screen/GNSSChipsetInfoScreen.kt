package com.example.gpssatelliteviewer.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.data.GnssHardwareInfo
import com.example.gpssatelliteviewer.ui.component.card.GNSSChipsetCapabilitiesCard
import com.example.gpssatelliteviewer.ui.component.card.GNSSChipsetInfoCard
import com.example.gpssatelliteviewer.ui.theme.DarkBackground

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GNSSChipsetInfoScreen(
    gnssHardwareInfo: GnssHardwareInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GNSSChipsetInfoCard(gnssHardwareInfo)
        GNSSChipsetCapabilitiesCard(gnssHardwareInfo)
    }
}