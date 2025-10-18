package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.normalized
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

class LocationMarkerManager(
    private val modelLoader: ModelLoader,
    private val centerNode: Node,
    private var parameters: Scene3DParameters
) {
    private var locationMarkerNode: ModelNode
    private var isVisible: Boolean = true

    private var lastLocationMarkerUpdateTime = 0L
    private val locationMarkerUpdateInterval = 60 * 1000 * 5 // 5 min
    private var firstLocationMarkerUpdate = true

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
            scaleToUnits = parameters.locationMarkerScale
        ).also {
            centerNode.addChildNode(it)
            it.name = "Location marker"
        }
        return node
    }

    private fun updateLocationMarker() {
        if (shouldUpdateLocationMarker()){
            if (firstLocationMarkerUpdate) {
                locationMarkerNode.position = calculateLocationMarkerScenePosition()

                locationMarkerNode.lookTowards(calculateLocationMarkerDirection(verticalToWorld = true))
                //locationMarkerNode.scaleToUnitCube(parameters.locationMarkerScale)

                lastLocationMarkerUpdateTime = System.currentTimeMillis()
                firstLocationMarkerUpdate = false
                //Log.d("LocationMarkerPos", "firstLocationMarkerUpdate")
            }

            val timeSinceLastLocationMarkerUpdate = System.currentTimeMillis() - lastLocationMarkerUpdateTime
            val locationMarkerUpdateIntervalMet = timeSinceLastLocationMarkerUpdate >= locationMarkerUpdateInterval

            if (locationMarkerUpdateIntervalMet) {
                locationMarkerNode.position = calculateLocationMarkerScenePosition()

                locationMarkerNode.lookTowards(calculateLocationMarkerDirection(verticalToWorld = true))

                lastLocationMarkerUpdateTime = System.currentTimeMillis()
                //Log.d("LocationMarkerPos", "locationMarkerUpdateIntervalMet")
            }

            // dodac framecount i brac threashold, zwiekszanie markera od odleglosci satelity
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
        val oldParameters = parameters
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
