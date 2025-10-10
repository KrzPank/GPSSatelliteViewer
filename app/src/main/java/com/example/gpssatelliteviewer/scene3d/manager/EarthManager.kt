package com.example.gpssatelliteviewer.scene3d.manager

import android.util.Log
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

class EarthManager(
    private val modelLoader: ModelLoader,
    private val centerNode: Node,
    private var parameters: Scene3DParameters = Scene3DParameters()
) {
    private var earthNode: ModelNode = createEarth()
    fun getEarthNode() = earthNode

    var onEarthClick: ((String) -> Unit)? = null

    private fun createEarth(): ModelNode {
        val earthNode = ModelNode(
            modelInstance = modelLoader.createModelInstance(parameters.earthModelPath),
            scaleToUnits = parameters.earthScale
        ).also {
            centerNode.addChildNode(it)
            it.name = "EarthNode"
            it.onSingleTapUp = {
                onEarthClick?.invoke(earthNode.name.toString())
                true
            }
        }
        return earthNode
    }

    fun updateEarthParameters(newParameters: Scene3DParameters) {
        val oldParameters = parameters
        parameters = newParameters

        if (oldParameters.earthModelPath != parameters.earthModelPath ||
            oldParameters.earthScale != parameters.earthScale) {
            updateEarthModel()
        }
    }

    private fun updateEarthModel() {
        try {
            earthNode.let { centerNode.removeChildNode(it) }
            earthNode.destroy()

            earthNode = ModelNode(
                modelInstance = modelLoader.createModelInstance(parameters.earthModelPath),
                scaleToUnits = parameters.earthScale
            ).also { centerNode.addChildNode(it) }
        } catch (e: Exception) {
            Log.e("Scene3D", "Failed to update earth model: ${e.message}")
            // Fall back to default if loading fails
            earthNode = ModelNode(
                modelInstance = modelLoader.createModelInstance(Scene3DParameters().earthModelPath),
                scaleToUnits = parameters.earthScale
            ).also { centerNode.addChildNode(it) }
        }
    }

    fun cleanup() {
        earthNode.let { centerNode.removeChildNode(it) }
        earthNode.destroy()
    }
}