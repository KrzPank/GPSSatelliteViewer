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
        position = Float3(z = -parameters.cameraDistance)
        lookAt(centerNode)
        centerNode.addChildNode(this)
    }

    // Mutable light node that can be recreated when needed
    private var mainLight: LightNode = createMainLight()
    private var isLightBeingRecreated = false

    // Dynamic object pooling for satellite nodes
    private val satelliteNodePool = mutableListOf<ModelNode>() // Reusable nodes from disappeared satellites
    private val activeSatelliteNodes = mutableMapOf<Int, ModelNode>() // PRN -> active ModelNode
    private var earthNode: ModelNode? = null

    var menuVisible: Boolean = true

    init {
        setupScene()
    }

    private fun createMainLight(): LightNode {
        val lightEntity = engine.entityManager.create()
        val pos = cameraNode.worldPosition
        val lightType = when (parameters.lightType) {
            Scene3DParameters.LightType.POINT -> LightManager.Type.POINT
            Scene3DParameters.LightType.DIRECTIONAL -> LightManager.Type.DIRECTIONAL
            Scene3DParameters.LightType.SPOT -> LightManager.Type.SPOT
            Scene3DParameters.LightType.SUN -> LightManager.Type.SUN
        }

        LightManager.Builder(lightType)
            .intensity(parameters.lightIntensity)
            .color(parameters.lightColorRed, parameters.lightColorGreen, parameters.lightColorBlue)
            .falloff(parameters.lightFalloff)
            .position(pos.x, pos.y, pos.z)
            .build(engine, lightEntity)

        return LightNode(engine, lightEntity).apply {
            centerNode.addChildNode(this)
        }
    }

    private fun setupScene() {
        earthNode = ModelNode(
            modelInstance = modelLoader.createModelInstance(parameters.earthModelPath),
            scaleToUnits = parameters.earthScale
        ).also { centerNode.addChildNode(it) }
    }

    @Composable
    fun Render() {
        
        // Apply high preset visual effects when creating the scene
        LaunchedEffect(Unit) {
            applyVisualEffects(view)
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
                updateSatellites()
                if (!isLightBeingRecreated) {
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

    private fun updateSatellites() {
        cameraNode.lookAt(centerNode)
        
        // Update orientation for all active satellite nodes
        activeSatelliteNodes.values.forEach { satellite ->
            satellite.lookAt(cameraNode.worldPosition)
        }
    }

    private fun updateLight() {
        try {
            // Check if the entity is valid before accessing it
            if (!engine.entityManager.isAlive(mainLight.entity)) {
                return
            }

            val lightType = engine.lightManager.getType(mainLight.entity)

            // Direction vector: from camera towards the scene center
            val lightDirection = (centerNode.worldPosition - cameraNode.worldPosition).normalized()
            val pos = cameraNode.worldPosition

            when (lightType) {
                LightManager.Type.DIRECTIONAL,
                LightManager.Type.SUN -> {
                    engine.lightManager.setDirection(
                        mainLight.entity,
                        lightDirection.x,
                        lightDirection.y,
                        lightDirection.z
                    )
                }

                LightManager.Type.SPOT -> {
                    engine.lightManager.setDirection(
                        mainLight.entity,
                        lightDirection.x,
                        lightDirection.y,
                        lightDirection.z
                    )
                    engine.lightManager.setPosition(
                        mainLight.entity,
                        pos.x, pos.y, pos.z
                    )
                }

                LightManager.Type.POINT -> {
                    engine.lightManager.setPosition(
                        mainLight.entity,
                        pos.x, pos.y, pos.z
                    )
                }

                else -> {
                    // Handle unknown types
                }
            }
        } catch (e: Exception) {
            Log.e("Scene3D", "Error updating light: ${e.message}")
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

    private fun toggleMenu() {
        menuVisible = !menuVisible
    }

    private fun calculateSatelliteAltitude(sat: GNSSStatusData): Float {
        return when {
            sat.constellation == "BeiDou" && sat.prn in keysFor35786000 -> 35786000f
            sat.constellation in constellationAltitudes -> constellationAltitudes[sat.constellation]!!
            else -> 0f
        }
    }

    /**
     * Update the scene parameters and apply changes
     */
    fun updateParameters(newParameters: Scene3DParameters) {
        val oldParameters = parameters
        parameters = newParameters

        Log.d("Scene3D", "Updating parameters")

        // Update light properties if they changed
        if (oldParameters.lightIntensity != newParameters.lightIntensity ||
            oldParameters.lightColorRed != newParameters.lightColorRed ||
            oldParameters.lightColorGreen != newParameters.lightColorGreen ||
            oldParameters.lightColorBlue != newParameters.lightColorBlue ||
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

    private fun recreateMainLight() {
        try {
            isLightBeingRecreated = true

            // Remove old light
            centerNode.removeChildNode(mainLight)
            mainLight.destroy()

            // Create new light with current parameters
            mainLight = createMainLight()

            Log.d("Scene3D", "Main light recreated with type: ${parameters.lightType}")
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to recreate main light: ${e.message}")
        } finally {
            isLightBeingRecreated = false
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

            Log.d("Scene3D", "Satellite models updated - all satellites will be recreated")
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to update satellite models: ${e.message}")
        }
    }
    
    /**
     * Apply visual effects based on current parameters (uses high preset by default)
     */
    private fun applyVisualEffects(view: View) {
        try {
            Log.d("Scene3D", "Applying visual effects from parameters")
            
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
}