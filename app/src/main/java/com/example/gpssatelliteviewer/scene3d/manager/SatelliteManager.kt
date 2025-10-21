package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import kotlin.math.asin
import kotlin.math.atan2
import com.example.gpssatelliteviewer.data.AzElHistory
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.frameCountUpdateInterval
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.scene3d.manager.camera.CameraManager
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.cross
import com.example.gpssatelliteviewer.utils.length
import com.example.gpssatelliteviewer.utils.sub
import com.example.gpssatelliteviewer.utils.dot
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.length
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val AZIMUTH_THRESHOLD_DEG = 0.1f
private const val ELEVATION_THRESHOLD_DEG = 0.1f

private const val EPS = 1e-3f

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

    private val satelliteNodePool = mutableListOf<ModelNode>()
    val activeSatelliteNodes = mutableMapOf<String, ModelNode>() // "constellation:prn" -> node

    private val satelliteCache = mutableMapOf<String, SatelliteCache>()

    private var frameCount = 0
    private var firstLookAt = true

    var onSatelliteClick: ((String) -> Unit)? = null

    private var orbitNode: ModelNode? = null
    private var currentOrbitKey: String? = null

    private fun satelliteKey(constellation: String, prn: Int) = "$constellation:$prn"
    fun satelliteKey(sat: GNSSStatusData) = satelliteKey(sat.constellation, sat.prn)

    private data class SatelliteCache(
        var lastData: GNSSStatusData,
        var usedInFix: Boolean,
        var altitude: Float,
        var firstPos: Float3? = null,
        var lastPos: Float3? = null
    )

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

            if (currentOrbitKey == key) {
                hideOrbit()
            }
            satelliteCache.remove(key)
        }

        // Add or update satellites
        satelliteList.forEach { sat ->
            val key = satelliteKey(sat)
            val existingNode = activeSatelliteNodes[key]

            if (existingNode != null) {
                val cache = satelliteCache[key]
                val satAzEl = azElHistory[key]!!

                val shouldUpdate = if (cache == null) {
                    true
                } else {
                    val azChanged = abs(sat.azimuth - satAzEl.lastAz) > AZIMUTH_THRESHOLD_DEG
                    val elChanged = abs(sat.elevation - satAzEl.lastEl) > ELEVATION_THRESHOLD_DEG
                    val usedChanged = sat.usedInFix != cache.usedInFix
                    azChanged || elChanged || usedChanged
                }
                // Only update node if the satellite data meaningfully changed (to avoid unnecessary rerenders)
                if (shouldUpdate) {
                    val newAltitude = calculateSatelliteAltitude(sat)
                    updateSatellitePosition(existingNode, sat)
                    //Log.d("SatelliteManager", "Updating satellite:${key} cache=${cache}")
                    cache?.lastData = sat
                    cache?.usedInFix = sat.usedInFix
                    cache?.altitude = newAltitude
                    cache?.lastPos = existingNode.position

                    Log.d("SatelliteManager", "Updating satellite:${key}, firstPos=${cache?.firstPos} lastPos=${cache?.lastPos}")
                    // If this satellite currently has orbit shown, update orbit orientation/scale
                    if (currentOrbitKey == key) {
                        updateOrbitForCache(satelliteCache[key])
                    }
                } else {
                    cache?.lastData = sat
                }
            } else {
                val satelliteNode = getOrCreateSatelliteNode()
                setupSatelliteNode(satelliteNode, sat)
                activeSatelliteNodes[key] = satelliteNode
                // compute initial altitude and position so we can set both first and last
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
                    lastData = sat,
                    usedInFix = sat.usedInFix,
                    altitude = altitude,
                    firstPos = pos,
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

    fun showOrbitForSatellite(key: String) {
        val cache = satelliteCache[key]
        if (cache == null) {
            Log.w("SatelliteManager", "No cache for satellite $key, cannot show orbit")
            return
        }

        val firstPos = cache.firstPos
        val lastPos = cache.lastPos
        if (firstPos == null || lastPos == null) {
            Log.w("SatelliteManager", "First or last position missing for $key")
            return
        }

        val diffLen = sub(firstPos, lastPos).length()
        if (diffLen < EPS) {
            hideOrbit()
            Log.d("SatelliteManager", "firstPos == lastPos for $key firstPos $firstPos lastPos ${lastPos}.")
            return
        }

        if (currentOrbitKey == key && orbitNode != null) {
            updateOrbitForCache(cache)
            return
        }

        hideOrbit()

        try {
            val ringInstance = modelLoader.createModelInstance(parameters.orbitModelPath)
            val ringNode = ModelNode(
                modelInstance = ringInstance,
                scaleToUnits = 1f,
            )

            //ringNode.name = key
            ringNode.position = Float3(0f, 0f, 0f)

            centerNode.addChildNode(ringNode)
            orbitNode = ringNode
            currentOrbitKey = key

            updateOrbitForCache(cache)
        } catch (e: Exception) {
            Log.e("SatelliteManager", "Failed to load orbit model ${parameters.orbitModelPath}: ${e.message}")
        }
    }

    fun hideOrbit() {
        orbitNode?.let { node ->
            try {
                centerNode.removeChildNode(node)
                node.destroy()
            } catch (e: Exception) {
                Log.e("SatelliteManager", "Failed to remove orbit node: ${e.message}")
            }
        }
        orbitNode = null
        currentOrbitKey = null
    }

    private fun updateOrbitForCache(cache: SatelliteCache?) {
        val node = orbitNode ?: return
        val c = cache ?: return
        val first = c.firstPos ?: return
        val last = c.lastPos ?: return

        if (first == last) return

        val euler = computeRingRotationEulerDeg(first, last)
        node.rotation = euler

        Log.d("SatelliteManager", "euler=${euler}")
        Log.d("SatelliteManager", "first=${first} last${last}")
        //val alignment = verifyRingAlignment(first, last)
        //Log.d("SatelliteManager", "plane alignment dot = $alignment") // should be ~1.0 when aligned

        val r1 = first.length()
        val r2 = last.length()
        val radius = (r1 + r2) / 2f

        node.scale = Float3(radius, radius, radius)
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
            satellite.lookAt(cameraManager.getCameraPosition())
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

        hideOrbit()
    }
}

private fun computeRingRotationEulerDeg(p2: Float3, p3: Float3, sourceNormal: Float3 = Float3(0f, 1f, 0f)): Float3 {
    val raw = cross(p2, p3)
    val rawLen = length(raw)
    //if (rawLen <= 1e-8f) return Float3(0f, 0f, 0f) // degenerate
    if (rawLen <= EPS) return Float3(0f, 0f, 0f) // degenerate

    val target = normalize(raw)

    var d = dot(sourceNormal, target).coerceIn(-1f, 1f)

    //  special cases
    val quat = if (d > 0.999999f) {
        Log.d("SatelliteManager", "if (d > 0.999999f)")
        doubleArrayOf(1.0, 0.0, 0.0, 0.0)
    } else if (d < -0.999999f) {
        Log.d("SatelliteManager", "else if (d < -0.999999f)")
        var axis = cross(sourceNormal, Float3(1f, 0f, 0f))
        if (length(axis) <= 1e-6f) axis = cross(sourceNormal, Float3(0f, 1f, 0f))
        axis = normalize(axis)
        axisAngleToQuat(axis, Math.PI.toFloat())
    } else {
        Log.d("SatelliteManager", "else")
        val axis = normalize(cross(sourceNormal, target))
        val angle = acos(d.toDouble()).toFloat()
        axisAngleToQuat(axis, angle)
    }

    return quatToEulerDeg(quat)
}

/** axis-angle -> quaternion (w,x,y,z) */
private fun axisAngleToQuat(axis: Float3, angleRad: Float): DoubleArray {
    val ha = angleRad / 2.0
    val s = sin(ha)
    val w = cos(ha)
    val x = axis.x * s
    val y = axis.y * s
    val z = axis.z * s

    val len = sqrt(w*w + x*x + y*y + z*z)
    return doubleArrayOf(w/len, x/len, y/len, z/len)
}

private fun quatToEulerDeg(q: DoubleArray): Float3 {
    val w = q[0]; val x = q[1]; val y = q[2]; val z = q[3]

    val sinr_cosp = 2.0 * (w * x + y * z)
    val cosr_cosp = 1.0 - 2.0 * (x * x + y * y)
    val roll = atan2(sinr_cosp, cosr_cosp)

    val sinp = 2.0 * (w * y - z * x)
    val pitch = when {
        sinp >= 1.0 -> Math.PI / 2.0
        sinp <= -1.0 -> -Math.PI / 2.0
        else -> asin(sinp)
    }

    val siny_cosp = 2.0 * (w * z + x * y)
    val cosy_cosp = 1.0 - 2.0 * (y * y + z * z)
    val yaw = atan2(siny_cosp, cosy_cosp)

    fun to360(d: Double): Float {
        var deg = Math.toDegrees(d)
        deg = ((deg % 360.0) + 360.0) % 360.0
        return deg.toFloat()
    }
    return Float3(to360(roll), to360(pitch), to360(yaw))
}
