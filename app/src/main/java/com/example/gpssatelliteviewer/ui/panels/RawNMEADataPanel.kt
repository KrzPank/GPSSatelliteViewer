package com.example.gpssatelliteviewer.ui.panels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.viewModel.GNSSViewModel

// GOLD NEVER FORGET
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

//

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun RawNMEADataPanel(
    navController: NavController,
    viewModel: GNSSViewModel
) {
    val NMEAMessage by viewModel.messagePack.collectAsState()

    val NMEAMessageType by viewModel.NMEAMessageType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Info", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Button(onClick = { navController.navigate("LocationInfoPanel") }) {
                Text("Panel lokalizacji")
            }

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
            ) {

                items(NMEAMessageType) { type ->
                    Text(
                        text = type
                    )
                }
            }
        }
    }
}