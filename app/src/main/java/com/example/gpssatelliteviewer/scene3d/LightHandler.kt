package com.example.gpssatelliteviewer.scene3d

import android.util.Log
import com.example.gpssatelliteviewer.data.Scene3DParameters
import com.google.android.filament.Engine
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
    // Main light node - always SUN type
    private var parameters: Scene3DParameters = initialParameters
    private var mainLight: LightNode = createMainLight()
    
    // Synchronization and update optimization
    private val lightUpdateLock = Object()
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
        val lightEntity = engine.entityManager.create()
        val lightType = parameters.lightType

        // Position light appropriately based on type
        val lightPosition = calculateLightPosition(lightType)
        val lightDirection = (centerNode.worldPosition - lightPosition).normalized()
        
        // Convert RGB to RGBA (add alpha = 1.0)
        val lightColor = Float3(parameters.lightColor.x, parameters.lightColor.y, parameters.lightColor.z)

        Log.d("LightManagement", "Creating ${lightType} light with intensity=${parameters.lightIntensity}, color=${lightColor}")

        val builder = LightManager.Builder(lightType)
            .intensity(parameters.lightIntensity)
            .color(lightColor.x, lightColor.y, lightColor.z)
            .falloff(parameters.lightFalloff)

        // Set position and direction based on light type
        when (lightType) {
            LightManager.Type.DIRECTIONAL, LightManager.Type.SUN -> {
                builder.direction(lightDirection.x, lightDirection.y, lightDirection.z)
            }
            LightManager.Type.POINT -> {
                builder.position(lightPosition.x, lightPosition.y, lightPosition.z)
            }
            LightManager.Type.SPOT, LightManager.Type.FOCUSED_SPOT-> {
                builder.position(lightPosition.x, lightPosition.y, lightPosition.z)
                    .direction(lightDirection.x, lightDirection.y, lightDirection.z)
                    .spotLightCone(Math.toRadians(30.0).toFloat(), Math.toRadians(45.0).toFloat()) // Inner and outer cone angles
            }
        }

        builder.build(engine, lightEntity)

        val lightNode = LightNode(engine, lightEntity).apply {
            centerNode.addChildNode(this)
        }

        Log.d("LightManagement", "Light created successfully with entity ID: $lightEntity")
        return lightNode
    }

    /**
     * Recreate light with new parameters - reliable approach
     */
    private fun recreateLight() {
        synchronized(lightUpdateLock) {
            try {
                isLightBeingRecreated = true
                Log.d("LightManagement", "Starting light recreation for parameter update")
                
                // Remove and destroy old light
                centerNode.removeChildNode(mainLight)
                
                try {
                    val oldEntityId = mainLight.entity
                    if (engine.entityManager.isAlive(oldEntityId) && 
                        engine.lightManager.hasComponent(oldEntityId)) {
                        engine.lightManager.destroy(oldEntityId)
                    }
                } catch (e: Exception) {
                    Log.w("LightManagement", "Error destroying old light entity: ${e.message}")
                }
                
                mainLight.destroy()
                
                // Small delay to ensure cleanup
                Thread.sleep(10)
                
                // Create new light with updated parameters
                mainLight = createMainLight()
                
                Log.d("LightManagement", "Light recreation completed successfully")
                
            } catch (e: Exception) {
                Log.e("LightManagement", "Failed to recreate light: ${e.message}")
                
                // Try to create a fallback light
                try {
                    mainLight = createMainLight()
                    Log.w("LightManagement", "Created fallback light after recreation failure")
                } catch (fallbackError: Exception) {
                    Log.e("LightManagement", "Failed to create fallback light: ${fallbackError.message}")
                }
            } finally {
                isLightBeingRecreated = false
            }
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
            LightManager.Type.SUN, LightManager.Type.DIRECTIONAL -> 
                Float3(5.0f, 5.0f, 5.0f) // Far away for directional lighting
            
            LightManager.Type.POINT, LightManager.Type.SPOT, LightManager.Type.FOCUSED_SPOT -> {
                // Position near Earth with slight camera-based adjustment for good lighting
                val offset = (cameraNode.worldPosition - centerNode.worldPosition).normalized() * 0.5f
                Float3(2.0f, 2.0f, 2.0f) + offset
            }
        }
    }
    
    /**
     * Update light to follow camera and illuminate what user is looking at
     */
    private fun updateLight() {
        synchronized(lightUpdateLock) {
            if (isLightBeingRecreated) {
                return
            }
            
            try {
                val entityId = mainLight.entity
                
                if (!engine.entityManager.isAlive(entityId) ||
                    !engine.lightManager.hasComponent(entityId)) {
                    Log.w("LightManagement", "Light entity invalid, skipping update")
                    return
                }
                
                val lightType = engine.lightManager.getType(entityId)
                
                when (lightType) {
                    LightManager.Type.DIRECTIONAL, LightManager.Type.SUN -> {
                        // For directional lights, update direction to always illuminate from behind camera
                        val lightDirection = (centerNode.worldPosition - cameraNode.worldPosition).normalized()
                        engine.lightManager.setDirection(entityId, lightDirection.x, lightDirection.y, lightDirection.z)
                    }
                    
                    LightManager.Type.POINT -> {
                        // For point lights, position behind camera
                        val lightPosition = calculateLightPosition(lightType)
                        engine.lightManager.setPosition(entityId, lightPosition.x, lightPosition.y, lightPosition.z)
                    }
                    
                    LightManager.Type.SPOT, LightManager.Type.FOCUSED_SPOT -> {
                        // For spot lights, position behind camera and point toward center
                        val lightPosition = calculateLightPosition(lightType)
                        val lightDirection = (centerNode.worldPosition - lightPosition).normalized()
                        engine.lightManager.setPosition(entityId, lightPosition.x, lightPosition.y, lightPosition.z)
                        engine.lightManager.setDirection(entityId, lightDirection.x, lightDirection.y, lightDirection.z)
                    }
                }
                
                // Update tracking variables
                lastCameraPosition = cameraNode.worldPosition
                lastLightUpdateFrame = frameCount
                
            } catch (e: Exception) {
                Log.e("LightManagement", "Light update failed: ${e.message}")
            }
        }
    }
    
    /**
     * Update the light parameters when they change
     */
    fun updateParameters(newParameters: Scene3DParameters) {
        Log.d("LightManagement", "updateParameters called - intensity: ${newParameters.lightIntensity}, color: ${newParameters.lightColor}")
        
        val oldParameters = parameters
        parameters = newParameters
        
        // Light is always initialized in constructor
        
        // Check if any light properties changed
        if (oldParameters.lightIntensity != newParameters.lightIntensity ||
            oldParameters.lightColor != newParameters.lightColor ||
            oldParameters.lightFalloff != newParameters.lightFalloff ||
            oldParameters.lightType != newParameters.lightType) {
            Log.d("LightManagement", "Light properties changed - recreating light")
            recreateLight()
        } else {
            Log.d("LightManagement", "No light property changes detected")
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
        try {
            centerNode.removeChildNode(mainLight)
            if (engine.entityManager.isAlive(mainLight.entity) && 
                engine.lightManager.hasComponent(mainLight.entity)) {
                engine.lightManager.destroy(mainLight.entity)
            }
            mainLight.destroy()
        } catch (e: Exception) {
            Log.w("LightManagement", "Error during light cleanup: ${e.message}")
        }
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