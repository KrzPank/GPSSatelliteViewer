package com.example.gpssatelliteviewer.scene3d.manager.satellite

import android.util.Log
import com.example.gpssatelliteviewer.data.EPS
import com.example.gpssatelliteviewer.data.SatelliteCache
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.utils.cross
import com.example.gpssatelliteviewer.utils.dot
import com.example.gpssatelliteviewer.utils.length
import com.example.gpssatelliteviewer.utils.sub
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class OrbitManager(
    private val modelLoader: ModelLoader,
    private val centerNode: Node,
    private var parameters: Scene3DParameters
) {
    private var orbitNode: ModelNode? = null
    private var currentOrbitKey: String? = null

    fun getOrbitNode() = orbitNode
    fun getCurrentOrbitKey() = currentOrbitKey

    fun showOrbitForSatellite(
        cache: SatelliteCache?,
        key: String
    ) {
        if (cache == null){
            Log.w("SatelliteManager", "null cache for $key")
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
            clearOrbit()
            Log.d("SatelliteManager", "firstPos == lastPos for $key firstPos $firstPos lastPos ${lastPos}.")
            return
        }

        if (currentOrbitKey == key && orbitNode != null) {
            updateOrbitForCache(cache)
            return
        }

        clearOrbit()

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

    fun clearOrbit() {
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

    fun updateOrbitForCache(cache: SatelliteCache?) {
        val node = orbitNode ?: return
        val c = cache ?: return
        val first = c.firstPos ?: return
        val last = c.lastPos ?: return

        if (first == last) return

        val euler = computeRingRotationEulerDeg(first, last)
        node.rotation = euler

        Log.d("SatelliteManager", "euler=${euler}")
        Log.d("SatelliteManager", "first=${first} last${last}")

        val r1 = first.length()
        val r2 = last.length()
        val radius = (r1 + r2) / 2f

        node.scale = Float3(radius, radius, radius)
    }

    private fun computeRingRotationEulerDeg(
        p2: Float3,
        p3: Float3,
        sourceNormal: Float3 = Float3(0f, 1f, 0f)
    ): Float3 {
        val raw = cross(p2, p3)
        val rawLen = dev.romainguy.kotlin.math.length(raw)
        //if (rawLen <= 1e-8f) return Float3(0f, 0f, 0f)
        if (rawLen <= EPS) return Float3(0f, 0f, 0f)

        val target = normalize(raw)

        var d = dot(sourceNormal, target).coerceIn(-1f, 1f)

        //  special cases
        val quat = if (d > 0.999999f) {
            Log.d("SatelliteManager", "if (d > 0.999999f)")
            doubleArrayOf(1.0, 0.0, 0.0, 0.0)
        } else if (d < -0.999999f) {
            Log.d("SatelliteManager", "else if (d < -0.999999f)")
            var axis = cross(sourceNormal, Float3(1f, 0f, 0f))
            if (dev.romainguy.kotlin.math.length(axis) <= 1e-6f) axis = cross(sourceNormal, Float3(0f, 1f, 0f))
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
    private fun axisAngleToQuat(
        axis: Float3,
        angleRad: Float
    ): DoubleArray {
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
}
