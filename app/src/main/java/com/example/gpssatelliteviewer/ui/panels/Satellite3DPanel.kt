package com.example.gpssatelliteviewer.ui.panels

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.viewModel.GNSSViewModel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.gpssatelliteviewer.utils.HideSystemUI
import com.example.gpssatelliteviewer.utils.Scene3D
import com.example.gpssatelliteviewer.utils.SetOrientation
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader


//import com.example.gpssatelliteviewer.viewModel.Satellite3DViewModel


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun Satellite3DPanel(
    navController: NavController,
    viewModel: GNSSViewModel
) {
    HideSystemUI()

    val satelliteList by viewModel.satelliteList.collectAsState()
    val locationNMEA by viewModel.locationNMEA.collectAsState()

    val userLocation: Triple<Float, Float, Float> =
        Triple(
            locationNMEA.latitude.toFloat(),
            locationNMEA.longitude.toFloat(),
            locationNMEA.altitude.toFloat()
        )

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val scene = remember {
        Scene3D(
            modifier = Modifier.fillMaxSize(),
            environmentLoader = environmentLoader,
            modelLoader = modelLoader,
            engine = engine
        )
    }

    val selectedConstellations = remember { mutableStateListOf<String>() }
    var onlyUsedInFix by remember { mutableStateOf(false) }
    val firstView = remember { mutableStateOf(true) }

    if (firstView.value) {
        selectedConstellations.addAll(satelliteList.map { it.constellation }.distinct())
        firstView.value = false
    }

    // Apply filters to satelliteList
    val filteredSatellites = satelliteList.filter { sat ->
        selectedConstellations.contains(sat.constellation) && (!onlyUsedInFix || sat.usedInFix)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        scene.Render()
        scene.updateSatellites(filteredSatellites, userLocation)

        DisposableEffect(Unit) {
            onDispose { scene.cleanup() }
        }

        val scrollState = rememberScrollState()
        // Left-side overlay menu
        AnimatedVisibility(
            visible = scene.menuVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(Color(0xAA000000)) // semi-transparent black
                    .padding(13.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("Double tap to open/close menu", color = Color.White)
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { navController.navigate("LocationInfoPanel") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Location Panel")
                }

                // Constellation filters
                Text("Constellations", color = Color.White)

                val allConstellations = satelliteList.map { it.constellation }.distinct()

                // Select All / Deselect All row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            selectedConstellations.addAll(allConstellations)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select All")
                    }
                    Button(
                        onClick = { selectedConstellations.clear() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deselect All")
                    }
                }

                //Spacer(Modifier.height(4.dp))

                // Individual constellation checkboxes
                allConstellations.forEach { constellation ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                    ) {
                        Checkbox(
                            checked = selectedConstellations.contains(constellation),
                            onCheckedChange = { checked ->
                                if (checked) selectedConstellations.add(constellation)
                                else selectedConstellations.remove(constellation)
                            },
                            colors = CheckboxDefaults.colors(
                                checkmarkColor = Color.White,
                                uncheckedColor = Color.White,
                                checkedColor = Color.Green
                            )
                        )
                        Text(constellation, color = Color.White)
                    }
                }

                // Only show satellites used in fix
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = onlyUsedInFix,
                        onCheckedChange = { onlyUsedInFix = it },
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color.White,
                            uncheckedColor = Color.White,
                            checkedColor = Color.Green
                        )
                    )
                    Text("Only in fix", color = Color.White)
                }
            }
        }
    }
}


/* *** INFORMATION CHECK WORKS ***
Text(
    text = gnssCapabilities.toString(),
    modifier = Modifier.Companion.padding(8.dp)
)
if (navMessage != null) {
    Text(
        text = navMessage.toString(),
        modifier = Modifier.Companion.padding((8.dp))
    )
} else {
    Text(
        text = "Waiting for navigation messages...",
        modifier = Modifier.Companion.padding(8.dp)
    )
}
*/