package com.example.gpssatelliteviewer.scene3d

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.Scene3DParameters
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
    private val centerNode = Node(engine)
    private val cameraNode = CameraNode(engine).apply {
        position = parameters.startingCameraLocation
        lookAt(centerNode)
        centerNode.addChildNode(this)
    }

    // Management systems
    private lateinit var mainLight: LightHandler
    private lateinit var satellites: SatelliteManager
    private lateinit var locationMarker: LocationMarkerManager
    private var earthNode: ModelNode? = null

    // Loading state management - now fully synchronous
    private var isSceneReady = false

    private var menuVisible: Boolean = true
    fun isMenuVisible(): Boolean = menuVisible
    private fun toggleMenu() {
        menuVisible = !menuVisible
    }

    fun isReady(): Boolean = isSceneReady
    private var hasInitialized = false
    private var isInitializing = false

    /**
     * Initialize the 3D scene synchronously
     */
    fun initializeScene() {
        // Simple synchronous initialization check
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
        Log.d("Scene3D", "Starting scene setup...")
        mainLight = LightHandler(engine, centerNode, cameraNode, parameters)
        satellites = SatelliteManager(modelLoader, centerNode, parameters)
        locationMarker = LocationMarkerManager(modelLoader, centerNode, parameters)

        // Load Earth model
        try {
            earthNode = ModelNode(
                modelInstance = modelLoader.createModelInstance(parameters.earthModelPath),
                scaleToUnits = parameters.earthScale
            ).also { centerNode.addChildNode(it) }
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to load Earth model: ${e.message}")
        }

        // Apply visual effects
        try {
            applyVisualEffects(view)
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to apply visual effects: ${e.message}")
        }

        // Mark scene as ready
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
                // Don't update lights here - let them be managed by scene graph
                mainLight.onFrame()
            },
            // Don't provide mainLightNode to avoid conflicts with SceneView's lighting
            mainLightNode = mainLight.getMainLightNode(),
            onGestureListener = rememberOnGestureListener(
                onDoubleTap = { _, _ ->
                    toggleMenu()
                }
            ),
        )
    }

    private fun updateLookAt() {
        cameraNode.lookAt(centerNode)

        // Update satellite orientations
        satellites.updateLookAt(cameraNode)

        // Update location marker orientation
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