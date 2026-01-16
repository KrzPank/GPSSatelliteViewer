package com.example.gpssatelliteviewer.scene3d.manager

import com.example.gpssatelliteviewer.data.minRecreationInterval
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.scene3d.manager.camera.CameraManager
import com.example.gpssatelliteviewer.data.parser.CoordinateConverter
import com.example.gpssatelliteviewer.utils.distance
import com.example.gpssatelliteviewer.utils.normalized
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.min

private const val MAX_SCALE_SIZE = 0.2f
private const val MIN_SCALE_SIZE = 0.008f
private const val scale = 0.05f
private const val locationMarkerUpdateInterval = 60 * 1000 * 5 // every 5 min

class LocationMarkerManager(
    private val modelLoader: ModelLoader,
    private val centerNode: Node,
    private val cameraManager: CameraManager,
    private var parameters: Scene3DParameters
) {
    private var locationMarkerNode: ModelNode
    private var isVisible: Boolean = true

    private var lastRecreationTime = 0L

    private var lastLocationMarkerUpdateTime = 0L
    private var firstLocationMarkerUpdate = true
    private var locationMarkerScale = 0.1f

    // need to call it from init
    init {
        locationMarkerNode = createLocationMarker()
        if (shouldUpdateLocationMarker()) updateLocationMarker()
    }

    fun isLocationMarkerVisible() = isVisible

    fun setVisible(visible: Boolean) {
        isVisible = visible
        if (visible) {
            if (locationMarkerNode.parent == null) {
                centerNode.addChildNode(locationMarkerNode)
                firstLocationMarkerUpdate = true
            }
        } else {
            if (locationMarkerNode.parent != null) {
                centerNode.removeChildNode(locationMarkerNode)
                firstLocationMarkerUpdate = false
            }
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
        if (!shouldUpdateLocationMarker()) return
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRecreationTime < minRecreationInterval) return
        lastRecreationTime = currentTime

        // small helpers to avoid duplication
        val updatePosition: () -> Unit = {
            locationMarkerNode.position = calculateLocationMarkerScenePosition()
            locationMarkerNode.lookTowards(calculateLocationMarkerDirection(followUser = false))
        }
        val updateScale: (Float) -> Unit = { dist ->
            val s = scale * dist + MIN_SCALE_SIZE
            locationMarkerScale = min(s, MAX_SCALE_SIZE)
            locationMarkerNode.scaleToUnitCube(locationMarkerScale)
        }

        if (firstLocationMarkerUpdate) {
            val camPos = cameraManager.getCameraPosition()
            val dist = distance(camPos, locationMarkerNode.position)
            updateScale(dist)
            updatePosition()
            lastLocationMarkerUpdateTime = currentTime
            firstLocationMarkerUpdate = false
            //Log.d("LocationMarkerPos", "firstLocationMarkerUpdate")
            return
        }

        if (currentTime - lastLocationMarkerUpdateTime >= locationMarkerUpdateInterval) {
            updatePosition()
            lastLocationMarkerUpdateTime = currentTime
        }

        val cameraMovement = cameraManager.getCameraMovedUnits()
        val shouldUpdateScale = cameraMovement > cameraManager.getDynamicUpdateThreshold()

        if (shouldUpdateScale) {
            val camPos = cameraManager.getCameraPosition()
            val dist = distance(camPos, locationMarkerNode.position)
            updateScale(dist)
            updatePosition()
            lastLocationMarkerUpdateTime = currentTime
            //Log.d("locationMarkerScale", " locationMarkerScale=$locationMarkerScale distToMarker=$dist")
        }
    }

    private fun shouldUpdateLocationMarker(): Boolean {
        if (parameters.userLocation == null) {
            setVisible(false)
            return false
        }

        if (!isVisible) {
            setVisible(false)
            return false
        }

        if (locationMarkerNode.parent == null) {
            return false
        }

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

    private fun calculateLocationMarkerDirection(followUser: Boolean): Float3 {
        val dir = if (followUser) cameraManager.getCameraPosition().normalized()
            else locationMarkerNode.position.normalized()
        return Float3(dir.x, 0.0f, dir.z).normalized()
    }

    fun cleanup() {
        locationMarkerNode.let {
            centerNode.removeChildNode(it)
            it.destroy()
        }
    }
}
