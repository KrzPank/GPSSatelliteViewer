package com.example.gpssatelliteviewer.scene3d.manager.satellite

import android.util.Log
import com.example.gpssatelliteviewer.data.AzElHistory
import com.example.gpssatelliteviewer.data.FAIR_SNR
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.GOOD_SNR
import com.example.gpssatelliteviewer.data.NO_SNR
import com.example.gpssatelliteviewer.data.POOR_SNR
import com.example.gpssatelliteviewer.data.SatelliteCache
import com.example.gpssatelliteviewer.data.frameCountUpdateInterval
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.scene3d.manager.camera.CameraManager
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.material.setBaseColorFactor
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.abs

private const val AZIMUTH_THRESHOLD_DEG = 0.2f
private const val ELEVATION_THRESHOLD_DEG = 0.2f

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

    private var azElHistory: Map<String, AzElHistory> = emptyMap()

    private val orbitManager: OrbitManager = OrbitManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
        parameters = parameters
    )

    private val satelliteNodePool = mutableListOf<ModelNode>()
    val activeSatelliteNodes = mutableMapOf<String, ModelNode>() // "constellation:prn" -> node

    private val satelliteCache = mutableMapOf<String, SatelliteCache>()

    private var frameCount = 0
    private var firstLookAt = true

    private var clickedSatelliteKey: String? = null

    //var onSatelliteClick: ((String) -> Unit)? = null

    private fun satelliteKey(constellation: String, prn: Int) = "$constellation:$prn"
    fun satelliteKey(sat: GNSSStatusData) = satelliteKey(sat.constellation, sat.prn)

    fun onFrame() {
        shouldUpdateSatelliteLookAt()
    }

    /**
     * Update satellites in the scene
     * Handles adding, removing, and updating satellite positions
     */
    fun updateSatelliteList(newList: List<GNSSStatusData>, azElH: Map<String, AzElHistory>) {
        //Log.d("SatelliteManager", "azELhistory for GPS:25=${azElHistory["GPS:16"]}")
        satelliteList = newList
        azElHistory = azElH

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

            if (orbitManager.getCurrentOrbitKey() == key) {
                orbitManager.clearOrbit()
            }
            satelliteCache.remove(key)
        }

        // Add or update satellites
        satelliteList.forEach { sat ->
            val key = satelliteKey(sat)
            val existingNode = activeSatelliteNodes[key]

            if (existingNode != null) {
                val cache = satelliteCache[key]!!
                val satAzEl = azElHistory[key]!!

                val azChanged = abs(cache.currentAz - satAzEl.lastAz) > AZIMUTH_THRESHOLD_DEG
                val elChanged = abs(cache.currentEl - satAzEl.lastEl) > ELEVATION_THRESHOLD_DEG
                val usedChanged = sat.usedInFix != cache.usedInFix

                val shouldUpdate = azChanged || elChanged || usedChanged

                val prevSNR = cache.currentSNR
                val bucketChanged = snrBucket(prevSNR) != snrBucket(sat.cn0DbHz)
                val shouldUpdateColor = usedChanged || bucketChanged

                if (shouldUpdate) {
                    //Log.d("SatelliteManager", "for sat:${key}  sat Az/El:${cache?.currentAz}/${cache?.currentEl}  azElHist:${satAzEl.lastAz}/${satAzEl.lastEl}")
                    val newAltitude = calculateSatelliteAltitude(sat)
                    updateSatellitePosition(existingNode, sat)

                    cache.usedInFix = sat.usedInFix
                    cache.altitude = newAltitude
                    cache.currentAz = satAzEl.lastAz
                    cache.currentEl = satAzEl.lastEl
                    cache.lastPos = existingNode.position
                    cache.currentSNR = sat.cn0DbHz

                    if (shouldUpdateColor) {
                        updateColor(key, existingNode)
                    }

                    if (orbitManager.getCurrentOrbitKey() == key) {
                        orbitManager.updateOrbitForCache(satelliteCache[key])
                    }
                } else {
                    // If we don't need to update position, we may still need to update color (bucket changed or used flag changed)
                    if (shouldUpdateColor) {
                        // update the cache SNR first, then color
                        cache.currentSNR = sat.cn0DbHz
                        cache.usedInFix = sat.usedInFix
                        updateColor(key, existingNode)
                    } else {
                        // No visual changes required — still keep SNR in cache up to date for next comparison
                        cache.currentSNR = sat.cn0DbHz
                        cache.usedInFix = sat.usedInFix
                    }
                }
            } else {
                val satelliteNode = getOrCreateSatelliteNode()
                setupSatelliteNode(satelliteNode, sat)
                activeSatelliteNodes[key] = satelliteNode

                val altitude = calculateSatelliteAltitude(sat)
                val key = satelliteKey(sat)
                val azEl = azElHistory[key]!!
                val pos = CoordinateConverter.ecefToScenePos(
                    CoordinateConverter.azElToECEF(
                        azEl.firstAz,
                        azEl.firstEl,
                        parameters.userLocation!!,
                        altitude
                    )
                )

                satelliteCache[key] = SatelliteCache(
                    usedInFix = sat.usedInFix,
                    currentSNR = sat.cn0DbHz,
                    altitude = altitude,
                    currentAz = azEl.firstAz,
                    currentEl = azEl.firstEl,
                    firstPos = pos,
                    lastPos = satelliteNode.position
                )

                updateColor(key, satelliteNode)
            }
        }
    }

    fun setClickedSatelliteKey(key: String?) {
        val previousKey = clickedSatelliteKey
        clickedSatelliteKey = key

        // Recolor previous clicked satellite (revert to SNR color) if present
        previousKey?.let { prev ->
            activeSatelliteNodes[prev]?.let { node ->
                updateColor(prev, node)
            }
        }

        // Color the newly clicked satellite blue if present
        key?.let { newKey ->
            activeSatelliteNodes[newKey]?.let { node ->
                updateColor(newKey, node)
            }
        }
    }

    private fun updateColor(key: String, node: ModelNode) {
        if (key == clickedSatelliteKey) {
            val blue = Float4(0.1f, 0.4f, 1.0f, 1f)
            try {
                node.materialInstances.forEach { matInst ->
                    matInst.forEach { material ->
                        material.setBaseColorFactor(blue)
                    }
                }
            } catch (e: Exception) {
                Log.w("SatelliteManager", "Failed to update material color for (clicked) $key: ${e.message}")
            }
            return
        }

        val snr = satelliteCache[key]?.currentSNR ?: NO_SNR
        val desiredColor = getColorForSNR(snr)

        try {
            node.materialInstances.forEach { matInst ->
                matInst.forEach { material ->
                    material.setBaseColorFactor(desiredColor)
                }
            }
        } catch (e: Exception) {
            Log.w("SatelliteManager", "Failed to update material color for $key: ${e.message}")
        }
    }

    /**
     * Update satellite look-at behavior to always face camera
     */
    private fun shouldUpdateSatelliteLookAt() {
        frameCount++

        if (firstLookAt) {
            firstLookAt = false
            updateLookAt()
        }

        val frameIntervalMet = frameCount >= frameCountUpdateInterval
        val cameraMovement = cameraManager.getCameraMovedUnits()
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

        if (oldParameters.satelliteModelPath != newParameters.satelliteModelPath ||
            oldParameters.satelliteScale != newParameters.satelliteScale) {
            updateSatelliteModels()
            updateLookAt()
        }
    }

    /**
     * Get a satellite node from pool or create a new one if pool is empty
     */
    private fun getOrCreateSatelliteNode(): ModelNode {
        return if (satelliteNodePool.isNotEmpty()) {
            // Reuse node from pool
            Log.d("SatelliteManager", "Reused satellite")
            satelliteNodePool.removeAt(satelliteNodePool.size - 1)
        } else {
            Log.d("SatelliteManager", "Created sat node")
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
        node.position = Float3(0f, 0f, 0f)
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
    }

    /**
     * Update satellite position without recreating the node
     */
    private fun updateSatellitePosition(node: ModelNode, sat: GNSSStatusData) {
        val altitude = calculateSatelliteAltitude(sat)
        val key = satelliteKey(sat)
        val azEl = azElHistory[key]!!
        val pos = CoordinateConverter.ecefToScenePos(
            CoordinateConverter.azElToECEF(
                azEl.lastAz,
                azEl.lastEl,
                parameters.userLocation!!,
                altitude
            )
        )

        if (altitude == 0f) {
            Log.e("SatPos", "Constellation:${sat.constellation} PRN:${sat.prn} position: x:${pos.x}, y:${pos.y}, z:${pos.z}")
        }

        node.position = pos
        node.lookAt(cameraManager.getCameraPosition())
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
            satellite.lookAt(cameraManager.getCameraPosition())   // lookAt camera
        }
    }

    private fun getColorForSNR(snr: Float): Float4 {
        return when {
            snr == NO_SNR -> Float4(1.0f, 0.0f, 0.0f, 1f)
            snr <= POOR_SNR -> Float4(0.89f, 0.18f, 0.14f, 1f) // red-ish (#E42E23)
            snr <= FAIR_SNR -> Float4(1.0f, 0.65f, 0.0f, 1f)  // orange-ish (#F78C18)
            snr <= GOOD_SNR -> Float4(0.97f, 0.97f, 0.0f, 1f) // yellow-ish (#FBE02A)
            else -> Float4(0.0f, 1.0f, 0.4f, 1f)        // green-ish (#28BD55)
        }
    }

    private fun snrBucket(snr: Float): Int = when {
        snr == NO_SNR -> 0
        snr <= POOR_SNR -> 1
        snr <= FAIR_SNR -> 2
        snr <= GOOD_SNR -> 3
        else -> 4
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
     * Orbit handling
     */
    fun showOrbitForSatellite(key: String) {
        val cache = satelliteCache[key]
        orbitManager.showOrbitForSatellite(cache, key)
    }

    fun clearOrbit() {
        orbitManager.clearOrbit()
    }

    /**
     * Cleanup all satellite resources
     */
    fun cleanup() {
        activeSatelliteNodes.values.forEach { node ->
            centerNode.removeChildNode(node)
            node.destroy()
        }
        satelliteNodePool.forEach { node ->
            node.destroy()
        }
        activeSatelliteNodes.clear()
        satelliteNodePool.clear()
        satelliteCache.clear()
        orbitManager.clearOrbit()
    }
}
