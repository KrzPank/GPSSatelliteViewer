package com.example.gpssatelliteviewer.scene3d.manager.satellite

import android.util.Log
import android.os.SystemClock
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.SatelliteCache
import com.example.gpssatelliteviewer.scene3d.manager.camera.CameraManager
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.material.setBaseColorFactor
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.pow

class AuraManager(
    private val modelLoader: ModelLoader,
    private val centerNode: Node,
    private val cameraManager: CameraManager
) {
    private val auraNodes = mutableMapOf<String, ModelNode>()

    private data class PulseController(val auraNode: ModelNode)
    private val auraPulseControllers = mutableMapOf<String, PulseController>()
    private val auraPulseKeys = mutableSetOf<String>()
    private val auraColorForKey = mutableMapOf<String, Float4>()

    private val AURA_MODEL_PATH = "models/RedCircle.glb"

    fun removeAura(key: String) {
        stopPulseAnimation(key)
        auraNodes[key]?.let {
            aura -> aura.parent?.removeChildNode(aura)
            aura.destroy()
            auraNodes.remove(key)
            auraColorForKey.remove(key)
        }
    }

    fun updateLookAt() {
        val cameraPos = cameraManager.getCameraPosition()
        auraNodes.values.forEach { it.lookAt(cameraPos) }
    }

    fun updateAuraFor(key: String, cache: SatelliteCache) {
        val snrValue = cache.currentSNR
        val existingAura = auraNodes[key]

        val desiredColor = getColorForSNR(snrValue)
        if (existingAura != null) {
            existingAura.position = cache.lastPos!!
            try {
                existingAura.materialInstances.forEach { matInst ->
                    matInst.forEach { material ->
                        material.setBaseColorFactor(desiredColor)
                    }
                }
                auraColorForKey[key] = desiredColor
            } catch (e: Exception) {
                Log.w("AuraManager", "Failed to update material color for $key: ${e.message}")
            }

            if (!auraPulseKeys.contains(key)) {
                //startPulseAnimation(existingAura, key)
            }

            return
        }

        try {
            Log.d("AuraManager", "Added aura node for $key")
            val instance = modelLoader.createModelInstance(AURA_MODEL_PATH)
            val auraNode = ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.2f
            )
            auraNode.name = "$key:aura"
            auraNode.position = cache.lastPos!!

            auraNode.materialInstances.forEach { matInst ->
                matInst.forEach { material ->
                    material.setBaseColorFactor(desiredColor)
                }
            }

            centerNode.addChildNode(auraNode)

            //Log.d("AuraManager", "Creating aura for $key at pos=${cache.lastPos}, path=$desiredModelPath")

            auraNodes[key] = auraNode
            auraColorForKey[key] = desiredColor
            auraNode.lookAt(cameraManager.getCameraPosition())

            //startPulseAnimation(auraNode, key)
        } catch (e: Exception) {
            Log.e("AuraManager", "Failed to create aura node for $key using $AURA_MODEL_PATH: ${e.message}")
        }

        //Log.d("AuraManager", "All keys:$auraPulseKeys ")
    }

    // returns RGB color with alpha=1f for a given SNR
    private fun getColorForSNR(snr: Float): Float4 {
        return when {
            snr <= 5f -> Float4(0.89f, 0.18f, 0.14f, 1f) // red-ish (#E42E23)
            snr <= 15f -> Float4(0.97f, 0.55f, 0.09f, 1f) // orange-ish (#F78C18)
            snr <= 25f -> Float4(0.98f, 0.87f, 0.16f, 1f) // yellow-ish (#FBE02A)
            else -> Float4(0.16f, 0.74f, 0.33f, 1f)       // green-ish (#28BD55)
        }
    }

    // problem with something
    // some animations pulsate and some disappear with time and changes
    private fun startPulseAnimation(
        auraNode: ModelNode,
        key: String,
        targetScale: Float = 0.2f,
        growMs: Long = 2500L,
        holdMs: Long = 300L,
        fadeMs: Long = 150L
    ) {
        auraPulseControllers[key] = PulseController(auraNode)
        auraPulseKeys.add(key)
        auraNode.lookAt(cameraManager.getCameraPosition())

        val cycleMs = growMs + holdMs + fadeMs
        val startTime = SystemClock.uptimeMillis()

        Log.d("AuraManager", "startPulseAnimation[$key]")
        auraNode.onFrame = { frameTimeNanos ->
            val nowMs = frameTimeNanos / 1_000_000L
            val elapsed = (nowMs - startTime) % cycleMs

            val progress = when {
                elapsed < growMs -> {
                    val p = elapsed.toFloat() / growMs
                    p
                }
                elapsed < growMs + holdMs -> 1f
                else -> {
                    val t = (elapsed - growMs - holdMs).toFloat() / fadeMs
                    val eased = 1f - t.pow(2f)
                    eased
                }
            }

            val s = targetScale * progress
            auraNode.scaleToUnitCube(s)
        }
    }

    private fun stopPulseAnimation(key: String) {
        auraPulseControllers.remove(key)?.auraNode?.onFrame = null
        auraPulseKeys.remove(key)
    }

    fun cleanup() {
        auraPulseKeys.toList().forEach { stopPulseAnimation(it) }
        auraNodes.values.forEach { it.parent?.removeChildNode(it); it.destroy() }
        auraNodes.clear()
        auraColorForKey.clear()
    }
}
