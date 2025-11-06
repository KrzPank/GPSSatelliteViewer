package com.example.gpssatelliteviewer.scene3d.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue

@Composable
fun Scene3DLoadingScreen(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Rotation animation for orbital rings
    val orbitalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing)
        ),
        label = "orbitalRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Companion.Black),
        contentAlignment = Alignment.Companion.Center
    ) {
        Column(
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title with pulsing animation
            Text(
                text = "GNSS Monitor",
                color = Color.Companion.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Companion.Bold
            )

            Spacer(modifier = Modifier.Companion.height(32.dp))

            // Animated satellite constellation representation
            Box(
                modifier = Modifier.Companion.size(120.dp),
                contentAlignment = Alignment.Companion.Center
            ) {
                // Central Earth representation
                Box(
                    modifier = Modifier.Companion
                        .size(24.dp)
                        .background(Color.Companion.Blue, CircleShape)
                )

                // Orbital rings with rotation
                repeat(2) { ring ->
                    val ringSize = 60.dp + (ring * 30).dp
                    CircularProgressIndicator(
                        progress = {
                            0.75f
                        },
                        modifier = Modifier.Companion
                            .size(ringSize)
                            .graphicsLayer(rotationZ = orbitalRotation + (ring * 45f)),
                        color = Color.Companion.Green.copy(alpha = 0.3f),
                        strokeWidth = 2.dp,
                        gapSize = 5.dp
                    )
                }
            }

            Spacer(modifier = Modifier.Companion.height(24.dp))

            // Main progress indicator
            CircularProgressIndicator(
                modifier = Modifier.Companion.size(48.dp),
                color = Color.Companion.Green,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.Companion.height(16.dp))

            // Loading text with animated dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Text(
                    text = "Initializing 3D Scene...",
                    color = Color.Companion.White,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.Companion.height(32.dp))
        }
    }
}
