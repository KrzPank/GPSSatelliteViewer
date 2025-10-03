package com.example.gpssatelliteviewer.ui.screen

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import com.example.gpssatelliteviewer.data.NMEALocationData
import com.example.gpssatelliteviewer.scene3d.Scene3DParametersState
import com.example.gpssatelliteviewer.ui.component.Scene3DLoadingScreen
import com.example.gpssatelliteviewer.ui.component.menu.Scene3DParametersMenu
import com.example.gpssatelliteviewer.ui.component.menu.SatelliteFilterMenu
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.scene3d.Scene3D
import com.example.gpssatelliteviewer.ui.theme.DarkBackground
import com.example.gpssatelliteviewer.utils.HideSystemUI
import com.example.gpssatelliteviewer.utils.LockOrientationLandscape
import com.example.gpssatelliteviewer.ui.theme.DarkBackground
import com.example.gpssatelliteviewer.ui.theme.TextLabel
import com.example.gpssatelliteviewer.ui.theme.GreenPrimary
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun Satellite3DScreen(
    navController: NavController,
    gnssViewModel: GNSSViewModel,
    locationNMEA: NMEALocationData
) {
    HideSystemUI()
    LockOrientationLandscape()

    val satelliteList by gnssViewModel.satelliteList.collectAsState()

    val userLocation: Triple<Float, Float, Float> =
        Triple(
            CoordinateConverter.nmeaCoordinateToDecimal(
                locationNMEA.latitude, 
                locationNMEA.latHemisphere
            ).toFloat(),
            CoordinateConverter.nmeaCoordinateToDecimal(
                locationNMEA.longitude, 
                locationNMEA.lonHemisphere
            ).toFloat(),
            locationNMEA.altitude.toFloat()
        )

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val view = rememberView(engine)
    val parametersState = remember { Scene3DParametersState() }

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
    var showLocationMarker by remember { mutableStateOf(true) }
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

    val menuAnimationDuration = 300
    val sceneOffsetX by animateDpAsState(
        targetValue = if (scene.isMenuVisible()) totalMenuWidth else 0.dp,
    )

    LaunchedEffect(scene) {
        scene.initializeScene()

        while (!scene.isReady()) {
            delay(50)
        }

        // Smooth transition not wanted but i don't know other way
        delay(200)
        isSceneReady = true
    }
    
    // Handle location marker visibility changes
    LaunchedEffect(showLocationMarker) {
        if (isSceneReady) {
            scene.setLocationMarkerVisible(showLocationMarker)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground) // Use theme background
    ) {
        AnimatedVisibility(
            visible = !isSceneReady,
            exit = fadeOut(
                animationSpec = tween(500) // Smooth fade out
            )
        ) {
            Scene3DLoadingScreen(
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3D Scene with fade in animation
        AnimatedVisibility(
            visible = isSceneReady,
            enter = fadeIn(
                animationSpec = tween(menuAnimationDuration)
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

        // Left-side overlay menu with tabs - only show when scene is loaded
        if (isSceneReady) {
            AnimatedVisibility(
                visible = scene.isMenuVisible(),
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(menuAnimationDuration)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(menuAnimationDuration)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .requiredWidth(totalMenuWidth)
                        .background(DarkBackground) // Solid black background
                        .padding(
                            start = safeInsets.calculateLeftPadding(LayoutDirection.Ltr),
                            top = 0.dp,
                            end = 0.dp,
                            bottom = 0.dp
                        )
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = TextLabel,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = GreenPrimary)
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Satellites", color = TextLabel) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Scene Settings", color = TextLabel) }
                        )
                    }

                    when (selectedTab) {
                        // Satellites Tab Content
                        0 -> {
                            SatelliteFilterMenu(
                                satelliteList = satelliteList,
                                selectedConstellations = selectedConstellations,
                                onlyUsedInFix = onlyUsedInFix,
                                onOnlyUsedInFixChanged = { onlyUsedInFix = it },
                                showLocationMarker = showLocationMarker,
                                onShowLocationMarkerChanged = { showLocationMarker = it },
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )
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
            onDispose {
                scene.cleanup()
            }
        }
    }
}
