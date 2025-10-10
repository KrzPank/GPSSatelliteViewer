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
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView

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

    private val cameraManager: CameraManager = CameraManager(engine, view, centerNode)

    // Management systems
    val satellites: SatelliteManager = SatelliteManager(modelLoader, centerNode, parameters)
    private var mainLight: LightHandler = LightHandler(engine, centerNode, cameraManager.getCameraNode(), parameters)
    private var locationMarker: LocationMarkerManager = LocationMarkerManager(modelLoader, centerNode, parameters)
    private var earthNode: ModelNode? = null

    // Menu on/off
    private var _menuVisible by mutableStateOf(true)
    private fun toggleMenu() { _menuVisible = !_menuVisible }
    fun isMenuVisible() = _menuVisible

    // satellite click handling
    private val _clickedSatelliteKey = mutableStateOf<String?>(null)
    val clickedSatelliteKeyState: State<String?> get() = _clickedSatelliteKey

    private var _isSatelliteInfoBoxVisible by mutableStateOf(false)
    fun isSatelliteInfoBoxVisible() = _isSatelliteInfoBoxVisible

    private var _satelliteClicked = false

    private fun onSceneDoubleTap() {
        if (_satelliteClicked) _isSatelliteInfoBoxVisible = true
        Log.d("clickingstuff", "double tap: satellite box:${_isSatelliteInfoBoxVisible}")
        toggleMenu()
    }

    private fun onSceneSingleTapConfirmed(node: Node?) {
        if (node != null) {
            val key = node.name
            if (key != null) {
                Log.d("clickingstuff", "Tapped node with satellite key: ${node.name}")
                _clickedSatelliteKey.value = key
                _isSatelliteInfoBoxVisible = true
                _satelliteClicked = true
            } else {
                // TODO add Earth node click and location variants
                _isSatelliteInfoBoxVisible = false
                _clickedSatelliteKey.value = null
                _satelliteClicked = false
                Log.d("clickingstuff", "Tapped node with no satellite key: ${node.name}")
            }
            return
        }

        // genuine empty-scene single tap -> close the info box
        Log.d("clickingstuff", "Single-tap (empty scene): closing InfoBox")
        _isSatelliteInfoBoxVisible = false
        _clickedSatelliteKey.value = null
        _satelliteClicked = false
    }

    fun resolveClickedSatelliteByKey(key: String?, satelliteList: List<GNSSStatusData>): GNSSStatusData? {
        if (key == null) return null
        val parts = key.split(":")
        if (parts.size != 2) return null
        val constellation = parts[0]
        val prn = parts[1].toIntOrNull() ?: return null
        return satelliteList.find { it.constellation == constellation && it.prn == prn }
    }

    // Scene initialization
    private var _isSceneReady by mutableStateOf(false)
    fun isSceneReady(): Boolean = _isSceneReady

    private var hasInitialized = false
    private var isInitializing = false
    fun initializeScene() {
        // Synchronous initialization check
        if (!hasInitialized && !isInitializing) {
            isInitializing = true
            try {
                setupScene()
                hasInitialized = true
                _isSceneReady = true
            } finally {
                isInitializing = false
            }
        }
    }

    private fun setupScene() {
        try {
            earthNode = ModelNode(
                modelInstance = modelLoader.createModelInstance(parameters.earthModelPath),
                scaleToUnits = parameters.earthScale
            ).also {
                centerNode.addChildNode(it)
            it.name = "Earth node"
            }
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to load Earth model: ${e.message}")
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
                cameraManager.updateLookAt(satellites, locationMarker)
                mainLight.onFrame()
            },
            mainLightNode = mainLight.getSunLightNode(),
            onGestureListener = rememberOnGestureListener(
                onDown = { event, _ -> true},
                onDoubleTap = { event, _ ->
                    onSceneDoubleTap()
                    //cameraManager.resetCamera()
                },
                onSingleTapConfirmed = { event, node ->
                    onSceneSingleTapConfirmed(node)
                },
                // sometimes prevents camera PAN ??
                onMove = { _, event, _ ->
                    if (event.pointerCount == 2 && event.actionMasked == MotionEvent.ACTION_MOVE) false
                    cameraManager.getCameraGestureDetector().onTouchEvent(event)
                },
                onMoveBegin = { _, event, _ ->
                    if (event.pointerCount == 2 && event.actionMasked == MotionEvent.ACTION_MOVE) false
                    cameraManager.getCameraGestureDetector().onTouchEvent(event)
                },
                onMoveEnd = { _, event, _ ->
                    if (event.pointerCount == 2 && event.actionMasked == MotionEvent.ACTION_MOVE) false
                    cameraManager.getCameraGestureDetector().onTouchEvent(event)
                },
            ),
        )
    }

    fun updateScene(satelliteList: List<GNSSStatusData>, userLocation: Float3) {
        satellites.updateSatellites(satelliteList, userLocation)
        locationMarker.updateLocationMarker(userLocation)
    }

    fun setLocationMarkerVisible(visible: Boolean) {
        locationMarker.setVisible(visible)
    }

    fun isLocationMarkerVisible() = locationMarker.isLocationMarkerVisible()

    fun updateParameters(newParameters: Scene3DParameters) {
        Log.d("Scene3D", "updateParameters called - intensity: ${newParameters.lightIntensity}, color: ${newParameters.lightColor}")

        val oldParameters = parameters
        parameters = newParameters

        mainLight.updateParameters(newParameters)

        satellites.updateParameters(newParameters)

        // Handle earth model path changes
        if (oldParameters.earthModelPath != newParameters.earthModelPath) {
            updateEarthModel()
            Log.d("Scene3D", "Updated earth model")
        }
    }

    private fun updateEarthModel() {
        try {
            earthNode?.let { centerNode.removeChildNode(it) }
            earthNode?.destroy()

            earthNode = ModelNode(
                modelInstance = modelLoader.createModelInstance(parameters.earthModelPath),
                scaleToUnits = parameters.earthScale
            ).also { centerNode.addChildNode(it) }
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to update earth model: ${e.message}")
            // Fall back to default if loading fails
            earthNode = ModelNode(
                modelInstance = modelLoader.createModelInstance(Scene3DParameters().earthModelPath),
                scaleToUnits = parameters.earthScale
            ).also { centerNode.addChildNode(it) }
        }
    }

    fun cleanup() {
        satellites.cleanup()

        earthNode?.destroy()
        locationMarker.cleanup()

        mainLight.cleanup()

        cameraManager.cleanup()
        centerNode.destroy()
    }
}