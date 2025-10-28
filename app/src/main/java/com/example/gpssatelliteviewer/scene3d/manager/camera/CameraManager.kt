package com.example.gpssatelliteviewer.scene3d.manager.camera

import android.util.Log
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.length
import com.example.gpssatelliteviewer.utils.normalized
import com.google.android.filament.Engine
import com.google.android.filament.View
import com.google.android.filament.utils.Manipulator
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.Node

private const val UPDATE_THRESHOLD = 0.005f
private const val STARTING_LOCATION_DISTANCE_FACTOR = 5f
private const val MIN_CAMERA_DISTANCE = 0.62f
private const val MAX_CAMERA_DISTANCE = 9f
private const val MAX_PITCH_DEG = 85f

private const val ORBIT_SPEED = 0.0042f
private const val ZOOM_SPEED = 0.05f

class CameraManager(
    private val engine: Engine,
    private val view: View,
    private val centerNode: Node,
    private val sceneParameters: Scene3DParameters
) {
    private var cameraMovedUnits = 0.0f

    private var cameraNode = createCamera()
    private val cameraGestureDetector = createCameraGestureDetector()

    private var lastCameraPosition = cameraNode.worldPosition
    private var cameraDistanceToCenter = sceneParameters.startingCameraLocation.length()
    private var dynamicUpdateThreshold = UPDATE_THRESHOLD

    fun getCameraNode() = cameraNode

    fun getCameraManipulator() = cameraGestureDetector.cameraManipulator
    fun getCameraGestureDetector() = cameraGestureDetector

    fun getCameraPosition() = cameraNode.worldPosition
    fun getLastCameraPosition() = lastCameraPosition
    fun getCameraMovedUnits() = cameraMovedUnits
    fun getCameraDistanceToCenter() = cameraDistanceToCenter
    fun getDynamicUpdateThreshold() = dynamicUpdateThreshold

    private fun createCamera(): CameraNode {
        val pos = calculateCameraStartingPosition() ?: sceneParameters.startingCameraLocation
        val camera = CameraNode(engine).apply {
            position = pos
            lookAt(centerNode)
            centerNode.addChildNode(this)
        }
        applyVisualEffects()
        return camera
    }

    // change to different manipulator wrapper have some ideas going on
    // move to transforms not camera.position
    private fun createCameraGestureDetector(): CameraGestureDetector {
        val baseManipulator = Manipulator.Builder()
            .orbitHomePosition(cameraNode.worldPosition.x, cameraNode.worldPosition.y, cameraNode.worldPosition.z)
            .targetPosition(centerNode.worldPosition.x, centerNode.worldPosition.y, centerNode.worldPosition.z)
            .orbitSpeed(ORBIT_SPEED, ORBIT_SPEED)
            .zoomSpeed(ZOOM_SPEED)
            .build(Manipulator.Mode.ORBIT)

        val clampedManipulator = CameraManipulatorWrapper(
            manipulator = baseManipulator,
            minCameraDistance = MIN_CAMERA_DISTANCE,
            maxCameraDistance = MAX_CAMERA_DISTANCE,
            maxPitchDeg = MAX_PITCH_DEG
        )

        return CameraGestureDetector(
            viewHeight = { view.viewport.height },
            cameraManipulator = clampedManipulator
        ).apply {
            isPanEnabled = false
        }
    }

    private fun updateCameraParameters() {
        val cameraWorldPos = cameraNode.worldPosition
        val dist = cameraWorldPos.length()

        val cameraMovement = (cameraWorldPos - lastCameraPosition).length()
        val shouldUpdate = cameraMovement > dynamicUpdateThreshold

        cameraMovedUnits = cameraMovement
        cameraDistanceToCenter = dist

        if (shouldUpdate) {
            lastCameraPosition = cameraWorldPos
            dynamicUpdateThreshold = cameraDistanceToCenter * UPDATE_THRESHOLD  // dist^2 * updateThreshold
        }
    }

    fun onFrame() {
        updateCameraParameters()
        cameraNode.lookAt(centerNode)
    }

    private fun calculateCameraStartingPosition(): Float3? {
        val userLocation = if (sceneParameters.userLocation != null) sceneParameters.userLocation!!
        else return null

        val ecef = CoordinateConverter.geodeticToECEF(
            userLocation.x.toDouble(),
            userLocation.y.toDouble(),
            userLocation.z.toDouble()
        )

        val userScenePos = CoordinateConverter.ecefToScenePos(ecef)
        val dir = userScenePos.normalized()
        // +- 1 for better viewing experience
        return Float3(
            dir.x * STARTING_LOCATION_DISTANCE_FACTOR + 1f,
            dir.y * STARTING_LOCATION_DISTANCE_FACTOR - 1f,
            dir.z * STARTING_LOCATION_DISTANCE_FACTOR + 1f
        )
    }

    private fun applyVisualEffects() {
        // HDR Color Buffer Quality
        view.renderQuality = view.renderQuality.apply {
            hdrColorBuffer = when (sceneParameters.hdrColorBufferQuality) {
                Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
                Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
                Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
                Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
            }
        }

        // Dynamic Resolution
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            if (sceneParameters.dynamicResolutionEnabled) {
                quality = when (sceneParameters.dynamicResolutionQuality) {
                    Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
                    Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
                    Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
                    Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
                }
            }
        }

        // MSAA (Multi-Sample Anti-Aliasing)
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
            enabled = sceneParameters.msaaEnabled
        }

        // FXAA (Fast Approximate Anti-Aliasing)
        view.antiAliasing = if (sceneParameters.fxaaEnabled) {
            View.AntiAliasing.FXAA
        } else {
            View.AntiAliasing.NONE
        }

        // Temporal Anti-Aliasing
        view.temporalAntiAliasingOptions = view.temporalAntiAliasingOptions.apply {
            enabled = sceneParameters.temporalAntiAliasingEnabled
        }

        // Ambient Occlusion
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
            enabled = sceneParameters.ambientOcclusionEnabled
        }

        // Bloom
        view.bloomOptions = view.bloomOptions.apply {
            enabled = sceneParameters.bloomEnabled
        }

        // Screen Space Reflections
        view.screenSpaceReflectionsOptions = view.screenSpaceReflectionsOptions.apply {
            enabled = sceneParameters.screenSpaceReflectionsEnabled
        }

        Log.d(
            "Scene3D", "Visual effects applied: HDR=${sceneParameters.hdrColorBufferQuality}, " +
                    "DynRes=${sceneParameters.dynamicResolutionEnabled}(${sceneParameters.dynamicResolutionQuality}), " +
                    "MSAA=${sceneParameters.msaaEnabled}, FXAA=${sceneParameters.fxaaEnabled}, " +
                    "TAA=${sceneParameters.temporalAntiAliasingEnabled}, AO=${sceneParameters.ambientOcclusionEnabled}, " +
                    "Bloom=${sceneParameters.bloomEnabled}, SSR=${sceneParameters.screenSpaceReflectionsEnabled}"
        )
    }

    fun cleanup() {
        cameraNode.let {
            centerNode.removeChildNode(it)
            it.destroy()
        }
    }
}
