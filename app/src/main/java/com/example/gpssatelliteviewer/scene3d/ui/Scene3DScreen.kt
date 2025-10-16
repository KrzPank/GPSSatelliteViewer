package com.example.gpssatelliteviewer.scene3d.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.data.NMEALocationData
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.scene3d.Scene3D
import com.example.gpssatelliteviewer.scene3d.Scene3DParametersState
import com.example.gpssatelliteviewer.scene3d.ui.menu.SatelliteFilterMenu
import com.example.gpssatelliteviewer.scene3d.ui.menu.Scene3DParametersMenu
import com.example.gpssatelliteviewer.app.theme.DarkBackground
import com.example.gpssatelliteviewer.app.theme.GreenPrimary
import com.example.gpssatelliteviewer.app.theme.TextLabel
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.data.viewmodel.LocationViewModel
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.scene3d.ui.infobox.EarthInfoBox
import com.example.gpssatelliteviewer.scene3d.ui.infobox.SatelliteInfoBox
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.HideSystemUI
import com.example.gpssatelliteviewer.utils.LockOrientationLandscape
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun Satellite3DScreen(
    navController: NavController,
    gnssViewModel: GNSSViewModel,
    locationNMEA: NMEALocationData,
    locationViewModel: LocationViewModel
) {
    // Immersive mode
    HideSystemUI()
    LockOrientationLandscape()

    val satelliteList by gnssViewModel.satelliteList.collectAsState()
    val locationAndroidApi by locationViewModel.locationAndroidApi.collectAsState()

    val userLocation: Float3? = when {
        (locationAndroidApi.latitude == 0.0 && locationAndroidApi.longitude == 0.0) &&
                (locationNMEA.latitude == 0.0 && locationNMEA.longitude == 0.0) -> null

        (locationAndroidApi.latitude == 0.0 && locationAndroidApi.longitude == 0.0) -> Float3(
            CoordinateConverter.nmeaCoordinateToDecimal(locationNMEA.latitude, locationNMEA.latHemisphere).toFloat(),
            CoordinateConverter.nmeaCoordinateToDecimal(locationNMEA.longitude, locationNMEA.lonHemisphere).toFloat(),
            locationNMEA.altitude.toFloat()
        )
        else -> Float3(
            locationAndroidApi.latitude.toFloat(),
            locationAndroidApi.longitude.toFloat(),
            locationAndroidApi.altitude.toFloat()
        )
    }

    // Satellite filtering
    val selectedConstellations = remember { mutableStateListOf<String>() }
    var onlyUsedInFix by remember { mutableStateOf(false) }
    val filteredSatellites = satelliteList.filter { sat ->
        selectedConstellations.contains(sat.constellation) && (!onlyUsedInFix || sat.usedInFix)
    }

    //  First view set constellation to be visible
    val firstView = remember { mutableStateOf(true) }
    if (firstView.value) {
        selectedConstellations.addAll(satelliteList.map { it.constellation }.distinct())
        firstView.value = false
    }

    //  menu stuff
    var selectedTab by remember { mutableIntStateOf(0) }
    val menuWidth = 300.dp
    val safeInsets = WindowInsets.Companion.safeDrawing.asPaddingValues()
    val totalMenuWidth = menuWidth + safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val menuAnimationDuration = 300

    // SceneView parameters init
    val engine = rememberEngine()
    val view = rememberView(engine)
    val renderer = rememberRenderer(engine)
    val coreScene = rememberScene(engine)
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val parametersState = remember { Scene3DParametersState().apply { updateLocation(userLocation) } }

    val scene = remember {
        Scene3D(
            engine = engine,
            view = view,
            renderer = renderer,
            scene = coreScene,
            modelLoader = modelLoader,
            environmentLoader = environmentLoader,
            parameters = parametersState.parameters,
            modifier = Modifier.fillMaxSize(),
        )
    }

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Smooth transition not wanted but i don't know other way
        var isSceneReady by remember { mutableStateOf(false) }
        LaunchedEffect(scene) {
            // mandatory 20ms delay ??any less and there are race conditions??
            delay(20)
            isSceneReady = true
        }

        AnimatedVisibility(
            visible = !isSceneReady,
            enter = fadeIn(
                animationSpec = tween(10)
            ),
            exit = fadeOut(
                animationSpec = tween(500)
            )
        ) {
            Scene3DLoadingScreen(
                modifier = Modifier.Companion.fillMaxSize()
            )
        }

        // Handle location marker visibility changes
        var showLocationMarker by remember { mutableStateOf(scene.isLocationMarkerVisible()) }
        LaunchedEffect(showLocationMarker, userLocation) {
            if (isSceneReady) {
                scene.setLocationMarkerVisible(showLocationMarker)
                scene.updateUserLocation(userLocation)
            }
        }

        // Handle satellite click
        val clickedSatelliteKey by scene.clickedSatelliteKeyState
        val clickedSatellite by remember(clickedSatelliteKey, satelliteList) {
            derivedStateOf {
                scene.resolveClickedSatelliteByKey(clickedSatelliteKey, satelliteList)
            }
        }

        AnimatedVisibility(
            visible = isSceneReady,
            enter = fadeIn(
                animationSpec = tween(menuAnimationDuration)
            )
        ) {
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .offset(x = if (scene.isMenuVisible()) totalMenuWidth / 2 else 0.dp)
            ) {
                scene.Render()
                scene.updateScene(filteredSatellites)

                AnimatedVisibility(
                    visible = scene.isSatelliteInfoBoxVisible(),
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(menuAnimationDuration/2)
                    ),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(menuAnimationDuration/2)
                    )
                ) {
                    SatelliteInfoBox(
                        clickedSatellite = clickedSatellite,
                        isMenuVisible = scene.isMenuVisible(),
                        safeInsets = safeInsets,
                        totalMenuWidth = totalMenuWidth
                    )
                }

                AnimatedVisibility(
                    visible = scene.isEarthInfoBoxVisible(),
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(menuAnimationDuration/2)
                    ),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(menuAnimationDuration/2)
                    )
                ) {
                    EarthInfoBox(
                        userLocation = userLocation,
                        nmea = locationNMEA,
                        isMenuVisible = scene.isMenuVisible(),
                        safeInsets = safeInsets,
                        totalMenuWidth = totalMenuWidth
                    )
                }
            }
        }

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
                    modifier = Modifier.Companion
                        .fillMaxHeight()
                        .requiredWidth(totalMenuWidth)
                        .background(DarkBackground)
                        .padding(
                            start = safeInsets.calculateLeftPadding(LayoutDirection.Ltr),
                            top = 0.dp,
                            end = 0.dp,
                            bottom = 0.dp
                        )
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Companion.Transparent,
                        contentColor = TextLabel,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.Companion.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = GreenPrimary
                            )
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
                        0 -> {
                            SatelliteFilterMenu(
                                satelliteList = satelliteList,
                                selectedConstellations = selectedConstellations,
                                onlyUsedInFix = onlyUsedInFix,
                                onOnlyUsedInFixChanged = { onlyUsedInFix = it },
                                showLocationMarker = showLocationMarker,
                                onShowLocationMarkerChanged = { showLocationMarker = it },
                                navController = navController,
                                modifier = Modifier.Companion.fillMaxSize()
                            )
                        }

                        1 -> {
                            Scene3DParametersMenu(
                                parametersState = parametersState,
                                onParametersChanged = { newParams ->
                                    scene.updateParameters(newParams)
                                },
                                modifier = Modifier.Companion.fillMaxSize()
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