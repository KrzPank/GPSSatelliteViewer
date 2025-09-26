package com.example.gpssatelliteviewer.utils

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.Scene3DParameters
import com.example.gpssatelliteviewer.scene3d.LightHandler
import com.example.gpssatelliteviewer.scene3d.SatelliteManager
import com.google.android.filament.Engine
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


class Scene3D(
    private val modifier: Modifier = Modifier.fillMaxSize(),
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

    // Clean management systems
    private val lightHandler = LightHandler(engine, centerNode, cameraNode, parameters)
    private val satelliteManager = SatelliteManager(modelLoader, centerNode, cameraNode, parameters)
    private var earthNode: ModelNode? = null
    private var locationMarkerNode: ModelNode? = null

    // Loading state management
    private var isSceneReady = false
    private val _initializationLock = Object()

    var menuVisible: Boolean = true
    
    fun isReady(): Boolean = synchronized(_initializationLock) { isSceneReady }
    private var hasInitialized = false
    private var isInitializing = false
    
    /**
     * Initialize the 3D scene asynchronously on main thread
     * Uses yielding to allow UI updates between heavy operations
     */

    fun initializeScene() {
        // Check if we should initialize (thread-safe)
        val shouldInitialize = synchronized(_initializationLock) {
            if (!hasInitialized && !isInitializing) {
                isInitializing = true
                true
            } else {
                false
            }
        }
        
        if (shouldInitialize) {
            try {
                // Setup scene outside of synchronized block to allow suspension points
                setupScene()
                
                // Mark as completed (thread-safe)
                synchronized(_initializationLock) {
                    hasInitialized = true
                    isSceneReady = true
                }
            } finally {
                synchronized(_initializationLock) {
                    isInitializing = false
                }
            }
        }
    }

    private fun setupScene() {
        Log.d("Scene3D", "Starting scene setup...")
        
        // Load Earth model
        try {
            earthNode = ModelNode(
                modelInstance = modelLoader.createModelInstance(parameters.earthModelPath),
                scaleToUnits = parameters.earthScale
            ).also { centerNode.addChildNode(it) }
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to load Earth model: ${e.message}")
        }

        // Initialize location marker
        createLocationMarker()

        // Apply visual effects
        try {
            applyVisualEffects(view)
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to apply visual effects: ${e.message}")
        }

        // Mark scene as ready (thread-safe)
        synchronized(_initializationLock) {
            isSceneReady = true
        }
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
                lightHandler.onFrame()
            },
            mainLightNode = lightHandler.getMainLightNode(),
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
        satelliteManager.updateLookAt()
        
        // Update location marker orientation
        locationMarkerNode?.lookAt(cameraNode.worldPosition)
    }

    fun updateSatellites(satelliteList: List<GNSSStatusData>, userLocation: Triple<Float, Float, Float>) {
        satelliteManager.updateSatellites(satelliteList, userLocation)
        updateLocationMarker(userLocation)
    }


    /**
     * Create and initialize the location marker
     */
    private fun createLocationMarker() {
        try {
            // Create location marker node with a distinct model (you can use the same satellite model for now)
            val markerInstance = modelLoader.createModelInstance(parameters.satelliteModelPath)
            locationMarkerNode = ModelNode(
                modelInstance = markerInstance,
                scaleToUnits = parameters.satelliteScale * 2.0f // Make it slightly larger for visibility
            ).also {
                centerNode.addChildNode(it)
                // Set initial position to origin (will be updated when location is available)
                it.position = Float3(0f, 0f, 0f)
            }
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to create location marker: ${e.message}")
            locationMarkerNode = null
        }
    }

    /**
     * Update location marker to show user's current position on Earth surface
     */
    private fun updateLocationMarker(userLocation: Triple<Float, Float, Float>) {
        try {
            // Create location marker if it doesn't exist yet
            if (locationMarkerNode == null) {
                createLocationMarker()
            }
            
            val (lat, lon, alt) = userLocation
            val ecefPosition = CoordinateConversion.geodeticToECEF(
                lat.toDouble(), 
                lon.toDouble(), 
                (alt + 5000f).toDouble()  // Add 5km above surface to make marker visible
            )
            val scenePosition = CoordinateConversion.ecefToScenePos(ecefPosition)
            
            locationMarkerNode?.position = scenePosition

        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to update location marker: ${e.message}")
        }
    }

    private fun toggleMenu() {
        menuVisible = !menuVisible
    }

    /**
     * Update the scene parameters and apply changes
     */
    fun updateParameters(newParameters: Scene3DParameters) {
        Log.d("Scene3D", "updateParameters called - intensity: ${newParameters.lightIntensity}, color: ${newParameters.lightColor}")
        
        val oldParameters = parameters
        parameters = newParameters

        // Update light parameters using the light management system
        lightHandler.updateParameters(newParameters)
        
        // Update satellite parameters using the satellite manager
        satelliteManager.updateParameters(newParameters)

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
     * Apply visual effects based on current parameters (uses high preset by default)
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
                enabled = parameters.dynamicResolutionEnabled
                if (enabled) {
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
        // Cleanup satellite management system
        satelliteManager.cleanup()
        
        earthNode?.destroy()
        locationMarkerNode?.let {
            centerNode.removeChildNode(it)
            it.destroy()
        }
        locationMarkerNode = null
        
        // Cleanup light management system
        lightHandler.cleanup()
        
        cameraNode.destroy()
        centerNode.destroy()
    }
}