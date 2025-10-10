package com.example.gpssatelliteviewer.scene3d.ui.infobox

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.app.theme.CardBackground
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import dev.romainguy.kotlin.math.Float3

@Composable
fun EarthInfoBox(
    userLocation: Float3?,
    isMenuVisible: Boolean,
    safeInsets: PaddingValues,
    totalMenuWidth: Dp,
    modifier: Modifier = Modifier
) {
    val sceneOffsetX by animateDpAsState(
        targetValue = if (isMenuVisible) totalMenuWidth else safeInsets.calculateLeftPadding(
            LayoutDirection.Ltr
        ) * 2,
    )
    userLocation?.let { sat ->
        Box(
            modifier = modifier
                .padding(top = 50.dp)
                .offset(x = sceneOffsetX / 2)
        ) {
            Card(
                modifier = modifier.padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier.padding(8.dp)) {
                    Text("Earth", fontWeight = FontWeight.Bold)
                    Text("Current location: ")
                    val latHem = if (userLocation.x >= 0) 'N' else 'S'
                    val lonHem = if (userLocation.y >= 0) 'E' else 'W'

                    Text(
                        "Latitude: ${
                            CoordinateConverter.decimalToDMS(
                                userLocation.x.toDouble(),
                                latHem
                            )
                        }"
                    )
                    Text(
                        "Longitude: ${
                            CoordinateConverter.decimalToDMS(
                                userLocation.y.toDouble(),
                                lonHem
                            )
                        }"
                    )
                    Text("Altitude: ${userLocation.z.let { "%.1f m".format(it) }}")
                }
            }
        }
    }
}