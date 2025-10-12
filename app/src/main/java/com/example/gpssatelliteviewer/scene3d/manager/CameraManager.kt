package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.utils.CoordinateConverter
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
    private val parameters: Scene3DParameters
) {
    private var cameraNode = createCamera(parameters.userLocation)
    fun getCameraNode() = cameraNode

    private val cameraGestureDetector = createCameraGestureDetector()
    fun getCameraManipulator() = cameraGestureDetector.cameraManipulator

    private var frameCount = 0
    private val lookAtUpdateInterval = 2
    private var lastCameraPosition = Float3(0.0f, 0.0f, 0.0f)
    // change updateThreshold based on camera zoom and add max zoom in
    // zoomed in -> smaller
    private val updateThreshold = 0.08f

    private fun createCameraGestureDetector(): CameraGestureDetector {
        val cameraGD = CameraGestureDetector(
            viewHeight = { view.viewport.height },
            cameraManipulator = CameraGestureDetector.DefaultCameraManipulator(
                orbitHomePosition = cameraNode.worldPosition,
                targetPosition = centerNode.worldPosition
            )
        ).apply {
            // does not disable camera pan???
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

            satellites.updateLookAt(cameraNode)
            locationMarker.updateLookAt(cameraNode)
        }
    }

    private fun createCamera(startingLocation: Float3?): CameraNode {
        Log.d("CameraPos", "Camera starting pos: ${startingLocation}")
        val pos = calculateCameraStartingPosition(startingLocation) ?: parameters.startingCameraLocation
        Log.d("CameraPos", "Camera pos: ${pos}")
        val camera = CameraNode(engine).apply {
            position = pos
            lookAt(centerNode)
            centerNode.addChildNode(this)
        }
        applyVisualEffects()

        return camera
    }

    fun onFrame(satellites: SatelliteManager, locationMarker: LocationMarkerManager) {
        cameraNode.lookAt(centerNode)
        shouldUpdateLookAt(
            satellites = satellites,
            locationMarker = locationMarker
        )
    }

    private fun calculateCameraStartingPosition(
        userLocation: Float3?,
        distanceFactor: Float = 10.0f
    ): Float3? {
        if (userLocation == null) return null
        val ecef = CoordinateConverter.geodeticToECEF(
            userLocation.x.toDouble(),
            userLocation.y.toDouble(),
            userLocation.z.toDouble()
        )

        val userScenePos = CoordinateConverter.ecefToScenePos(ecef)

        val length = userScenePos.length()
        if (length == 0f) return null

        val dir = userScenePos.normalized(length)

        val cameraDistance = length * distanceFactor

        // +- 1 for better viewing experience
        val cameraPos = Float3(
            dir.x * cameraDistance + 1f,
            dir.y * cameraDistance - 1f,
            dir.z * cameraDistance + 1f
        )

        return cameraPos
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

    private fun Float3.normalized(length: Float): Float3 {
        return if (length > 0f) {
            Float3(x / length, y / length, z / length)
        } else {
            Float3(0f, 0f, 0f)
        }
    }
}
