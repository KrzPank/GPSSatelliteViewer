package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

class LocationMarkerManager(
    private val modelLoader: ModelLoader,
    private val centerNode: Node,
    private var parameters: Scene3DParameters
) {
    private var locationMarkerNode: ModelNode = createLocationMarker()
    private var isVisible: Boolean = true

    private fun createLocationMarker(): ModelNode {
        val node = ModelNode(
            modelInstance = modelLoader.createModelInstance(parameters.locationMarkerModelPath),
            scaleToUnits = parameters.locationMarkerScale
        ).also {
            centerNode.addChildNode(it)
            it.position = Float3(0f, 0f, 0f)
            it.name = "Location marker"
        }
        return node
    }

    fun updateLocationMarker(userLocation: Float3?) {
        if (userLocation == null) {
            setVisible(false)
            return
        }

        val (lat, lon, alt) = userLocation
        val ecefPosition = CoordinateConverter.geodeticToECEF(
            lat.toDouble(),
            lon.toDouble(),
            (alt + 5000f).toDouble()  // Add 5km above surface to make marker visible
        )
        val scenePosition = CoordinateConverter.ecefToScenePos(ecefPosition)

        locationMarkerNode.position = scenePosition
        Log.d("LocationMarkerPos", "${scenePosition}")
    }

    fun setVisible(visible: Boolean) {
        isVisible = visible
        if (visible) {
            if (locationMarkerNode.parent == null) centerNode.addChildNode(locationMarkerNode)
        } else {
            if (locationMarkerNode.parent != null) centerNode.removeChildNode(locationMarkerNode)
        }
    }
    
    fun isLocationMarkerVisible(): Boolean = isVisible

    fun updateLookAt(cameraNode: CameraNode) {
        if (isVisible && locationMarkerNode.parent != null) {
            locationMarkerNode.lookAt(cameraNode.worldPosition)
        }
    }

    fun cleanup() {
        locationMarkerNode.let {
            centerNode.removeChildNode(it)
            it.destroy()
        }
    }
}