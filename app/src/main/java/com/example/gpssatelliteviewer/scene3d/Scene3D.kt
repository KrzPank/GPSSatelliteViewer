package com.example.gpssatelliteviewer.scene3d

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.scene3d.manager.CameraManager
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
    private var earth: EarthManager = EarthManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
        parameters = parameters
    )

    private var locationMarker: LocationMarkerManager = LocationMarkerManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
        parameters = parameters
    )

    private val camera: CameraManager = CameraManager(
        engine = engine,
        view = view,
        centerNode = centerNode,
        parameters = parameters
    )

    private var mainLight: LightHandler = LightHandler(
        engine = engine,
        centerNode = centerNode,
        cameraNode = camera.getCameraNode(),
        parameters = parameters
    )

    val satellites: SatelliteManager = SatelliteManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
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
                // Empty tap or node without a name → hide everything
                Log.d("clickingstuff", "Single-tap (empty scene): closing InfoBox")
                _clickedSatelliteKey.value = null
                _isSatelliteInfoBoxVisible = false
                _isEarthInfoBoxVisible = false
            }
            earth.getEarthNode().name -> {
                // Earth tapped
                Log.d("clickingstuff", "Tapped earth node")
                _clickedSatelliteKey.value = null
                _isSatelliteInfoBoxVisible = false
                _isEarthInfoBoxVisible = true
            }
            else -> {
                // Satellite tapped
                Log.d("clickingstuff", "Tapped node with satellite key: $key")
                _clickedSatelliteKey.value = key
                _isSatelliteInfoBoxVisible = true
                _isEarthInfoBoxVisible = false
            }
        }
    }

    fun resolveClickedSatelliteByKey(key: String?, satelliteList: List<GNSSStatusData>): GNSSStatusData? {
        if (key == null) return null
        val parts = key.split(":")
        if (parts.size != 2) return null
        val constellation = parts[0]
        val prn = parts[1].toIntOrNull() ?: return null
        return satelliteList.find { it.constellation == constellation && it.prn == prn }
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
            cameraNode = camera.getCameraNode(),
            cameraManipulator = camera.getCameraManipulator(),
            childNodes = listOf(centerNode),
            environment = environmentLoader.createHDREnvironment(parameters.environmentPath)!!,
            onFrame = {
                camera.onFrame(satellites, locationMarker)
                mainLight.onFrame()
            },
            mainLightNode = mainLight.getSunLightNode(),
            onGestureListener = rememberOnGestureListener(
                onDown = { event, _ -> true},
                onDoubleTap = { event, _ ->
                    onSceneDoubleTap()
                },
                onSingleTapConfirmed = { event, node ->
                    onSceneSingleTapConfirmed(node)
                },
                // sometimes prevents camera PAN ??
                onMove = { _, event, _ ->
                    if (event.pointerCount == 2 && event.actionMasked == MotionEvent.ACTION_MOVE) false
                    camera.getCameraGestureDetector().onTouchEvent(event)
                },
                onMoveBegin = { _, event, _ ->
                    if (event.pointerCount == 2 && event.actionMasked == MotionEvent.ACTION_MOVE) false
                    camera.getCameraGestureDetector().onTouchEvent(event)
                },
                onMoveEnd = { _, event, _ ->
                    if (event.pointerCount == 2 && event.actionMasked == MotionEvent.ACTION_MOVE) false
                    camera.getCameraGestureDetector().onTouchEvent(event)
                },
            ),
        )
    }

    fun updateScene(satelliteList: List<GNSSStatusData>) {
        satellites.updateSatellites(satelliteList, parameters.userLocation)
        locationMarker.updateLocationMarker(parameters.userLocation)
    }

    fun setLocationMarkerVisible(visible: Boolean) { locationMarker.setVisible(visible) }

    fun isLocationMarkerVisible() = locationMarker.isLocationMarkerVisible()

    fun updateParameters(newParameters: Scene3DParameters) {
        Log.d("Scene3D", "updateParameters called - intensity: ${newParameters.lightIntensity}, color: ${newParameters.lightColor}")
        parameters = newParameters

        mainLight.updateParameters(newParameters)
        satellites.updateParameters(newParameters)
        earth.updateEarthParameters(newParameters)
    }

    fun updateUserLocation(userLocation: Float3?) { parameters.userLocation = userLocation }

    fun cleanup() {
        satellites.cleanup()
        locationMarker.cleanup()
        earth.cleanup()

        mainLight.cleanup()

        camera.cleanup()
        centerNode.destroy()
    }
}