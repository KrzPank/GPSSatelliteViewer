package com.example.gpssatelliteviewer.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color
import android.os.Build
import androidx.core.graphics.toColorInt


@Composable
fun HideSystemUI() {
    val activity = LocalActivity.current as? ComponentActivity

    DisposableEffect(activity) {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }
}

@Composable
fun SetupDarkSystemUI() {
    val activity = LocalActivity.current as? ComponentActivity

    DisposableEffect(activity) {
        activity?.window?.let { window ->
            // Enable edge-to-edge content
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Set status bar and navigation bar colors to dark
            window.statusBarColor = "#1E1E1E".toColorInt() // Your dark background ? DarkBackground from colors
            window.navigationBarColor = "#1E1E1E".toColorInt()

            // Set light status bar icons (dark icons on light background = false, light icons on dark background = true)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false  // Light icons for dark background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                controller.isAppearanceLightNavigationBars = false  // Light icons for dark navigation
            }
        }

        onDispose {
            activity?.window?.let { window ->
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = Color.TRANSPARENT
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    controller.isAppearanceLightNavigationBars = true
                }
            }
        }
    }
}

@Composable
fun LockOrientationLandscape() {
    val activity = LocalActivity.current as? ComponentActivity

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
