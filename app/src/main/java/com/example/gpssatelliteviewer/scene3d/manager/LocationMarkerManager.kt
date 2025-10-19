package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import com.example.gpssatelliteviewer.data.frameCountUpdateInterval
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.scene3d.manager.camera.CameraManager
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.normalized
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

private const val SCALE_THRESHOLD = 0.15f
private const val scale = 0.05f
private const val locationMarkerUpdateInterval = 60 * 1000 * 5

class LocationMarkerManager(
    private val modelLoader: ModelLoader,
    private val centerNode: Node,
    private val cameraManager: CameraManager,
    private var parameters: Scene3DParameters
) {
    private var locationMarkerNode: ModelNode
    private var isVisible: Boolean = true

    private var frameCount = 0

    private var lastLocationMarkerUpdateTime = 0L
    private var firstLocationMarkerUpdate = true
    private var locationMarkerScale = 0.1f

    init {
        locationMarkerNode = createLocationMarker()
        if (shouldUpdateLocationMarker()) updateLocationMarker()
    }

    fun isLocationMarkerVisible() = isVisible
    fun setVisible(visible: Boolean) {
        isVisible = visible
        if (visible) {
            if (locationMarkerNode.parent == null) centerNode.addChildNode(locationMarkerNode)
        } else {
            if (locationMarkerNode.parent != null) centerNode.removeChildNode(locationMarkerNode)
        }
    }

    fun getNode() = locationMarkerNode

    fun onFrame() {
        updateLocationMarker()
    }

    private fun createLocationMarker(): ModelNode {
        val node = ModelNode(
            modelInstance = modelLoader.createModelInstance(parameters.locationMarkerModelPath),
            scaleToUnits = locationMarkerScale
        ).also {
            centerNode.addChildNode(it)
            it.name = "Location marker"
        }
        return node
    }

    private fun updateLocationMarker() {
        if (shouldUpdateLocationMarker()){
            frameCount++

            if (firstLocationMarkerUpdate) {
                val dist = cameraManager.getCameraDistanceToCenter()
                val s = scale * dist * dist
                locationMarkerScale = if (s >= SCALE_THRESHOLD) SCALE_THRESHOLD else s

                locationMarkerNode.position = calculateLocationMarkerScenePosition()
                locationMarkerNode.lookTowards(calculateLocationMarkerDirection(verticalToWorld = true))
                locationMarkerNode.scaleToUnitCube(locationMarkerScale)

                lastLocationMarkerUpdateTime = System.currentTimeMillis()
                firstLocationMarkerUpdate = false

                return
                Log.d("LocationMarkerPos", "firstLocationMarkerUpdate")
            }

            val timeSinceLastLocationMarkerPositionUpdate = System.currentTimeMillis() - lastLocationMarkerUpdateTime
            val locationMarkerPositionUpdateIntervalMet = timeSinceLastLocationMarkerPositionUpdate >= locationMarkerUpdateInterval

            if (locationMarkerPositionUpdateIntervalMet) {
                locationMarkerNode.position = calculateLocationMarkerScenePosition()
                locationMarkerNode.lookTowards(calculateLocationMarkerDirection(verticalToWorld = true))

                lastLocationMarkerUpdateTime = System.currentTimeMillis()
            }

            val frameIntervalMet = frameCount >= frameCountUpdateInterval
            val cameraMovement = cameraManager.getCameraMovedUnits()
            val shouldUpdateScale = frameIntervalMet && cameraMovement > cameraManager.getDynamicUpdateThreshold()

            if (shouldUpdateScale) {
                val dist = cameraManager.getCameraDistanceToCenter()
                val s = scale * dist * dist
                locationMarkerScale = if (s >= SCALE_THRESHOLD) SCALE_THRESHOLD else s

                locationMarkerNode.position = calculateLocationMarkerScenePosition()
                locationMarkerNode.lookTowards(calculateLocationMarkerDirection(verticalToWorld = true))
                locationMarkerNode.scaleToUnitCube(locationMarkerScale)

                lastLocationMarkerUpdateTime = System.currentTimeMillis()
                Log.d("locationMarkerScale", " locationMarkerScale=$locationMarkerScale CameraDist=${cameraManager.getCameraDistanceToCenter()}")
            }
        }
    }

    //locationMarkerNode.scaleToUnitCube(parameters.locationMarkerScale)

    private fun shouldUpdateLocationMarker(): Boolean {
        if (parameters.userLocation == null) {
            Log.d("LocationMarkerPos", "userLocation == null")
            setVisible(false)
            return false
        }

        if (!isVisible) {
            Log.d("LocationMarkerPos", "isVisible == false")
            setVisible(false)
            return false
        }

        if (locationMarkerNode.parent == null) {
            Log.d("LocationMarkerPos", "locationMarkerNode.parent == null")
            return false
        }

        //Log.d("LocationMarkerPos", "shouldUpdateLocationMarker == true")
        return true
    }

    fun updateParameters(newParameters: Scene3DParameters) {
        //val oldParameters = parameters
        parameters = newParameters
    }

    private fun calculateLocationMarkerScenePosition(): Float3 {
        val userLocation = parameters.userLocation!!
        //if (userLocation == null) return null
        val ecef = CoordinateConverter.geodeticToECEF(
            userLocation.x.toDouble(),
            userLocation.y.toDouble(),
            userLocation.z.toDouble()
        )
        //Log.d("LocationMarketLookAt", "calculateLocationMarkerScenePosition")
        return CoordinateConverter.ecefToScenePos(ecef)
    }

    private fun calculateLocationMarkerDirection(
        verticalToWorld: Boolean
    ): Float3 {
        val dir = locationMarkerNode.position.normalized()
        return if (!verticalToWorld) dir
        else Float3(dir.x, 0.0f, dir.z).normalized()
    }

    fun cleanup() {
        locationMarkerNode.let {
            centerNode.removeChildNode(it)
            it.destroy()
        }
    }
}
