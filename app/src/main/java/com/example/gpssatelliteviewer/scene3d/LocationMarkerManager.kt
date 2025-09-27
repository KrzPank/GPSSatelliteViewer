package com.example.gpssatelliteviewer.scene3d

import com.example.gpssatelliteviewer.data.Scene3DParameters
import com.example.gpssatelliteviewer.utils.CoordinateConversion
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

    fun createLocationMarker(): ModelNode {
        val markerInstance = modelLoader.createModelInstance(parameters.locationMarkerModelPath)
        val node = ModelNode(
            modelInstance = markerInstance,
            scaleToUnits = parameters.locationMarkerScale
        ).also {
            centerNode.addChildNode(it)
            // Set initial position to origin (will be updated when location is available)
            it.position = Float3(0f, 0f, 0f)
        }
        return node
    }

    fun updateLocationMarker(userLocation: Triple<Float, Float, Float>) {
        val (lat, lon, alt) = userLocation
        val ecefPosition = CoordinateConversion.geodeticToECEF(
            lat.toDouble(),
            lon.toDouble(),
            (alt + 5000f).toDouble()  // Add 5km above surface to make marker visible
        )
        val scenePosition = CoordinateConversion.ecefToScenePos(ecefPosition)

        locationMarkerNode.position = scenePosition
    }

    fun updateLookAt(cameraNode: CameraNode) {
        locationMarkerNode.lookAt(cameraNode.worldPosition)
    }

    fun cleanup() {
        locationMarkerNode.let {
            centerNode.removeChildNode(it)
            it.destroy()
        }
    }
}