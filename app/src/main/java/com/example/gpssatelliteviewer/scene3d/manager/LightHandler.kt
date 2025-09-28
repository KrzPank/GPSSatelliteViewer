package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import com.example.gpssatelliteviewer.data.LightParameters
import com.example.gpssatelliteviewer.data.Scene3DParameters
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import kotlin.math.sqrt

class LightHandler(
    private val engine: Engine,
    private val centerNode: Node,
    private val cameraNode: CameraNode,
    initialParameters: Scene3DParameters
) {
    private var sunLight: LightNode = createSunLight(initialParameters.getLightParameters())

    private var currentLightParameters: LightParameters = initialParameters.getLightParameters()

    // Light update optimization
    private var lastCameraPosition = Float3(0f, 0f, 0f)
    private var isLightBeingRecreated = false

    // Light recreation throttling
    private var lastRecreationTime = 0L
    private val minRecreationInterval = 16L // Minimum 16ms between recreations /60Hz
    private var frameCount = 0
    private val lightUpdateInterval = 2
    private val lightUpdateThreshold = 0.08f

    /**
     * Create a sun light from behind the camera
     */
    private fun createSunLight(lightParams: LightParameters): LightNode {
        val lightEntity = EntityManager.get().create()

        // Sun light direction: from camera towards center (behind camera illuminating forward)
        //val cameraToCenter = (centerNode.worldPosition - cameraNode.worldPosition).normalized()
        //val lightDirection = cameraToCenter

        // Calculate optimal light direction for good sphere shading
        val lightDirection = calculateOptimalLightDirection()

        val builder = LightManager.Builder(lightParams.type)
            .intensity(lightParams.intensity)
            .color(lightParams.color.x, lightParams.color.y, lightParams.color.z)
            .direction(lightDirection.x, lightDirection.y, lightDirection.z)

        builder.build(engine, lightEntity)

        val lightNode = LightNode(engine, lightEntity).apply {
            centerNode.addChildNode(this)
        }

        Log.d("Scene3D", "Sun light created from behind camera - color: ${lightParams.color.x}, ${lightParams.color.y}, ${lightParams.color.z}, direction: ${lightDirection.x}, ${lightDirection.y}, ${lightDirection.z}")
        isLightBeingRecreated = false
        return lightNode
    }

    /**
     * Recreate sun light with new parameters
     */
    private fun recreateSunLight(lightParams: LightParameters = currentLightParameters) {
        if (isLightBeingRecreated) {
            Log.d("Scene3D", "Light recreation already in progress, skipping")
            return
        }
        isLightBeingRecreated = true

        // Remove and destroy old light
        centerNode.removeChildNode(sunLight)
        val oldEntityId = sunLight.entity

        if (engine.entityManager.isAlive(oldEntityId) &&
            engine.lightManager.hasComponent(oldEntityId)
        ) {
            engine.lightManager.destroy(oldEntityId)
        }
        sunLight.destroy()

        // Create new sun light
        sunLight = createSunLight(lightParams)
    }

    /**
     * Update sun light direction to follow camera
     */
    private fun updateSunLight() {
        frameCount++

        // Early exit if light is being recreated
        if (isLightBeingRecreated) return

        // Check if enough frames have passed and camera moved
        val frameIntervalMet = frameCount>= lightUpdateInterval
        val cameraMovement = (cameraNode.worldPosition - lastCameraPosition).length()
        val shouldUpdate = frameIntervalMet && cameraMovement > lightUpdateThreshold

        if (shouldUpdate) {
            // Update tracking variables before recreation
            lastCameraPosition = cameraNode.worldPosition
            frameCount = 0

            recreateSunLight()

            Log.d("Scene3D", "Updated sun light direction due to camera movement, frameCount $frameCount")
        }
    }

    fun updateParameters(newParameters: Scene3DParameters) {
        val newLightParameters = newParameters.getLightParameters()

        // Check if light parameters have actually changed
        if (currentLightParameters == newLightParameters) {
            Log.d("Scene3D", "Light parameters unchanged, skipping recreation")
            return
        }

        // Prevent rapid recreation while already recreating
        if (isLightBeingRecreated) {
            Log.d("Scene3D", "Skipping updateParameters - light already being recreated")
            return
        }

        // Time-based throttling to prevent too frequent recreations
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRecreationTime < minRecreationInterval) {
            Log.d("Scene3D", "Skipping updateParameters - too soon (${currentTime - lastRecreationTime}ms < ${minRecreationInterval}ms)")
            return
        }

        Log.d("Scene3D", "Light parameters changed - recreating light with intensity: ${newLightParameters.intensity}, color: ${newLightParameters.color}")
        lastRecreationTime = currentTime
        currentLightParameters = newLightParameters

        recreateSunLight(currentLightParameters)
    }

    /**
     * Called each frame - update sun light direction when camera moves
     */
    fun onFrame() {
        updateSunLight()
    }

    /**
     * Calculate optimal light direction for sphere shading
     * Creates a light that illuminates from the upper-right relative to camera view,
     * leaving shadows on the left side of spheres for better 3D depth perception
     */
    private fun calculateOptimalLightDirection(): Float3 {
        val cameraPos = cameraNode.worldPosition
        val centerPos = centerNode.worldPosition

        val cameraForward = (centerPos - cameraPos).normalized()

        val worldUp = Float3(0f, 1f, 0f)
        val cameraRight = cross(cameraForward, worldUp).normalized()

        val cameraUp = cross(cameraRight, cameraForward).normalized()

        val lightDirection = (
                cameraForward * 0.75f +      // Mostly forward
                cameraRight * -0.55f +        // Some from the right
                cameraUp * -0.25f             // A bit from above
                ).normalized()

        //Log.d("LightHandler", "Camera forward: $cameraForward")
        //Log.d("LightHandler", "Camera right: $cameraRight")
        //Log.d("LightHandler", "Camera up: $cameraUp")
        //Log.d("LightHandler", "Calculated optimal light direction: $lightDirection")

        return lightDirection
    }

    /**
     * Get the sun light node for scene rendering
     */
    fun getSunLightNode(): LightNode = sunLight

    /**
     * Cleanup resources
     */
    fun cleanup() {
        centerNode.removeChildNode(sunLight)
        sunLight.destroy()
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

    private fun cross(a: Float3, b: Float3): Float3 {
        return Float3(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        )
    }
}