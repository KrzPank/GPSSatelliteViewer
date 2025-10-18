package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.length
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.abs

// Thresholds to avoid tiny jitter updates
private const val AZIMUTH_THRESHOLD_DEG = 0.1f
private const val ELEVATION_THRESHOLD_DEG = 0.1f

class SatelliteManager(
    private val modelLoader: ModelLoader,
    private val centerNode: Node,
    private val cameraManager: CameraManager,
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

    private var satelliteList: List<GNSSStatusData> = emptyList()

    fun updateSatelliteList(newList: List<GNSSStatusData>) {
        satelliteList = newList
    }

    // Dynamic object pooling for satellite nodes
    private val satelliteNodePool = mutableListOf<ModelNode>()
    val activeSatelliteNodes = mutableMapOf<String, ModelNode>() // "constellation:prn" -> node

    // Cache last-known values per satellite so we only update nodes when something meaningful changed
    private val satelliteCache = mutableMapOf<String, SatelliteCache>()

    private var frameCount = 0
    private val lookAtUpdateInterval = 2
    //private var userLocation = parameters.userLocation //?: Float3(0.0f, 0.0f, 0.0f)

    var onSatelliteClick: ((String) -> Unit)? = null

    private fun satelliteKey(constellation: String, prn: Int) = "$constellation:$prn"
    fun satelliteKey(sat: GNSSStatusData) = satelliteKey(sat.constellation, sat.prn)

    private data class SatelliteCache(
        var lastData: GNSSStatusData,
        var azimuth: Float,
        var elevation: Float,
        var usedInFix: Boolean,
        var altitude: Float,
        var lastPos: Float3? = null
    )

    fun onFrame() {
        updateSatellites()
        shouldUpdateSatelliteLookAt()
    }

    /**
     * Update satellites in the scene
     * Handles adding, removing, and updating satellite positions
     */
    fun updateSatellites() {
        if (parameters.userLocation == null) return
        val currentSatelliteKeys = satelliteList.map { satelliteKey(it) }.toSet()
        val activeKeys = activeSatelliteNodes.keys.toSet()

        val disappearedKeys = activeKeys - currentSatelliteKeys
        disappearedKeys.forEach { key ->
            activeSatelliteNodes[key]?.let { node ->
                centerNode.removeChildNode(node)
                returnNodeToPool(node)
                activeSatelliteNodes.remove(key)
            }
            satelliteCache.remove(key)
        }

        // Add or update satellites
        satelliteList.forEach { sat ->
            val key = satelliteKey(sat)
            val existingNode = activeSatelliteNodes[key]

            if (existingNode != null) {
                // Only update node if the satellite data meaningfully changed (to avoid unnecessary rerenders)
                val cache = satelliteCache[key]
                val newAltitude = calculateSatelliteAltitude(sat)

                val shouldUpdate = if (cache == null) {
                    true
                } else {
                    val azChanged = abs(sat.azimuth - cache.azimuth) > AZIMUTH_THRESHOLD_DEG
                    val elChanged = abs(sat.elevation - cache.elevation) > ELEVATION_THRESHOLD_DEG
                    val usedChanged = sat.usedInFix != cache.usedInFix
                    azChanged || elChanged || usedChanged
                }
                if (shouldUpdate) {
                    updateSatellitePosition(existingNode, sat)
                    //Log.d("SatelliteManager", "Updating satellite:${key}")
                    //Log.d("SatelliteManager", " Info - New:${sat.azimuth}, ${sat.elevation}, ${sat.usedInFix}, ${sat.snr} Old:${cache?.lastData?.azimuth}, ${cache?.lastData?.elevation}, ${cache?.lastData?.usedInFix}, ${cache?.lastData?.snr}")
                    satelliteCache[key] = SatelliteCache(
                        lastData = sat,
                        azimuth = sat.azimuth,
                        elevation = sat.elevation,
                        usedInFix = sat.usedInFix,
                        altitude = newAltitude,
                        lastPos = existingNode.position
                    )
                } else {
                    cache?.lastData = sat
                }
            } else {
                val satelliteNode = getOrCreateSatelliteNode()
                setupSatelliteNode(satelliteNode, sat)
                activeSatelliteNodes[key] = satelliteNode

                val altitude = calculateSatelliteAltitude(sat)
                satelliteCache[key] = SatelliteCache(
                    lastData = sat,
                    azimuth = sat.azimuth,
                    elevation = sat.elevation,
                    usedInFix = sat.usedInFix,
                    altitude = altitude,
                    lastPos = satelliteNode.position
                )
            }
        }
    }

    /**
     * Update satellite look-at behavior to always face camera
     */
    private fun shouldUpdateSatelliteLookAt() {
        frameCount++

        val frameIntervalMet = frameCount>= lookAtUpdateInterval
        val cameraMovement = (cameraManager.getCameraPosition() - cameraManager.getLastCameraPosition()).length()
        val shouldUpdate = frameIntervalMet && cameraMovement > cameraManager.getDynamicUpdateThreshold()

        if (shouldUpdate) {
            frameCount = 0

            updateLookAt()
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
            //Log.d("SatelliteManager", "Updated satellite models")
            updateLookAt()
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
        //node.rotation = Float3(0f, 0f, 0f)
        // clear click handler to avoid capturing stale references
        node.onSingleTapUp = null
        node.name = ""

        satelliteNodePool.add(node)
    }

    /**
     * Setup a satellite node with position and add to scene
     */
    private fun setupSatelliteNode(node: ModelNode, sat: GNSSStatusData) {
        updateSatellitePosition(node, sat)
        centerNode.addChildNode(node)

        // "CONSTELLATION:PRN"
        node.name = satelliteKey(sat)
        node.onSingleTapUp = {
            onSatelliteClick?.invoke(node.name.toString())
            true
        }
    }

    /**
     * Update satellite position without recreating the node
     */
    private fun updateSatellitePosition(node: ModelNode, sat: GNSSStatusData) {
        val altitude = calculateSatelliteAltitude(sat)
        val pos = CoordinateConverter.ecefToScenePos(
            CoordinateConverter.azElToECEF(
                sat.azimuth,
                sat.elevation,
                parameters.userLocation!!,
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

    private fun updateLookAt() {
        activeSatelliteNodes.values.forEach { satellite ->
            satellite.lookAt(cameraManager.getCameraNode().worldPosition)
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
            satelliteCache.clear()

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

        satelliteCache.clear()

        //Log.d("SatelliteManager", "Satellite cleanup completed")
    }
}
