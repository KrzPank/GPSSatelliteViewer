package com.example.gpssatelliteviewer.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


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
fun SetOrientation(
    orientation: Int
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(Unit) {
        activity?.requestedOrientation = orientation

        onDispose {
            // Reset to sensor orientation when leaving the screen
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}