package com.example.gpssatelliteviewer.utils

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.utils.CoordinateConversion.azElToECEF
import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.managers.setPosition
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberOnGestureListener
import kotlin.math.sqrt

// Extension functions for Float3 vector operations
private fun Float3.normalized(): Float3 {
    val length = sqrt(x * x + y * y + z * z)
    return if (length > 0f) {
        Float3(x / length, y / length, z / length)
    } else {
        Float3(0f, 0f, 0f)
    }
}

class Scene3D(
    private val modifier: Modifier = Modifier
        .fillMaxSize(),
    private val environmentLoader: EnvironmentLoader,
    private val modelLoader: ModelLoader,
    private val engine: Engine
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
        position = Float3(z = -3.0f)
        lookAt(centerNode)
        centerNode.addChildNode(this)
    }

    // Camera-following light for consistent satellite illumination
    private val mainLight: LightNode = run {
        val lightEntity = engine.entityManager.create()
        val pos = cameraNode.worldPosition
        LightManager.Builder(LightManager.Type.POINT)
            .intensity(10_000_000.0f)     // stronger, since point light falls off
            .color(1.0f, 1.0f, 1.0f)
            .falloff(1000.0f)
            .position(pos.x, pos.y, pos.z)
            .build(engine, lightEntity)

        LightNode(engine, lightEntity).apply {
            centerNode.addChildNode(this)
        }
    }

    // Dynamic object pooling for satellite nodes
    private val satelliteNodePool = mutableListOf<ModelNode>() // Reusable nodes from disappeared satellites
    private val activeSatelliteNodes = mutableMapOf<Int, ModelNode>() // PRN -> active ModelNode
    private var earthNode: ModelNode? = null

    private val earthModelPath = "models/material_test_noQuad.glb"
    private val satelliteModelPath = "models/RedCircle.glb"
    private val environmentPath = "envs/8k_stars_milky_way.hdr"//"envs/sky_2k.hdr"//"envs/8k_stars_milky_way.hdr"

    var menuVisible: Boolean = true

    init {
        setupScene()
    }

    private fun setupScene() {
        earthNode = ModelNode(
            modelInstance = modelLoader.createModelInstance(earthModelPath),
            scaleToUnits = 1.0f
        ).also { centerNode.addChildNode(it) }
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
            environment = environmentLoader.createHDREnvironment(environmentPath)!!,
            onFrame = { 
                updateCameraAndSatellites()
                updateLight()
            },
            mainLightNode = mainLight,
            onGestureListener = rememberOnGestureListener (
                onDoubleTap = {_, node ->
                    toggleMenu()
                }
            )
        )
    }

    private fun updateCameraAndSatellites() {
        cameraNode.lookAt(centerNode)
        
        // Update orientation for all active satellite nodes
        activeSatelliteNodes.values.forEach { satellite ->
            satellite.lookAt(cameraNode.worldPosition)
        }
    }

    private fun updateLight() {
        val lightType = engine.lightManager.getType(mainLight.entity)

        // Direction vector: from camera towards the scene center
        val lightDirection = (centerNode.worldPosition - cameraNode.worldPosition).normalized()

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
                    cameraNode.worldPosition
                )
            }

            LightManager.Type.POINT -> {
                engine.lightManager.setPosition(
                    mainLight.entity,
                    cameraNode.worldPosition
                )
            }

            else -> {
                // Optional: handle other types if needed
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
            val instance = modelLoader.createModelInstance(satelliteModelPath)
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.05f
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
}