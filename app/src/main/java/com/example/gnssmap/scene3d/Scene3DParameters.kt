package com.example.gnssmap.scene3d

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float3

data class LightParameters(
    val intensity: Float,
    val color: Float3,
    val falloff: Float,
    val type: LightManager.Type
)

// TODO make earth, satellite, marker and render quality specific parameters extraction like for light

data class Scene3DParameters(
    // Light Parameters
    var lightIntensity: Float = 220_000.0f,
    var lightColor: Float3 = Float3(1.0f, 1.0f, 1.0f), // RGB white
    var lightFalloff: Float = 1000.0f,
    val lightType: LightManager.Type = LightManager.Type.DIRECTIONAL,

    // Earth Model Parameters
    var earthModelPath: String = "models/NASA_EARTH.glb",
    var earthScale: Float = 1.0f,

    // Satellite Parameters
    val orbitModelPath: String = "models/orbit.glb",
    val satelliteModelPath: String = "models/RedCircle.glb",
    var satelliteScale: Float = 0.08f,

    // Location marker Parameters
    var locationMarkerModelPath: String = "models/pointer.glb",
    var locationMarkerScale: Float = 0.05f,
    var userLocation: Float3? = null,

    // Camera Parameters - positioned for better Earth view
    var startingCameraLocation: Float3 = Float3(3.0f, 1.0f, 3.0f),

    // Rendering Quality Parameters - Medium Quality Preset
    var hdrColorBufferQuality: QualityLevel = QualityLevel.MEDIUM,
    var dynamicResolutionEnabled: Boolean = true,
    var dynamicResolutionQuality: QualityLevel = QualityLevel.MEDIUM,
    var msaaEnabled: Boolean = false,
    var fxaaEnabled: Boolean = true,
    var ambientOcclusionEnabled: Boolean = false,
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
            "models/RedCircle.glb" to "Circle",
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

    fun updateHdrColorBufferQuality(quality: Scene3DParameters.QualityLevel) {
        parameters = parameters.copy(hdrColorBufferQuality = quality)
    }

    fun updateDynamicResolutionEnabled(enabled: Boolean) {
        parameters = parameters.copy(dynamicResolutionEnabled = enabled)
    }

    fun updateDynamicResolutionQuality(quality: Scene3DParameters.QualityLevel) {
        parameters = parameters.copy(dynamicResolutionQuality = quality)
    }

    fun updateMsaaEnabled(enabled: Boolean) {
        parameters = parameters.copy(msaaEnabled = enabled)
    }

    fun updateFxaaEnabled(enabled: Boolean) {
        parameters = parameters.copy(fxaaEnabled = enabled)
    }

    fun updateTemporalAntiAliasingEnabled(enabled: Boolean) {
        parameters = parameters.copy(temporalAntiAliasingEnabled = enabled)
    }

    fun updateAmbientOcclusionEnabled(enabled: Boolean) {
        parameters = parameters.copy(ambientOcclusionEnabled = enabled)
    }

    fun updateBloomEnabled(enabled: Boolean) {
        parameters = parameters.copy(bloomEnabled = enabled)
    }

    fun resetToDefaults() {
        parameters = Scene3DParameters()
    }
}