package com.example.gpssatelliteviewer.scene3d

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float3

/**
 * Light-specific parameters extracted for LightHandler
 */
data class LightParameters(
    val intensity: Float,
    val color: Float3,
    val falloff: Float,
    val type: LightManager.Type
)

// TODO make earth, satellite, marker and render quality specific parameters extraction like for light

/**
 * Data class containing all configurable parameters for Scene3D
 */
data class Scene3DParameters(
    // Light Parameters
    var lightIntensity: Float = 250_000.0f,
    var lightColor: Float3 = Float3(1.0f, 1.0f, 1.0f), // RGB white
    var lightFalloff: Float = 1000.0f,
    val lightType: LightManager.Type = LightManager.Type.DIRECTIONAL,

    // Earth Model Parameters
    var earthModelPath: String = "models/NASA_EARTH.glb",
    var earthScale: Float = 1.0f,

    // Satellite Parameters
    var satelliteModelPath: String = "models/RedCircle.glb",
    var satelliteScale: Float = 0.05f,

    // Location marker Parameters
    var locationMarkerModelPath: String = "models/RedCircle.glb",
    var locationMarkerScale: Float = 0.1f,
    var userLocation: Float3? = null,

    // Environment Parameters
    var environmentPath: String = "envs/NightSkyHDRI002_2K.hdr",
    var environmentIntensity: Float = 1.0f,

    // Camera Parameters - positioned for better Earth view
    var startingCameraLocation: Float3 = Float3(0.0f, 1.5f, 4.0f),

    // Performance Parameters
    var enableLevelOfDetail: Boolean = true,
    var enableOcclusion: Boolean = false,

    // Rendering Quality Parameters - Medium Quality Preset
    var hdrColorBufferQuality: QualityLevel = QualityLevel.MEDIUM,
    var dynamicResolutionEnabled: Boolean = true,
    var dynamicResolutionQuality: QualityLevel = QualityLevel.MEDIUM,
    var msaaEnabled: Boolean = false, // Disabled for better performance
    var fxaaEnabled: Boolean = true,  // Faster alternative to MSAA
    var ambientOcclusionEnabled: Boolean = false, // Disabled for medium preset
    var bloomEnabled: Boolean = true,
    var screenSpaceReflectionsEnabled: Boolean = false, // Disabled for better performance
    var temporalAntiAliasingEnabled: Boolean = false
) {
    fun getLightParameters(): LightParameters {
        return LightParameters(
            intensity = lightIntensity,
            color = lightColor,
            falloff = lightFalloff,
            type = lightType
        )
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

    fun updateLocation(location: Float3?) {
        parameters = parameters.copy(userLocation = location)
    }

    fun updateLightColor(red: Float, green: Float, blue: Float) {
        parameters = parameters.copy(lightColor = Float3(red, green, blue))
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