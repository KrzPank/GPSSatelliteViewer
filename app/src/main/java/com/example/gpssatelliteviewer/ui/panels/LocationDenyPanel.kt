package com.example.gpssatelliteviewer.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun LocationDeny(
    navController: NavController
) {
    Surface(
        modifier = Modifier.Companion.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.Companion.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            Text(
                "Ta aplikacja potrzebuje dostępu do lokalizacji.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}