package com.example.gpssatelliteviewer.scene3d

import android.util.Log
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.Scene3DParameters
import com.example.gpssatelliteviewer.utils.CoordinateConversion
import com.example.gpssatelliteviewer.utils.CoordinateConversion.azElToECEF
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

/**
 * Manages GNSS satellite positioning, node pooling, and lifecycle
 * Handles efficient satellite visualization with object pooling
 */
class SatelliteManager(
    private val modelLoader: ModelLoader,
    private val centerNode: Node,
    private var parameters: Scene3DParameters
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

    // Dynamic object pooling for satellite nodes
    private val satelliteNodePool = mutableListOf<ModelNode>() // Reusable nodes from disappeared satellites
    private val activeSatelliteNodes = mutableMapOf<Int, ModelNode>() // PRN -> active ModelNode

    /**
     * Update satellites in the scene
     * Handles adding, removing, and updating satellite positions
     */
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
     * Update satellite look-at behavior to always face camera
     */
    fun updateLookAt(cameraNode: CameraNode) {
        // Update orientation for all active satellite nodes
        activeSatelliteNodes.values.forEach { satellite ->
            satellite.lookAt(cameraNode.worldPosition)
        }
    }

    /**
     * Update satellite model parameters when they change
     */
    fun updateParameters(newParameters: Scene3DParameters) {
        val oldParameters = parameters
        parameters = newParameters
        
        // Handle satellite model path changes
        if (oldParameters.satelliteModelPath != newParameters.satelliteModelPath ||
            oldParameters.satelliteScale != newParameters.satelliteScale) {
            updateSatelliteModels()
            Log.d("SatelliteManager", "Updated satellite models")
        }
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

    /**
     * Calculate satellite altitude based on constellation and PRN
     */
    private fun calculateSatelliteAltitude(sat: GNSSStatusData): Float {
        return when {
            sat.constellation == "BeiDou" && sat.prn in keysFor35786000 -> 35786000f
            sat.constellation in constellationAltitudes -> constellationAltitudes[sat.constellation]!!
            else -> 0f
        }
    }

    /**
     * Update satellite models when parameters change
     */
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
            Log.e("SatelliteManager", "Failed to update satellite models: ${e.message}")
        }
    }

    /**
     * Cleanup all satellite resources
     */
    fun cleanup() {
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
        
        Log.d("SatelliteManager", "Satellite cleanup completed")
    }

    /**
     * Log pool statistics for debugging and performance monitoring
     */
    private fun logPoolStats() {
        Log.d("SatelliteManager", "Active satellites: ${activeSatelliteNodes.size}, Pooled nodes: ${satelliteNodePool.size}")
    }

    /**
     * Get current statistics about satellite management
     */
    fun getStats(): SatelliteStats {
        return SatelliteStats(
            activeSatellites = activeSatelliteNodes.size,
            pooledNodes = satelliteNodePool.size,
            totalConstellations = activeSatelliteNodes.values
                .mapNotNull { node -> 
                    // This would require storing constellation info with the node
                    // For now, return approximate count based on typical distributions
                    null
                }.distinct().size
        )
    }
    
    /**
     * Data class for satellite statistics
     */
    data class SatelliteStats(
        val activeSatellites: Int,
        val pooledNodes: Int,
        val totalConstellations: Int
    )
}