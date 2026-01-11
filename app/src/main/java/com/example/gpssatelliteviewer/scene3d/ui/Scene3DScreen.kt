package com.example.gpssatelliteviewer.scene3d.ui

import android.view.Display
import android.view.Surface
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
import com.example.gpssatelliteviewer.mainscreen.screen.locationinfo.IS_LOCATION_ENABLED_TIMER
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
import java.util.concurrent.atomic.AtomicBoolean

private const val LOADING_SCREEN_ANIMATION_DURATION = 400L
private const val LOADING_SCREEN_FADEOUT_DURATION = 300
private const val MENU_ANIMATION_DURATION = 300
private val MENU_WIDTH = 300.dp

// TODO JESLI WYLACZYMY LOKALIZACJA NA SCENIE 3D TO SATELITY NIE ZNIKAJA TYLKO PAMIETANA SA STERE DANE
// CZYLI SCENA NIE POKAZUJE NOWYCH WIADOMOSCI -> TELEFON TAK NA PRAWDE NIE WIDZI SATELIT ALE NA SCENIE SA POKAZANE

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

    var startEngineInit by remember { mutableStateOf(false) }
    var isSceneReady by remember { mutableStateOf(false) }

    val isSceneActive = remember { AtomicBoolean(true) }

    LaunchedEffect(Unit) {
        delay(LOADING_SCREEN_ANIMATION_DURATION)
        startEngineInit = true
    }

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

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(DarkBackgroundColor)
    ) {
        if (startEngineInit) {
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
            // Handle location marker visibility changes
            var showLocationMarker by remember { mutableStateOf(scene.isLocationMarkerVisible()) }
            LaunchedEffect(showLocationMarker, userLocation) {
                if (isSceneActive.get()) { // SPRAWDZAMY CZY ŻYJE
                    scene.setLocationMarkerVisible(showLocationMarker)
                    scene.updateUserLocation(userLocation)
                }
            }

            LaunchedEffect(filteredSatellites) {
                if (isSceneActive.get()) { // SPRAWDZAMY CZY ŻYJE
                    scene.updateSatelliteList(filteredSatellites, azElHistory)
                }
            }

            // Handle satellite click
            val satelliteInfo = mergeLists(satelliteList, measurements)
            val clickedSatelliteKey by scene.clickedSatelliteKeyState.collectAsState()
            val clickedSatellite by remember(clickedSatelliteKey, satelliteInfo) {
                derivedStateOf { resolveClickedSatelliteByKey(clickedSatelliteKey, satelliteInfo) }
            }

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
                scene.Render(
                    onSceneLoaded = {
                        if (isSceneActive.get()) isSceneReady = true
                    }
                )

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

            DisposableEffect(Unit) {
                onDispose {
                    isSceneActive.set(false)

                    try {
                        scene.cleanup()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isSceneReady,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(LOADING_SCREEN_FADEOUT_DURATION)) // Czas na płynne zniknięcie
        ) {
            Scene3DLoadingScreen(modifier = Modifier.fillMaxSize())
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
