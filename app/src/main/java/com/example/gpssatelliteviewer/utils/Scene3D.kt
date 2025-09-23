package com.example.gpssatelliteviewer.utils

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.Scene3DParameters
import com.example.gpssatelliteviewer.utils.CoordinateConversion.azElToECEF
import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt



class Scene3D(
    private val modifier: Modifier = Modifier.fillMaxSize(),
    private val environmentLoader: EnvironmentLoader,
    private val modelLoader: ModelLoader,
    private val engine: Engine,
    private val view: View,
    private var parameters: Scene3DParameters = Scene3DParameters()
) {
    /*
    Orbit heights were taken from https://en.wikipedia.org/wiki/Satellite_navigation
    and https://en.wikipedia.org/wiki/List_of_BeiDou_satellites
    */
    private val keysFor35786000 = setOf(1,2,3,4,5,6,7,8,9,10,13,16,31,38,39,40,56,59,60,61,62)
    private val constellationAltitudes = mapOf(
        "GLONASS" to 19100000f,
        "Galileo" to 23222000f,
        "QZSS" to 35800000f, // average height (elliptical orbit 32,600–39,000 km)
        "IRNSS" to 36000000f,
        "SBAS" to 35786000f, // Usually GEO
        "BeiDou" to 21500000f,
        "GPS" to 20180000f,
        "Unknown" to 0f,
        "Other" to 0f
    )

    private val centerNode = Node(engine)
    private val cameraNode = CameraNode(engine).apply {
        position = parameters.startingCameraLocation
        lookAt(centerNode)
        centerNode.addChildNode(this)
    }

    // Mutable light node that can be recreated when needed
    private var mainLight: LightNode = createMainLight()
    private var isLightBeingRecreated = false
    private val lightUpdateLock = Object()
    private var framesAfterLightRecreation = 0
    
    // Light update optimization
    private var lastCameraPosition = Float3(0f, 0f, 0f)
    private var lastLightUpdateFrame = 0
    private var frameCount = 0
    private val lightUpdateThreshold = 0.1f // Minimum camera movement to trigger light update
    private val lightUpdateInterval = 5 // Update light every 5 frames minimum

    // Dynamic object pooling for satellite nodes
    private val satelliteNodePool = mutableListOf<ModelNode>() // Reusable nodes from disappeared satellites
    private val activeSatelliteNodes = mutableMapOf<Int, ModelNode>() // PRN -> active ModelNode
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
    suspend fun initializeScene() {
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
                setupSceneWithYielding()
                
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
    
    /**
     * Setup scene with yield points to maintain UI responsiveness
     * Breaks heavy operations into chunks with coroutine yields
     */
    private suspend fun setupSceneWithYielding() {
        Log.d("Scene3D", "Starting scene setup with yielding...")
        
        // Load Earth model with yield
        try {
            earthNode = ModelNode(
                modelInstance = modelLoader.createModelInstance(parameters.earthModelPath),
                scaleToUnits = parameters.earthScale
            ).also { centerNode.addChildNode(it) }
            
            // Yield to allow UI updates
            delay(1)
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to load Earth model: ${e.message}")
        }

        // Initialize location marker with yield
        createLocationMarker()
        delay(1) // Yield for UI responsiveness

        // Apply visual effects with yield
        try {
            applyVisualEffects(view)
            delay(1) // Yield for UI responsiveness
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to apply visual effects: ${e.message}")
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
        // Additional initialization to ensure light is properly applied after scene setup
        LaunchedEffect(Unit) {
            // Small delay to ensure scene is fully initialized
            delay(100)
            if (!isLightBeingRecreated) {
                Log.d("Scene3D", "Post-initialization light check - ensuring correct color")
                recreateMainLight()
            }
        }
        
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
                if (shouldUpdateLight()) {
                    updateLight()
                }
            },
            mainLightNode = mainLight,
            onGestureListener = rememberOnGestureListener(
                onDoubleTap = { _, _ ->
                    toggleMenu()
                }
            ),
        )
    }

    private fun updateLookAt() {
        cameraNode.lookAt(centerNode)

        // Update orientation for all active satellite nodes
        activeSatelliteNodes.values.forEach { satellite ->
            satellite.lookAt(cameraNode.worldPosition)
        }
        locationMarkerNode?.lookAt(cameraNode.worldPosition)
    }
    
    /**
     * Calculate optimal light position based on light type and camera position
     */
    private fun calculateLightPosition(lightType: LightManager.Type): Float3 {
        return when (lightType) {
            LightManager.Type.SUN, LightManager.Type.DIRECTIONAL -> 
                Float3(5.0f, 5.0f, 5.0f) // Far away for directional lighting
            
            LightManager.Type.POINT, LightManager.Type.SPOT -> {
                // Position near Earth with slight camera-based adjustment for good lighting
                val offset = (cameraNode.worldPosition - centerNode.worldPosition).normalized() * 0.5f
                Float3(2.0f, 2.0f, 2.0f) + offset
            }
            
            else -> Float3(2.0f, 2.0f, 2.0f)
        }
    }
    
    /**
     * Update light position and/or direction based on light type
     */
    private fun updateLightProperties(lightType: LightManager.Type, position: Float3, direction: Float3) {
        when (lightType) {
            LightManager.Type.DIRECTIONAL, LightManager.Type.SUN -> {
                engine.lightManager.setDirection(mainLight.entity, direction.x, direction.y, direction.z)
            }
            
            LightManager.Type.SPOT -> {
                engine.lightManager.setPosition(mainLight.entity, position.x, position.y, position.z)
                engine.lightManager.setDirection(mainLight.entity, direction.x, direction.y, direction.z)
            }
            
            LightManager.Type.POINT -> {
                engine.lightManager.setPosition(mainLight.entity, position.x, position.y, position.z)
            }
            
            else -> {
                Log.w("Scene3D", "Unknown light type: $lightType")
            }
        }
    }

    private fun createMainLight(): LightNode {
        val lightEntity = engine.entityManager.create()

        val lightType = when (parameters.lightType) {
            Scene3DParameters.LightType.POINT -> LightManager.Type.POINT
            Scene3DParameters.LightType.DIRECTIONAL -> LightManager.Type.DIRECTIONAL
            Scene3DParameters.LightType.SPOT -> LightManager.Type.SPOT
            Scene3DParameters.LightType.SUN -> LightManager.Type.SUN
        }

        // Position light as a proper light source
        val lightPosition = calculateLightPosition(lightType)
        val lightDirection = (centerNode.worldPosition - lightPosition).normalized()

        // Validate light color parameters to prevent fallback to default red
        val lightColor = parameters.lightColor
        val red = lightColor.x
        val green = lightColor.y
        val blue = lightColor.z

        Log.d("Scene3D", "Creating light with color RGB($red, $green, $blue), intensity=${parameters.lightIntensity}, type=${parameters.lightType}")

        val builder = LightManager.Builder(lightType)
            .intensity(parameters.lightIntensity)
            .color(red, green, blue)
            .falloff(parameters.lightFalloff)
            .position(lightPosition.x, lightPosition.y, lightPosition.z)

        // Set direction for directional lights during creation
        when (lightType) {
            LightManager.Type.DIRECTIONAL,
            LightManager.Type.SUN -> {
                builder.direction(lightDirection.x, lightDirection.y, lightDirection.z)
            }
            LightManager.Type.SPOT -> {
                builder.direction(lightDirection.x, lightDirection.y, lightDirection.z)
                    .spotLightCone(Math.toRadians(30.0).toFloat(), Math.toRadians(45.0).toFloat()) // Inner and outer cone angles
            }
            LightManager.Type.POINT -> {
                // Point lights don't need direction
            }
            else -> {}
        }

        builder.build(engine, lightEntity)

        val lightNode = LightNode(engine, lightEntity).apply {
            centerNode.addChildNode(this)
        }

        Log.d("Scene3D", "Light node created successfully with entity ID: ${lightEntity}")
        return lightNode
    }
    
    /**
     * Check if light should be updated based on camera movement and frame count
     */
    private fun shouldUpdateLight(): Boolean {
        frameCount++
        
        // Skip if light is being recreated or just recreated
        if (isLightBeingRecreated || framesAfterLightRecreation < 3) {
            if (framesAfterLightRecreation < 3) framesAfterLightRecreation++
            return false
        }
        
        // Check frame interval and camera movement
        val frameIntervalMet = frameCount - lastLightUpdateFrame >= lightUpdateInterval
        val cameraMovement = (cameraNode.worldPosition - lastCameraPosition).length()
        
        return frameIntervalMet && cameraMovement > lightUpdateThreshold
    }

    private fun updateLight() {
        synchronized(lightUpdateLock) {
            try {
                // Validate light entity
                if (!engine.entityManager.isAlive(mainLight.entity) ||
                    !engine.lightManager.hasComponent(mainLight.entity)) {
                    Log.w("Scene3D", "Light entity invalid, skipping update")
                    return
                }

                val lightType = engine.lightManager.getType(mainLight.entity)
                val lightPosition = calculateLightPosition(lightType)
                val lightDirection = (centerNode.worldPosition - lightPosition).normalized()
                
                // Update light properties based on type
                updateLightProperties(lightType, lightPosition, lightDirection)
                
                // Update tracking variables
                lastCameraPosition = cameraNode.worldPosition
                lastLightUpdateFrame = frameCount
                
            } catch (e: Exception) {
                Log.e("Scene3D", "Light update failed: ${e.message}")
            }
        }
    }

    private fun recreateMainLight() {
        synchronized(lightUpdateLock) {
            try {
                isLightBeingRecreated = true
                Log.d("Scene3D", "Starting light recreation")

                // Remove old light
                centerNode.removeChildNode(mainLight)

                // Give a small delay to ensure any pending operations complete
                // Possible race condition with light color? what
                Thread.sleep(10)

                mainLight.destroy()

                // Create new light with current parameters
                mainLight = createMainLight()

                Log.d("Scene3D", "Main light recreated with type: ${parameters.lightType}")
            } catch (e: Exception) {
                Log.e("Scene3D", "Failed to recreate main light: ${e.message}")

                // Try to create a fallback light if recreation failed
                try {
                    mainLight = createMainLight()
                    Log.w("Scene3D", "Fallback light created")
                } catch (fallbackError: Exception) {
                    Log.e("Scene3D", "Failed to create fallback light: ${fallbackError.message}")
                }
            } finally {
                isLightBeingRecreated = false
                framesAfterLightRecreation = 0  // Reset frame counter
                Log.d("Scene3D", "Light recreation completed")
            }
        }
    }

    fun updateSatellites(satelliteList: List<GNSSStatusData>, userLocation: Triple<Float, Float, Float>) {
        val currentSatellitePrns = satelliteList.map { it.prn }.toSet()
        val activePrns = activeSatelliteNodes.keys.toSet()

        // Remove satellites that are no longer visible (return nodes to pool)
        val disappearedSatellites = activePrns - currentSatellitePrns
        disappearedSatellites.forEach { prn ->
            activeSatelliteNodes[prn]?.let { node ->
                centerNode.removeChildNode(node)
                returnNodeToPool(node)
                activeSatelliteNodes.remove(prn)
            }
        }

        // Add or update satellites
        satelliteList.forEach { sat ->
            val existingNode = activeSatelliteNodes[sat.prn]

            if (existingNode != null) {
                // Update position of existing satellite
                updateSatellitePosition(existingNode, sat, userLocation)
            } else {
                // Create or reuse node for new satellite
                val satelliteNode = getOrCreateSatelliteNode()
                setupSatelliteNode(satelliteNode, sat, userLocation)
                activeSatelliteNodes[sat.prn] = satelliteNode
            }
        }

        updateLocationMarker(userLocation)
           
        // Log pool statistics for monitoring
        logPoolStats()
    }

    /**
     * Get a satellite node from pool or create a new one if pool is empty
     */
    private fun getOrCreateSatelliteNode(): ModelNode {
        return if (satelliteNodePool.isNotEmpty()) {
            // Reuse node from pool
            satelliteNodePool.removeAt(satelliteNodePool.size - 1)
        } else {
            // Create new node when pool is empty
            val instance = modelLoader.createModelInstance(parameters.satelliteModelPath)
            ModelNode(
                modelInstance = instance,
                scaleToUnits = parameters.satelliteScale
            )
        }
    }

    /**
     * Return a node to the pool for reuse
     */
    private fun returnNodeToPool(node: ModelNode) {
        // Reset node state before returning to pool
        node.position = Float3(0f, 0f, 0f)
        node.rotation = Float3(0f, 0f, 0f)

        satelliteNodePool.add(node)
    }

    /**
     * Setup a satellite node with position and add to scene
     */
    private fun setupSatelliteNode(node: ModelNode, sat: GNSSStatusData, userLocation: Triple<Float, Float, Float>) {
        updateSatellitePosition(node, sat, userLocation)
        centerNode.addChildNode(node)
    }

    /**
     * Update satellite position without recreating the node
     */
    private fun updateSatellitePosition(node: ModelNode, sat: GNSSStatusData, userLocation: Triple<Float, Float, Float>) {
        val altitude = calculateSatelliteAltitude(sat)
        val pos = CoordinateConversion.ecefToScenePos(
            azElToECEF(
                sat.azimuth,
                sat.elevation,
                userLocation,
                altitude
            )
            )

        if (altitude == 0f) {
            Log.e("SatPos", "Constellation:${sat.constellation} PRN:${sat.prn} position: x:${pos.x}, y:${pos.y}, z:${pos.z}")
        }

        node.position = pos
    }

    private fun calculateSatelliteAltitude(sat: GNSSStatusData): Float {
        return when {
            sat.constellation == "BeiDou" && sat.prn in keysFor35786000 -> 35786000f
            sat.constellation in constellationAltitudes -> constellationAltitudes[sat.constellation]!!
            else -> 0f
        }
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
        val oldParameters = parameters
        parameters = newParameters

        // Update light properties if they changed
        if (oldParameters.lightIntensity != newParameters.lightIntensity ||
            oldParameters.lightColor != newParameters.lightColor ||
            oldParameters.lightFalloff != newParameters.lightFalloff ||
            oldParameters.lightType != newParameters.lightType) {
            recreateMainLight()
            Log.d("Scene3D", "Updated light properties")
        }

        // Handle satellite model path changes
        if (oldParameters.satelliteModelPath != newParameters.satelliteModelPath) {
            updateSatelliteModels()
            Log.d("Scene3D", "Updated satellite models")
        }

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

    private fun updateSatelliteModels() {
        try {
            // Clear current satellites and pool to force recreation with new models
            activeSatelliteNodes.values.forEach { node ->
                centerNode.removeChildNode(node)
                node.destroy()
            }
            activeSatelliteNodes.clear()

            satelliteNodePool.forEach { node ->
                node.destroy()
            }
            satelliteNodePool.clear()

        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to update satellite models: ${e.message}")
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
        cleanupSatellites()
        earthNode?.destroy()
        locationMarkerNode?.let {
            centerNode.removeChildNode(it)
            it.destroy()
        }
        locationMarkerNode = null
        mainLight.destroy()
        cameraNode.destroy()
        centerNode.destroy()
    }

    private fun cleanupSatellites() {
        // Destroy all active satellite nodes
        activeSatelliteNodes.values.forEach { node ->
            centerNode.removeChildNode(node)
            node.destroy()
        }
        activeSatelliteNodes.clear()
        
        // Destroy all pooled satellite nodes
        satelliteNodePool.forEach { node ->
            node.destroy()
        }
        satelliteNodePool.clear()
    }
    
    /**
     * Log pool statistics for debugging and performance monitoring
     */
    private fun logPoolStats() {
        Log.d("ActiveSatellites", "Active satellites: ${activeSatelliteNodes.size}, Pooled nodes: ${satelliteNodePool.size}")
    }

    // Extension functions for Float3 vector operations
    private fun Float3.normalized(): Float3 {
        val length = sqrt(x * x + y * y + z * z)
        return if (length > 0f) {
            Float3(x / length, y / length, z / length)
        } else {
            Float3(0f, 0f, 0f)
        }
    }
    
    private operator fun Float3.plus(other: Float3): Float3 {
        return Float3(x + other.x, y + other.y, z + other.z)
    }
    
    private operator fun Float3.times(scalar: Float): Float3 {
        return Float3(x * scalar, y * scalar, z * scalar)
    }
    
    private fun Float3.length(): Float {
        return sqrt(x * x + y * y + z * z)
    }
}