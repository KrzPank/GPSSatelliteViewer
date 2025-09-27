package com.example.gpssatelliteviewer.scene3d

import android.util.Log
import com.example.gpssatelliteviewer.data.Scene3DParameters
import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.FilamentEntity
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import com.google.android.filament.EntityManager
import kotlin.math.sqrt

class LightHandler(
    private val engine: Engine,
    private val centerNode: Node,
    private val cameraNode: CameraNode,
    private var parameters: Scene3DParameters
) {
    private var mainLight: LightNode = createMainLight()

    // Removed synchronization - now fully synchronous
    private var isLightBeingRecreated = false

    // Light update optimization
    private var lastCameraPosition = Float3(0f, 0f, 0f)
    private var lastLightUpdateFrame = 0
    private var frameCount = 0
    private val lightUpdateThreshold = 0.1f // Minimum camera movement to trigger light update
    private val lightUpdateInterval = 5 // Update light every 5 frames minimum

    /**
     * Create the main light - supports directional, point, and spot lights
     */
    private fun createMainLight(): LightNode {
        // 1. Create an entity using Filament’s EntityManager
        val lightEntity = EntityManager.get().create()

        // 2. Attach a Filament light component to it
        val lightType = parameters.lightType
        val lightPosition = calculateLightPosition(lightType)
        val lightDirection = (centerNode.worldPosition - lightPosition).normalized()
        val (r, g, b) = parameters.lightColor
        val intensity = parameters.lightIntensity
        val falloff = parameters.lightFalloff

        val builder = LightManager.Builder(lightType)
            .intensity(intensity)
            .color(r, g, b)
            .falloff(falloff)

        when (lightType) {
            LightManager.Type.DIRECTIONAL, LightManager.Type.SUN -> {
                builder.direction(lightDirection.x, lightDirection.y, lightDirection.z)
            }
            LightManager.Type.POINT -> {
                builder.position(lightPosition.x, lightPosition.y, lightPosition.z)
            }
            LightManager.Type.SPOT, LightManager.Type.FOCUSED_SPOT -> {
                builder.position(lightPosition.x, lightPosition.y, lightPosition.z)
                    .direction(lightDirection.x, lightDirection.y, lightDirection.z)
                    .spotLightCone(
                        Math.toRadians(30.0).toFloat(),
                        Math.toRadians(45.0).toFloat()
                    )
            }
        }

        builder.build(engine, lightEntity)

        // 3. Create the LightNode with *the same entity*
        val lightNode = LightNode(engine, lightEntity).apply {
            centerNode.addChildNode(this)
        }

        Log.d("Scene3D", "Light created entity=$lightEntity")

        return lightNode
    }

    /**
     * Recreate light with new parameters - fully synchronous approach
     */
    private fun recreateLight() {
        try {
            isLightBeingRecreated = true
            Log.d("Scene3D", "Starting light recreation for parameter update")

            // Properly remove and destroy old light
            centerNode.removeChildNode(mainLight)

            try {
                val oldEntityId = mainLight.entity
                if (engine.entityManager.isAlive(oldEntityId)) {
                    // Check if entity has light component before destroying
                    val hasComponent = try {
                        engine.lightManager.hasComponent(oldEntityId)
                    } catch (e: IndexOutOfBoundsException) {
                        Log.w("Scene3D", "Bounds error checking hasComponent during cleanup: ${e.message}")
                        false
                    }

                    if (hasComponent) {
                        try {
                            engine.lightManager.destroy(oldEntityId)
                            Log.d("Scene3D", "Successfully destroyed old light entity: $oldEntityId")
                        } catch (e: IndexOutOfBoundsException) {
                            Log.w("Scene3D", "Bounds error destroying light: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("Scene3D", "Error during old light cleanup: ${e.message}")
            }

            mainLight.destroy()

            // Create new light
            mainLight = createMainLight()
            Log.d("Scene3D", "Light recreation completed successfully")

        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to recreate light: ${e.message}")

            // Simple fallback - create light with default parameters
            try {
                val originalParams = parameters
                parameters = Scene3DParameters()
                mainLight = createMainLight()
                parameters = originalParams
                Log.w("Scene3D", "Created fallback light with default parameters")
            } catch (fallbackError: Exception) {
                Log.e("Scene3D", "Failed to create fallback light: ${fallbackError.message}")
            }
        } finally {
            isLightBeingRecreated = false
        }
    }

    /**
     * Check if light should be updated based on camera movement and frame count
     */
    private fun shouldUpdateLight(): Boolean {
        frameCount++

        // Skip if light is being recreated
        if (isLightBeingRecreated) {
            return false
        }

        // Check frame interval and camera movement
        val frameIntervalMet = frameCount - lastLightUpdateFrame >= lightUpdateInterval
        val cameraMovement = (cameraNode.worldPosition - lastCameraPosition).length()

        return frameIntervalMet && cameraMovement > lightUpdateThreshold
    }

    /**
     * Calculate optimal light position - kept for consistency but not used for SUN light
     */
    private fun calculateLightPosition(lightType: LightManager.Type): Float3 {
        return when (lightType) {
            LightManager.Type.POINT,
            LightManager.Type.SPOT,
            LightManager.Type.FOCUSED_SPOT -> cameraNode.worldPosition + Float3(1.0f, 1.0f, 1.0f)
            else -> Float3(0.0f, 1.0f, 0.0f)
        }
    }

    /**
     * Update light to follow camera and illuminate what user is looking at
     */
    private fun updateLight() {
        if (isLightBeingRecreated) {
            return
        }
        try {
            val entityId = mainLight.entity

            if (!engine.entityManager.isAlive(entityId)) {
                Log.w("Scene3D", "updateLight skipped: entity $entityId not alive")
                return
            }
            if (!engine.lightManager.hasComponent(entityId)) {
                Log.w("Scene3D", "updateLight skipped: entity $entityId has no light component")
                return
            }
                // Safely get light type with validation
                val lightType = try {
                    val retrievedType = engine.lightManager.getType(entityId)
                    // Validate that the light type is one of the expected enum values
                    when (retrievedType) {
                        LightManager.Type.DIRECTIONAL,
                        LightManager.Type.POINT,
                        LightManager.Type.SPOT,
                        LightManager.Type.FOCUSED_SPOT,
                        LightManager.Type.SUN -> retrievedType
                    }
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("Scene3D", "Light manager index out of bounds: ${e.message}")
                    // Fallback to parameters lightType
                    parameters.lightType
                } catch (e: Exception) {
                    Log.e("Scene3D", "Failed to get light type: ${e.message}")
                    return
                }

            when (lightType) {
                LightManager.Type.DIRECTIONAL, LightManager.Type.SUN -> {
                    // For directional lights, update direction to always illuminate from behind camera
                    val lightDirection =
                        (centerNode.worldPosition - cameraNode.worldPosition).normalized()
                    try {
                        engine.lightManager.setDirection(
                            entityId,
                            lightDirection.x,
                            lightDirection.y,
                            lightDirection.z
                        )
                    } catch (e: IndexOutOfBoundsException) {
                        Log.e(
                            "Scene3D", "Light manager setDirection index out of bounds: ${e.message}"
                        )
                        return
                    }
                }

                LightManager.Type.POINT -> {
                    // For point lights, position behind camera
                    val lightPosition = calculateLightPosition(lightType)
                    try {
                        engine.lightManager.setPosition(
                            entityId,
                            lightPosition.x,
                            lightPosition.y,
                            lightPosition.z
                        )
                    } catch (e: IndexOutOfBoundsException) {
                        Log.e(
                            "Scene3D", "Light manager setPosition index out of bounds: ${e.message}"
                        )
                        return
                    }
                }

                LightManager.Type.SPOT, LightManager.Type.FOCUSED_SPOT -> {
                    // For spot lights, position behind camera and point toward center
                    val lightPosition = calculateLightPosition(lightType)
                    val lightDirection = (centerNode.worldPosition - lightPosition).normalized()
                    try {
                        engine.lightManager.setPosition(
                            entityId,
                            lightPosition.x,
                            lightPosition.y,
                            lightPosition.z
                        )
                        engine.lightManager.setDirection(
                            entityId,
                            lightDirection.x,
                            lightDirection.y,
                            lightDirection.z
                        )
                    } catch (e: IndexOutOfBoundsException) {
                        Log.e(
                            "Scene3D",
                            "Light manager setPosition/setDirection index out of bounds: ${e.message}"
                        )
                        return
                    }
                }
            }

            // Update tracking variables
            lastCameraPosition = cameraNode.worldPosition
            lastLightUpdateFrame = frameCount

        } catch (e: Exception) {
            Log.e("Scene3D", "Light update failed: ${e.message}")
        }
    }

    /**
     * Update the light parameters when they change
     */
    fun updateParameters(newParameters: Scene3DParameters) {
        Log.d("Scene3D", "updateParameters called - intensity: ${newParameters.lightIntensity}, color: ${newParameters.lightColor}")

        val oldParameters = parameters
        parameters = newParameters

        // Light is always initialized in constructor

        // Check if any light properties changed
        if (oldParameters.lightIntensity != newParameters.lightIntensity ||
            oldParameters.lightColor != newParameters.lightColor ||
            oldParameters.lightFalloff != newParameters.lightFalloff ||
            oldParameters.lightType != newParameters.lightType) {
            Log.d("Scene3D", "Light properties changed - recreating light")
            //recreateLight()
        } else {
            Log.d("Scene3D", "No light property changes detected")
        }
    }

    /**
     * Called each frame to update light if needed
     */
    fun onFrame() {
        if (shouldUpdateLight()) {
            updateLight()
        }
    }

    /**
     * Get the main light node for scene rendering
     */
    fun getMainLightNode(): LightNode = mainLight

    /**
     * Cleanup resources
     */
    fun cleanup() {
        centerNode.removeChildNode(mainLight)
        mainLight.destroy()
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
}