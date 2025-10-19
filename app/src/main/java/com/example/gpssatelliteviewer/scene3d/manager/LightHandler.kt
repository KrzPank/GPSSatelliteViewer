package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import com.example.gpssatelliteviewer.data.frameCountUpdateInterval
import com.example.gpssatelliteviewer.scene3d.LightParameters
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.scene3d.manager.camera.CameraManager
import com.example.gpssatelliteviewer.utils.length
import com.example.gpssatelliteviewer.utils.normalized
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.cross
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node

private const val minRecreationInterval = 16L // Minimum 16ms between recreations /60Hz

class LightHandler(
    private val engine: Engine,
    private val centerNode: Node,
    private val cameraManager: CameraManager,
    parameters: Scene3DParameters
) {
    private var sunLight: LightNode = createSunLight(parameters.getLightParameters())
    private var currentLightParameters: LightParameters = parameters.getLightParameters()

    // Light update optimization
    private var isLightBeingRecreated = false

    // Light recreation throttling
    private var lastRecreationTime = 0L
    private var frameCount = 0

    /**
     * Create a sun light from behind the camera
     */
    private fun createSunLight(lightParams: LightParameters): LightNode {
        val lightEntity = EntityManager.get().create()
        val lightDirection = calculateOptimalLightDirection()

        val builder = LightManager.Builder(lightParams.type)
            .intensity(lightParams.intensity)
            .color(lightParams.color.x, lightParams.color.y, lightParams.color.z)
            .direction(lightDirection.x, lightDirection.y, lightDirection.z)

        builder.build(engine, lightEntity)

        val lightNode = LightNode(engine, lightEntity).apply {
            centerNode.addChildNode(this)
        }

        //Log.d("Scene3D", "Sun light created from behind camera - color: ${lightParams.color.x}, ${lightParams.color.y}, ${lightParams.color.z}, direction: ${lightDirection.x}, ${lightDirection.y}, ${lightDirection.z}")
        isLightBeingRecreated = false
        return lightNode
    }

    /**
     * Recreate sun light with new parameters
     */
    private fun recreateSunLight(lightParams: LightParameters = currentLightParameters) {
        if (isLightBeingRecreated) {
            //Log.d("Scene3D", "Light recreation already in progress, skipping")
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

        val frameIntervalMet = frameCount >= frameCountUpdateInterval
        val cameraMovement = cameraManager.getCameraMovedUnits()
        val shouldUpdate = frameIntervalMet && cameraMovement > cameraManager.getDynamicUpdateThreshold()

        if (shouldUpdate) {
            frameCount = 0
            recreateSunLight()
            //Log.d("CameraDetection", " updateSunLight shouldUpdate triggered")
        }
    }

    fun updateParameters(newParameters: Scene3DParameters) {
        val newLightParameters = newParameters.getLightParameters()

        // Check if light parameters have actually changed
        if (currentLightParameters == newLightParameters) {
            return
        }

        // Prevent rapid recreation while already recreating
        if (isLightBeingRecreated) {
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRecreationTime < minRecreationInterval) {
            return
        }

        lastRecreationTime = currentTime
        currentLightParameters = newLightParameters

        recreateSunLight(currentLightParameters)
    }

    fun onFrame() {
        updateSunLight()
    }

    private fun calculateOptimalLightDirection(): Float3 {
        val cameraPos = cameraManager.getCameraPosition()
        val centerPos = centerNode.worldPosition

        val cameraForward = (centerPos - cameraPos).normalized()

        val worldUp = Float3(0f, 1f, 0f)
        val cameraRight = cross(cameraForward, worldUp).normalized()

        val cameraUp = cross(cameraRight, cameraForward).normalized()

        val lightDirection = (
                cameraForward * 0.75f +
                cameraRight * -0.55f +
                cameraUp * -0.25f
        ).normalized()
        return lightDirection
    }

    fun getSunLightNode() = sunLight

    fun cleanup() {
        centerNode.removeChildNode(sunLight)
        sunLight.destroy()
    }
}