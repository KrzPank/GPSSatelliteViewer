package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.google.android.filament.Engine
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.Node
import kotlin.apply
import kotlin.math.sqrt

class CameraManager(
    private val engine: Engine,
    private val view: View,
    private val centerNode: Node,
    private val parameters: Scene3DParameters = Scene3DParameters()
) {
    private var cameraNode = createCamera(parameters.location)
    fun getCameraNode() = cameraNode

    private val cameraGestureDetector = createCameraGestureDetector()
    fun getCameraManipulator() = cameraGestureDetector.cameraManipulator
    fun getCameraGestureDetector() = cameraGestureDetector

    private var frameCount = 0
    private val lookAtUpdateInterval = 2
    private var lastCameraPosition = Float3(0.0f, 0.0f, 0.0f)
    private val updateThreshold = 0.08f

    private fun createCameraGestureDetector(): CameraGestureDetector {
        // does not disable camera pan???
        val cameraGD = CameraGestureDetector(
            viewHeight = { view.viewport.height },
            cameraManipulator = CameraGestureDetector.DefaultCameraManipulator(
                orbitHomePosition = cameraNode.worldPosition,
                targetPosition = centerNode.worldPosition
            )
        ).apply {
            isPanEnabled = false
        }

        return cameraGD
    }

    private fun shouldUpdateLookAt(satellites: SatelliteManager, locationMarker: LocationMarkerManager) {
        frameCount++

        val frameIntervalMet = frameCount >= lookAtUpdateInterval
        val cameraMoved = (cameraNode.worldPosition - lastCameraPosition).length()
        val shouldUpdate = frameIntervalMet && cameraMoved  > updateThreshold

        if (shouldUpdate) {
            lastCameraPosition = cameraNode.worldPosition
            frameCount = 0

            cameraNode.lookAt(centerNode)
            satellites.updateLookAt(cameraNode)
            locationMarker.updateLookAt(cameraNode)

            Log.d("update look at", "updated look at")
        }
    }

    private fun createCamera(startingLocation: Float3?): CameraNode {
        val location = startingLocation ?: parameters.startingCameraLocation
        val camera = CameraNode(engine).apply {
            position = location
            lookAt(centerNode)
            centerNode.addChildNode(this)
        }
        applyVisualEffects()

        return camera
    }

    fun onFrame(satellites: SatelliteManager, locationMarker: LocationMarkerManager) {
        shouldUpdateLookAt(
            satellites = satellites,
            locationMarker = locationMarker
        )
    }

    private fun applyVisualEffects() {
        // HDR Color Buffer Quality
        view.renderQuality = view.renderQuality.apply {
            hdrColorBuffer = when (parameters.hdrColorBufferQuality) {
                Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
                Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
                Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
                Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
            }
        }

        // Dynamic Resolution
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            if (parameters.dynamicResolutionEnabled) {
                quality = when (parameters.dynamicResolutionQuality) {
                    Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
                    Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
                    Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
                    Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
                }
            }
        }

        // MSAA (Multi-Sample Anti-Aliasing)
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
            enabled = parameters.msaaEnabled
        }

        // FXAA (Fast Approximate Anti-Aliasing)
        view.antiAliasing = if (parameters.fxaaEnabled) {
            View.AntiAliasing.FXAA
        } else {
            View.AntiAliasing.NONE
        }

        // Temporal Anti-Aliasing
        view.temporalAntiAliasingOptions = view.temporalAntiAliasingOptions.apply {
            enabled = parameters.temporalAntiAliasingEnabled
        }

        // Ambient Occlusion
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
            enabled = parameters.ambientOcclusionEnabled
        }

        // Bloom
        view.bloomOptions = view.bloomOptions.apply {
            enabled = parameters.bloomEnabled
        }

        // Screen Space Reflections
        view.screenSpaceReflectionsOptions = view.screenSpaceReflectionsOptions.apply {
            enabled = parameters.screenSpaceReflectionsEnabled
        }

        Log.d(
            "Scene3D", "Visual effects applied: HDR=${parameters.hdrColorBufferQuality}, " +
                    "DynRes=${parameters.dynamicResolutionEnabled}(${parameters.dynamicResolutionQuality}), " +
                    "MSAA=${parameters.msaaEnabled}, FXAA=${parameters.fxaaEnabled}, " +
                    "TAA=${parameters.temporalAntiAliasingEnabled}, AO=${parameters.ambientOcclusionEnabled}, " +
                    "Bloom=${parameters.bloomEnabled}, SSR=${parameters.screenSpaceReflectionsEnabled}"
        )
    }

    fun cleanup() {
        cameraNode.let {
            centerNode.removeChildNode(it)
            it.destroy()
        }
    }

    private fun Float3.length(): Float {
        return sqrt(x * x + y * y + z * z)
    }
}
