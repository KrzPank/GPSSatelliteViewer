package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import com.example.gpssatelliteviewer.utils.length
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.normalized
import com.google.android.filament.Engine
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.gesture.transform
import io.github.sceneview.math.Transform
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.Node
import kotlin.apply

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
    fun getCameraGestureDetector() = cameraGestureDetector

    private var frameCount = 0
    private val lookAtUpdateInterval = 2
    private var lastCameraPosition = Float3(0.0f, 0.0f, 0.0f)
    private val updateThreshold = 0.008f
    private val minCameraDistance = 0.60f

    private var lastValidLocalPosition: Float3 = cameraNode.position

    private fun createCameraGestureDetector(): CameraGestureDetector {
        val base = CameraGestureDetector.DefaultCameraManipulator(
            orbitHomePosition = cameraNode.worldPosition,
            targetPosition = centerNode.worldPosition
        )

        //val clamped = ClampedManipulatorWrapper(
        //    base = base,
        //    minDistance = minCameraDistance
        //)

        return CameraGestureDetector(
            viewHeight = { view.viewport.height },
            cameraManipulator = base
        ).apply {
            isPanEnabled = false
        }
    }

    private fun shouldUpdateLookAt(satellites: SatelliteManager, locationMarker: LocationMarkerManager) {
        frameCount++

        val distanceToCenter = (cameraNode.worldPosition - centerNode.worldPosition).length()
        val cameraMoved = (cameraNode.worldPosition - lastCameraPosition).length()
        val dynamicThreshold = updateThreshold * distanceToCenter * distanceToCenter
        val frameIntervalMet = frameCount >= lookAtUpdateInterval

        val shouldUpdate = frameIntervalMet && cameraMoved  > dynamicThreshold
        if (shouldUpdate) {
            lastCameraPosition = cameraNode.worldPosition
            frameCount = 0

            satellites.updateLookAt(cameraNode)
            locationMarker.updateLookAt(cameraNode)
        }
    }

    private fun createCamera(startingLocation: Float3?): CameraNode {
        val pos = calculateCameraStartingPosition(startingLocation) ?: parameters.startingCameraLocation
        val camera = CameraNode(engine).apply {
            position = pos
            lookAt(centerNode)
            centerNode.addChildNode(this)
        }
        applyVisualEffects()

        return camera
    }

    fun onFrame(satellites: SatelliteManager, locationMarker: LocationMarkerManager) {
        shouldUpdateLookAt(satellites, locationMarker)

        val cameraWorldPos = cameraNode.worldPosition
        val centerWorldPos = centerNode.worldPosition
        val offset = cameraWorldPos - centerWorldPos
        val dist = offset.length()

        if (dist >= minCameraDistance){
            lastValidLocalPosition = cameraNode.position
        } else {
            val dirNorm = offset.normalized(dist)
            cameraNode.position = centerWorldPos + dirNorm * minCameraDistance
            //Log.d("CameraPos", "Clamped camera to $clampedWorldPos, lastValidLocalPosition: $lastValidLocalPosition   (dist=$dist). New manipulator created.")
        }
        cameraNode.lookAt(centerNode)
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

        val dir = userScenePos.normalized()

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
}

class ClampedManipulatorWrapper(
    private val base: CameraGestureDetector.CameraManipulator,
    private val minDistance: Float = 0.58f,
    private val maxDistance: Float = 500.0f
) : CameraGestureDetector.CameraManipulator {

    override fun setViewport(width: Int, height: Int) {
        base.setViewport(width, height)
    }

    override fun getTransform(): Transform {
        val baseTransform = base.getTransform()

        // Read base values
        val eye = baseTransform.position
        var forward = baseTransform.forward
        var up = baseTransform.up

        val target = eye + forward * -1f

        val dir = eye - target
        val dist = dir.length()

        val clamped = dist.coerceIn(minDistance, maxDistance)

        if (clamped == dist) {
            return baseTransform
        }

        val dirNorm = if (dist > 0f) dir.normalized() else Float3(0f, 0f, -1f)
        val clampedEye = target + dirNorm * clamped

        val result = Transform().apply {
            position = clampedEye
            // recompute forward to look towards the target
            forward = (target - clampedEye).normalized()
        }
        return result
    }

    override fun grabBegin(x: Int, y: Int, strafe: Boolean) { base.grabBegin(x, y, strafe) }
    override fun grabUpdate(x: Int, y: Int) { base.grabUpdate(x, y) }
    override fun grabEnd() { base.grabEnd() }
    override fun scrollBegin(x: Int, y: Int, separation: Float) { base.scrollBegin(x, y, separation) }
    override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {
        base.scrollUpdate(x, y, prevSeparation, currSeparation)
    }
    override fun scrollEnd() { base.scrollEnd() }

    override fun update(deltaTime: Float) { base.update(deltaTime) }
}
