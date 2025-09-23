package com.example.gpssatelliteviewer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Scene3DLoadingScreen(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    // Pulsing animation for the title
    val titleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titlePulse"
    )
    
    // Floating animation for satellite dots
    val satelliteOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "satelliteFloat"
    )
    
    // Rotation animation for orbital rings
    val orbitalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "orbitalRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title with pulsing animation
            Text(
                text = "GNSS Monitor",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer(alpha = titleAlpha)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animated satellite constellation representation
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Central Earth representation
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Blue, CircleShape)
                )
                
                // Orbital rings with rotation
                repeat(2) { ring ->
                    val ringSize = 60.dp + (ring * 30).dp
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(ringSize)
                            .graphicsLayer(rotationZ = orbitalRotation + (ring * 45f)),
                        color = Color.Green.copy(alpha = 0.3f),
                        strokeWidth = 2.dp,
                        progress = 0.75f
                    )
                }
                
                // Floating satellite dots
                repeat(4) { index ->
                    val angle = (index * 90f) + orbitalRotation
                    val radius = 45f
                    val x = radius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                    val y = radius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() + satelliteOffset * kotlin.math.sin(Math.toRadians((index * 45f).toDouble())).toFloat()
                    
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer {
                                translationX = x
                                translationY = y
                            }
                            .background(Color.Green, CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main progress indicator
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color.Green,
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Loading text with animated dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Initializing 3D Scene",
                    color = Color.White,
                    fontSize = 18.sp
                )
                
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = ".",
                        color = Color.White.copy(alpha = dotAlpha),
                        fontSize = 18.sp,
                        modifier = Modifier.graphicsLayer {
                            // Stagger the animation timing for each dot
                            val delay = index * 200L
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Subtitle with helpful info
            Text(
                text = "Loading satellites and Earth models...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}
