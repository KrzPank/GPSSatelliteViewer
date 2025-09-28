package com.example.gpssatelliteviewer.scene3d

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.Scene3DParameters
import com.example.gpssatelliteviewer.scene3d.manager.LightHandler
import com.example.gpssatelliteviewer.scene3d.manager.LocationMarkerManager
import com.example.gpssatelliteviewer.scene3d.manager.SatelliteManager
import com.google.android.filament.Engine
import com.google.android.filament.View
import io.github.sceneview.Scene
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberOnGestureListener

class Scene3D(
    private val modifier: Modifier = Modifier.Companion.fillMaxSize(),
    private val environmentLoader: EnvironmentLoader,
    private val modelLoader: ModelLoader,
    private val engine: Engine,
    private val view: View,
    private var parameters: Scene3DParameters = Scene3DParameters()
) {
    // Management systems
    private lateinit var mainLight: LightHandler
    private lateinit var satellites: SatelliteManager
    private lateinit var locationMarker: LocationMarkerManager

    private val centerNode = Node(engine)
    private val cameraNode = CameraNode(engine).apply {
        position = parameters.startingCameraLocation
        lookAt(centerNode)
        centerNode.addChildNode(this)
    }
    private var earthNode: ModelNode? = null

    private var _menuVisible by mutableStateOf(true)
    
    private var isSceneReady by mutableStateOf(false)

    private var hasInitialized = false
    private var isInitializing = false

    fun isMenuVisible(): Boolean = _menuVisible

    private fun toggleMenu() {
        _menuVisible = !_menuVisible
    }

    fun isReady(): Boolean = isSceneReady

    /**
     * Initialize the 3D scene synchronously
     */
    fun initializeScene() {
        // Synchronous initialization check
        if (!hasInitialized && !isInitializing) {
            isInitializing = true
            try {
                setupScene()
                hasInitialized = true
                isSceneReady = true
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
            ).also { centerNode.addChildNode(it) }
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to load Earth model: ${e.message}")
        }

        satellites = SatelliteManager(modelLoader, centerNode, parameters)
        locationMarker = LocationMarkerManager(modelLoader, centerNode, parameters)
        mainLight = LightHandler(engine, centerNode, cameraNode, parameters)

        // Apply visual effects
        try {
            applyVisualEffects(view)
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to apply visual effects: ${e.message}")
        }

        isSceneReady = true
    }

    @Composable
    fun Render() {
        Scene(
            modifier = modifier,
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = cameraNode.worldPosition,
                targetPosition = centerNode.worldPosition
            ),
            childNodes = listOf(centerNode),
            environment = environmentLoader.createHDREnvironment(parameters.environmentPath)!!,
            view = view,
            onFrame = {
                updateLookAt()
                mainLight.onFrame()
            },
            mainLightNode = mainLight.getSunLightNode(),
            onGestureListener = rememberOnGestureListener(
                onDoubleTap = { _, _ ->
                    toggleMenu()
                }
            ),
        )
    }

    private fun updateLookAt() {
        cameraNode.lookAt(centerNode)

        satellites.updateLookAt(cameraNode)

        locationMarker.updateLookAt(cameraNode)
    }

    fun updateScene(satelliteList: List<GNSSStatusData>, userLocation: Triple<Float, Float, Float>) {
        satellites.updateSatellites(satelliteList, userLocation)
        locationMarker.updateLocationMarker(userLocation)
    }

    /**
     * Update the scene parameters and apply changes
     */
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


    /**
     * Apply visual effects based on current parameters (uses medium preset by default)
     */
    private fun applyVisualEffects(view: View) {
        try {
            // HDR Color Buffer Quality
            view.renderQuality = view.renderQuality.apply {
                hdrColorBuffer = when (parameters.hdrColorBufferQuality) {
                    Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
                    Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
                    Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
                    Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
                }
            }

            // Dynamic Resolution
            view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
                if (parameters.dynamicResolutionEnabled) {
                    quality = when (parameters.dynamicResolutionQuality) {
                        Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
                        Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
                        Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
                        Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
                    }
                }
            }

            // MSAA (Multi-Sample Anti-Aliasing)
            view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
                enabled = parameters.msaaEnabled
            }

            // FXAA (Fast Approximate Anti-Aliasing)
            view.antiAliasing = if (parameters.fxaaEnabled) {
                View.AntiAliasing.FXAA
            } else {
                View.AntiAliasing.NONE
            }

            // Temporal Anti-Aliasing
            view.temporalAntiAliasingOptions = view.temporalAntiAliasingOptions.apply {
                enabled = parameters.temporalAntiAliasingEnabled
            }

            // Ambient Occlusion
            view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
                enabled = parameters.ambientOcclusionEnabled
            }

            // Bloom
            view.bloomOptions = view.bloomOptions.apply {
                enabled = parameters.bloomEnabled
            }

            // Screen Space Reflections
            view.screenSpaceReflectionsOptions = view.screenSpaceReflectionsOptions.apply {
                enabled = parameters.screenSpaceReflectionsEnabled
            }

            Log.d("Scene3D", "Visual effects applied: HDR=${parameters.hdrColorBufferQuality}, " +
                    "DynRes=${parameters.dynamicResolutionEnabled}(${parameters.dynamicResolutionQuality}), " +
                    "MSAA=${parameters.msaaEnabled}, FXAA=${parameters.fxaaEnabled}, " +
                    "TAA=${parameters.temporalAntiAliasingEnabled}, AO=${parameters.ambientOcclusionEnabled}, " +
                    "Bloom=${parameters.bloomEnabled}, SSR=${parameters.screenSpaceReflectionsEnabled}")

        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to apply visual effects: ${e.message}")
            Log.w("Scene3D", "Scene will render with default quality settings")
        }
    }

    fun cleanup() {
        satellites.cleanup()

        earthNode?.destroy()
        locationMarker.cleanup()

        mainLight.cleanup()

        cameraNode.destroy()
        centerNode.destroy()
    }
}