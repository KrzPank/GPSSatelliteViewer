package com.example.gpssatelliteviewer.data

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.romainguy.kotlin.math.Float3

/**
 * Data class containing all configurable parameters for Scene3D
 */
data class Scene3DParameters(
    // Light Parameters
    var lightIntensity: Float = 10_000_000.0f,
    var lightColor: Float3 = Float3(1.0f, 1.0f, 1.0f), // RGB white
    var lightFalloff: Float = 1000.0f,
    var lightType: LightType = LightType.POINT,
    
    // Earth Model Parameters
    var earthModelPath: String = "models/NASA_EARTH.glb",
    var earthScale: Float = 1.0f,
    
    // Satellite Parameters
    var satelliteModelPath: String = "models/RedCircle.glb",
    var satelliteScale: Float = 0.05f,
    
    // Environment Parameters
    var environmentPath: String = "envs/8k_stars_milky_way.hdr",
    var environmentIntensity: Float = 1.0f,
    
    // Camera Parameters
    var cameraDistance: Float = 3.0f,

    // Performance Parameters
    var enableLevelOfDetail: Boolean = true,
    var enableOcclusion: Boolean = false,
    
    // Rendering Quality Parameters - High Quality Preset
    var hdrColorBufferQuality: QualityLevel = QualityLevel.HIGH,
    var dynamicResolutionEnabled: Boolean = true,
    var dynamicResolutionQuality: QualityLevel = QualityLevel.HIGH,
    var msaaEnabled: Boolean = true,
    var fxaaEnabled: Boolean = false,
    var ambientOcclusionEnabled: Boolean = true,
    var bloomEnabled: Boolean = true,
    var screenSpaceReflectionsEnabled: Boolean = true,
    var temporalAntiAliasingEnabled: Boolean = false
) {
    enum class LightType(val displayName: String) {
        POINT("Point Light"),
        DIRECTIONAL("Directional Light"),
        SPOT("Spot Light"),
        SUN("Sun Light")
    }
    
    enum class QualityLevel(val displayName: String) {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        ULTRA("Ultra")
    }
    
    companion object {
        // Predefined earth model options
        val EARTH_MODEL_OPTIONS = listOf(
            "models/material_test_noQuad.glb" to "Material Test Earth",
            "models/Earth_base.glb" to "Base Earth",
            "models/NASA_EARTH.glb" to "NASA Earth"
        )
        
        // Predefined satellite model options
        val SATELLITE_MODEL_OPTIONS = listOf(
            "models/RedCircle.glb" to "Red Circle",
            "models/TDRS_A.glb" to "Simple Satellite",
        )
    }
}

/**
 * Observable wrapper for Scene3D parameters
 */
class Scene3DParametersState {
    var parameters by mutableStateOf(Scene3DParameters())
        private set

    fun updateLightIntensity(intensity: Float) {
        parameters = parameters.copy(lightIntensity = intensity)
    }
    
    fun updateLightColor(color: Float3) {
        parameters = parameters.copy(lightColor = color)
    }
    
    fun updateLightColor(red: Float, green: Float, blue: Float) {
        parameters = parameters.copy(lightColor = Float3(red, green, blue))
    }
    
    fun updateLightType(type: Scene3DParameters.LightType) {
        parameters = parameters.copy(lightType = type)
    }
    
    fun updateLightFalloff(falloff: Float) {
        parameters = parameters.copy(lightFalloff = falloff)
    }
    
    fun updateEarthModel(path: String) {
        parameters = parameters.copy(earthModelPath = path)
    }
    
    fun updateSatelliteModel(path: String) {
        parameters = parameters.copy(satelliteModelPath = path)
    }

    fun resetToDefaults() {
        parameters = Scene3DParameters()
    }
}
