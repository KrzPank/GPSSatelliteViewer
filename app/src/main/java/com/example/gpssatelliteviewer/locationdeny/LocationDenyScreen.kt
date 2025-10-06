package com.example.gpssatelliteviewer.locationdeny

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LocationDenyScreen(
    onRequestPermission: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.Companion.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            // Location icon
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Location",
                modifier = Modifier.Companion.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.Companion.height(24.dp))

            // Title
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Companion.Bold,
                textAlign = TextAlign.Companion.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.Companion.height(16.dp))

            // Description card
            Card(
                modifier = Modifier.Companion.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.Companion.padding(20.dp)
                ) {
                    Text(
                        text = "GNSS Monitor needs location access to:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Companion.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.Companion.height(12.dp))

                    val features = listOf(
                        "Display your current location on the 3D map",
                        "Show satellite positions relative to you",
                        "Provide accurate GNSS data and statistics"
                    )

                    features.forEach { feature ->
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.Companion.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.height(32.dp))

            // Grant permission button
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Grant Location Permission",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Companion.SemiBold
                )
            }

            Spacer(modifier = Modifier.Companion.height(12.dp))

            // Secondary information
            Text(
                text = "Your location data is only used within the app and never shared.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Companion.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}