package com.example.gpssatelliteviewer.scene3d

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.scene3d.manager.camera.CameraManager
import com.example.gpssatelliteviewer.scene3d.manager.EarthManager
import com.example.gpssatelliteviewer.scene3d.manager.LightHandler
import com.example.gpssatelliteviewer.scene3d.manager.LocationMarkerManager
import com.example.gpssatelliteviewer.scene3d.manager.SatelliteManager
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.Node
import io.github.sceneview.rememberOnGestureListener

class Scene3D(
    private val engine: Engine,
    private val view: View,
    private val renderer: Renderer,
    private val scene: Scene,
    private val modelLoader: ModelLoader,
    private val environmentLoader: EnvironmentLoader,
    private val modifier: Modifier = Modifier.Companion.fillMaxSize(),
    private var parameters: Scene3DParameters = Scene3DParameters()
) {
    private val centerNode = Node(engine)

    // Management systems
    private val cameraManager: CameraManager = CameraManager(
        engine = engine,
        view = view,
        centerNode = centerNode,
        sceneParameters = parameters
    )

    private val earthManager: EarthManager = EarthManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
        parameters = parameters
    )

    private val locationMarkerManager: LocationMarkerManager = LocationMarkerManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
        cameraManager = cameraManager,
        parameters = parameters
    )

    private val mainLightManager: LightHandler = LightHandler(
        engine = engine,
        centerNode = centerNode,
        cameraManager = cameraManager,
        parameters = parameters
    )

    private val satellitesManager: SatelliteManager = SatelliteManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
        cameraManager = cameraManager,
        parameters = parameters
    )

    // Menu on/off
    private var _menuVisible by mutableStateOf(true)
    private fun toggleMenu() { _menuVisible = !_menuVisible }
    fun isMenuVisible() = _menuVisible

    // satellite click handling
    private val _clickedSatelliteKey = mutableStateOf<String?>(null)
    val clickedSatelliteKeyState: State<String?> get() = _clickedSatelliteKey

    private var _isSatelliteInfoBoxVisible by mutableStateOf(false)
    fun isSatelliteInfoBoxVisible() = _isSatelliteInfoBoxVisible

    private var _isEarthInfoBoxVisible by mutableStateOf(false)
    fun isEarthInfoBoxVisible() = _isEarthInfoBoxVisible

    private fun onSceneDoubleTap() {
        toggleMenu()
    }

    private fun onSceneSingleTapConfirmed(node: Node?) {
        val key = node?.name
        when (key) {
            null -> {
                _clickedSatelliteKey.value = null
                _isSatelliteInfoBoxVisible = false
                _isEarthInfoBoxVisible = false
            }
            earthManager.getEarthNode().name -> {
                _clickedSatelliteKey.value = null
                _isSatelliteInfoBoxVisible = false
                _isEarthInfoBoxVisible = !_isEarthInfoBoxVisible
            }
            else -> {
                _clickedSatelliteKey.value = key
                _isSatelliteInfoBoxVisible = true
                _isEarthInfoBoxVisible = false
            }
        }
    }

    @Composable
    fun Render() {
        Scene(
            modifier = modifier,
            engine = engine,
            view = view,
            renderer = renderer,
            scene = scene,
            modelLoader = modelLoader,
            cameraNode = cameraManager.getCameraNode(),
            cameraManipulator = cameraManager.getCameraManipulator(),
            childNodes = listOf(centerNode),
            environment = environmentLoader.createHDREnvironment(parameters.environmentPath)!!,
            onFrame = {
                cameraManager.onFrame()
                locationMarkerManager.onFrame()
                satellitesManager.onFrame()
                mainLightManager.onFrame()
            },
            mainLightNode = mainLightManager.getSunLightNode(),
            onGestureListener = rememberOnGestureListener(
                onDown = { event, _ ->
                    true
                },
                onDoubleTap = { event, _ ->
                    Log.d("CameraDebug", "Went in onDoubleTap")
                    onSceneDoubleTap()
                },
                onSingleTapConfirmed = { event, node ->
                    Log.d("CameraDebug", "Went in onSingleTapConfirmed")
                    onSceneSingleTapConfirmed(node)
                },
            ),
        )
    }

    fun setLocationMarkerVisible(visible: Boolean) { locationMarkerManager.setVisible(visible) }
    fun isLocationMarkerVisible() = locationMarkerManager.isLocationMarkerVisible()

    fun updateSatelliteList(newList: List<GNSSStatusData>){
        satellitesManager.updateSatelliteList(newList)
    }

    fun updateParameters(newParameters: Scene3DParameters) {
        Log.d("Scene3D", "updateParameters called - intensity: ${newParameters.lightIntensity}, color: ${newParameters.lightColor}")
        parameters = newParameters

        mainLightManager.updateParameters(newParameters)
        satellitesManager.updateParameters(newParameters)
        earthManager.updateParameters(newParameters)
        locationMarkerManager.updateParameters(newParameters)
    }

    fun updateUserLocation(userLocation: Float3?) { parameters.userLocation = userLocation }

    fun cleanup() {
        satellitesManager.cleanup()
        locationMarkerManager.cleanup()
        earthManager.cleanup()

        mainLightManager.cleanup()

        cameraManager.cleanup()
        centerNode.destroy()
    }
}
