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
import android.provider.Settings

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

            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false  // Light icons for dark background
            controller.isAppearanceLightNavigationBars = false   // Light icons for dark navigation
        }

        onDispose {
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }
}

@Composable
fun LockOrientationLandscape() {
    val context = LocalActivity.current
    val activity = LocalActivity.current as? ComponentActivity

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            val isAutoRotateOn = Settings.System.getInt(
                context?.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1

            if (isAutoRotateOn) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            else activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
