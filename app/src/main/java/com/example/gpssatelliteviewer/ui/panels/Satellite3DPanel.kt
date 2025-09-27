package com.example.gpssatelliteviewer.ui.panels

import android.content.pm.ActivityInfo
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.viewModel.GNSSViewModel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.LaunchedEffect
// Removed LaunchedEffect import
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import com.example.gpssatelliteviewer.data.NMEALocationData
import com.example.gpssatelliteviewer.data.Scene3DParametersState
import com.example.gpssatelliteviewer.ui.components.Scene3DLoadingScreen
import com.example.gpssatelliteviewer.ui.components.Scene3DParametersMenu
import com.example.gpssatelliteviewer.utils.CoordinateConversion
import com.example.gpssatelliteviewer.utils.HideSystemUI
import com.example.gpssatelliteviewer.scene3d.Scene3D
import com.example.gpssatelliteviewer.utils.findActivity
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun Satellite3DPanel(
    navController: NavController,
    gnssViewModel: GNSSViewModel,
    locationNMEA: NMEALocationData
) {
    HideSystemUI()
    val context = LocalContext.current
    val activity = context.findActivity()

    val satelliteList by gnssViewModel.satelliteList.collectAsState()

    val userLocation: Triple<Float, Float, Float> =
        Triple(
            CoordinateConversion.nmeaCoordinateToDecimal(
                locationNMEA.latitude, 
                locationNMEA.latHemisphere
            ).toFloat(),
            CoordinateConversion.nmeaCoordinateToDecimal(
                locationNMEA.longitude, 
                locationNMEA.lonHemisphere
            ).toFloat(),
            locationNMEA.altitude.toFloat()
        )

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val view = rememberView(engine)
    
    // Scene3D parameters state
    val parametersState = remember { Scene3DParametersState() }
    
    // Loading state for Scene3D
    var isSceneReady by remember { mutableStateOf(false) }
    
    val scene = remember {
        Scene3D(
            modifier = Modifier.fillMaxSize(),
            environmentLoader = environmentLoader,
            modelLoader = modelLoader,
            engine = engine,
            view = view,
            parameters = parametersState.parameters
        )
    }

    val selectedConstellations = remember { mutableStateListOf<String>() }
    var onlyUsedInFix by remember { mutableStateOf(false) }
    val firstView = remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    if (firstView.value) {
        selectedConstellations.addAll(satelliteList.map { it.constellation }.distinct())
        firstView.value = false
    }

    // Apply filters to satelliteList
    val filteredSatellites = satelliteList.filter { sat ->
        selectedConstellations.contains(sat.constellation) && (!onlyUsedInFix || sat.usedInFix)
    }

    val menuWidth = 300.dp
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val totalMenuWidth = menuWidth + safeInsets.calculateLeftPadding(LayoutDirection.Ltr)

    val sceneOffsetX by animateDpAsState(
        targetValue = if (scene.isMenuVisible()) totalMenuWidth else 0.dp,
        animationSpec = tween(300) // Match menu animation timing
    )

    LaunchedEffect(scene) {
        scene.initializeScene()

        while (!scene.isReady()) {
            delay(100)
        }

        delay(200)
        isSceneReady = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Prevent white background during animation
    ) {
        AnimatedVisibility(
            visible = !isSceneReady,
            exit = fadeOut(
                animationSpec = tween(500) // Smooth fade out
            )
        ) {
            scene.initializeScene()
            Scene3DLoadingScreen(
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3D Scene with fade in animation
        AnimatedVisibility(
            visible = isSceneReady,
            enter = fadeIn(
                animationSpec = tween(300) // Smooth fade in over 300ms
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = sceneOffsetX/2)
            ) {
                scene.Render()
                scene.updateScene(filteredSatellites, userLocation)
            }
        }

        val scrollState = rememberScrollState()
        // Left-side overlay menu with tabs - only show when scene is loaded
        if (isSceneReady) {
            AnimatedVisibility(
                visible = scene.isMenuVisible(),
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .requiredWidth(totalMenuWidth)
                        .background(Color(0xFF000000)) // Solid black background
                        .padding(safeInsets)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color.Green
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Satellites", color = Color.White) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Scene 3D", color = Color.White) }
                        )
                    }

                    when (selectedTab) {
                        // Satellites Tab Content
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .absolutePadding(0.dp, 10.dp, 10.dp, 15.dp)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Spacer(Modifier.height(2.dp))
                                Text("Double tap to open/close menu", color = Color.White)
                                Button(
                                    onClick = { navController.navigate("LocationInfoPanel") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Location Panel")
                                }
                                Spacer(Modifier.height(5.dp))
                                Text("Constellations", color = Color.White)

                                val allConstellations =
                                    satelliteList.map { it.constellation }.distinct()

                                // Select All / Deselect All row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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

                                // Individual constellation checkboxes
                                allConstellations.forEach { constellation ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                    ) {
                                        Checkbox(
                                            checked = selectedConstellations.contains(constellation),
                                            onCheckedChange = { checked ->
                                                if (checked) selectedConstellations.add(
                                                    constellation
                                                )
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
                        // Scene3D Parameters Tab Content
                        1 -> {
                            Scene3DParametersMenu(
                                parametersState = parametersState,
                                onParametersChanged = { newParams ->
                                    scene.updateParameters(newParams)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            onDispose {
                scene.cleanup()
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
        }
    }
}
