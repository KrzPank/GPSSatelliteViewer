package com.example.gpssatelliteviewer.scene3d.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.example.gpssatelliteviewer.app.theme.DarkBackgroundColor
import com.example.gpssatelliteviewer.app.theme.GreenPrimaryColor
import com.example.gpssatelliteviewer.app.theme.TextLabelColor
import com.example.gpssatelliteviewer.data.GNSSCombinedData
import com.example.gpssatelliteviewer.data.mergeLists
import com.example.gpssatelliteviewer.data.viewmodel.LocationViewModel
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

private const val LOADING_SCREEN_ANIMATION_DURATION = 100
private const val MENU_ANIMATION_DURATION = 300
private val MENU_WIDTH = 300.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Satellite3DScreen(
    navController: NavController,
    gnssViewModel: GNSSViewModel,
    locationNMEA: NMEALocationData,
    locationViewModel: LocationViewModel
) {
    HideSystemUI()
    LockOrientationLandscape()

    val satelliteList by gnssViewModel.satelliteList.collectAsState()
    val measurements by gnssViewModel.gnssMeasurements.collectAsState()
    val azElHistory by gnssViewModel.azElHistory.collectAsState()

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
    val safeInsets = WindowInsets.Companion.safeDrawing.asPaddingValues()
    val totalMenuWidth = MENU_WIDTH + safeInsets.calculateLeftPadding(LayoutDirection.Ltr)

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

    val isSatelliteInfoBoxVisible by scene.satelliteInfoBoxVisible.collectAsState()
    val isEarthInfoBoxVisible by scene.earthInfoBoxVisible.collectAsState()

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(DarkBackgroundColor)
    ) {
        // Smooth transition not wanted but i don't know other way
        var isSceneReady by remember { mutableStateOf(false) }
        LaunchedEffect(scene) {
            isSceneReady = true
        }

        AnimatedVisibility(
            visible = !isSceneReady,
            enter = fadeIn(
                animationSpec = tween(LOADING_SCREEN_ANIMATION_DURATION)
            ),
            exit = fadeOut(
                animationSpec = tween(LOADING_SCREEN_ANIMATION_DURATION)
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

        LaunchedEffect(filteredSatellites) {
            if (isSceneReady) {
                scene.updateSatelliteList(filteredSatellites, azElHistory)
            }
        }

        // Handle satellite click
        val satelliteInfo = mergeLists(satelliteList, measurements)
        val clickedSatelliteKey by scene.clickedSatelliteKeyState.collectAsState()
        val clickedSatellite by remember (clickedSatelliteKey, satelliteInfo) {
            derivedStateOf { resolveClickedSatelliteByKey(clickedSatelliteKey, satelliteInfo) }
        }

        AnimatedVisibility(
            visible = isSceneReady,
            enter = fadeIn(
                animationSpec = tween(MENU_ANIMATION_DURATION)
            )
        ) {
            // Animate horizontal offset
            val targetOffset = if (scene.isMenuVisible()) totalMenuWidth / 2 else 0.dp
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = tween(durationMillis = MENU_ANIMATION_DURATION),
                label = "sceneOffsetAnim"
            )

            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .offset(x = animatedOffset)
            ) {
                scene.Render()

                AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = when {
                        isSatelliteInfoBoxVisible -> "satellite"
                        isEarthInfoBoxVisible -> "earth"
                        else -> "none"
                    },
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(MENU_ANIMATION_DURATION)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(MENU_ANIMATION_DURATION)
                        )
                    },
                    label = "InfoBoxTransition",
                ) { target ->
                    when (target) {
                        "satellite" -> SatelliteInfoBox(
                            clickedSatellite = clickedSatellite,
                            isMenuVisible = scene.isMenuVisible(),
                            safeInsets = safeInsets,
                            totalMenuWidth = totalMenuWidth
                        )
                        "earth" -> EarthInfoBox(
                            userLocation = userLocation,
                            nmea = locationNMEA,
                            isMenuVisible = scene.isMenuVisible(),
                            safeInsets = safeInsets,
                            totalMenuWidth = totalMenuWidth
                        )
                        "none" -> Box(modifier = Modifier.fillMaxSize()) {}
                    }
                }
            }
        }

        if (isSceneReady) {
            AnimatedVisibility(
                visible = scene.isMenuVisible(),
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(MENU_ANIMATION_DURATION)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(MENU_ANIMATION_DURATION)
                )
            ) {
                Column(
                    modifier = Modifier.Companion
                        .fillMaxHeight()
                        .requiredWidth(totalMenuWidth)
                        .background(DarkBackgroundColor)
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
                        contentColor = TextLabelColor,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.Companion.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = GreenPrimaryColor
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Satellites", color = TextLabelColor) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Scene Settings", color = TextLabelColor) }
                        )
                    }

                    when (selectedTab) {
                        0 -> {
                            SatelliteFilterMenu(
                                satelliteList = satelliteList,
                                azElHistory = azElHistory,
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

private fun resolveClickedSatelliteByKey(key: String?, satelliteList: List<GNSSCombinedData>): GNSSCombinedData? {
    if (key == null) return null
    val parts = key.split(":")
    if (parts.size != 2) return null
    val constellation = parts[0]
    val prn = parts[1].toIntOrNull() ?: return null
    return satelliteList.find { it.constellation == constellation && it.prn == prn }
}
