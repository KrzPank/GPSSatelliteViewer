package com.example.gpssatelliteviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Scene3DLoadingScreen(
    progress: Float,
    modifier: Modifier = Modifier
) {
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
            // App Title
            Text(
                text = "GNSS Monitor",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtitle
            Text(
                text = "Initializing 3D Scene",
                color = Color.Gray,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Progress indicator
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(80.dp),
                color = Color.Green,
                strokeWidth = 6.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress text
            Text(
                text = getLoadingMessage(progress),
                color = Color.White,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress percentage
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color.Green,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getLoadingMessage(progress: Float): String {
    return when {
        progress < 0.2f -> "Starting up..."
        progress < 0.4f -> "Initializing engine..."
        progress < 0.6f -> "Loading models..."
        progress < 0.8f -> "Setting up environment..."
        progress < 1.0f -> "Finalizing scene..."
        else -> "Ready!"
    }
}