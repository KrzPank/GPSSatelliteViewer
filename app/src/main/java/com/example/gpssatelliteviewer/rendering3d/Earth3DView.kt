package com.example.gpssatelliteviewer.rendering3d


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.addPathNodes
import io.github.sceneview.collision.Sphere
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberNodes

private val DEFAULT_LOCATIONS = listOf(
    40.7128 to -74.0060,   // New York
    34.0522 to -118.2437,  // Los Angeles
    51.5074 to -0.1278,    // London
    48.8566 to 2.3522,     // Paris
    35.6762 to 139.6503,   // Tokyo
    41.8781 to -87.6298,   // Chicago
    29.7604 to -95.3698,   // Houston
    37.7749 to -122.4194,  // San Francisco
    39.7392 to -104.9903,  // Denver
    25.7617 to -80.1918,   // Miami
    43.6532 to -79.3832,   // Toronto
    1.3521 to 103.8198,    // Singapore
    -33.8688 to 151.2093,  // Sydney
    55.7558 to 37.6173,    // Moscow
    52.5200 to 13.4050,    // Berlin
    45.7640 to 4.8357,     // Lyon
    50.8503 to 4.3517,     // Brussels
    59.3293 to 18.0686,    // Stockholm
    41.8719 to 12.4964,    // Rome
    40.4168 to -3.7038,    // Madrid
    48.8584 to 2.2945,     // Paris
    52.3676 to 4.9041,     // Amsterdam
    47.4979 to 19.0402,    // Budapest
    50.0755 to 14.4378,    // Prague
    42.8746 to 74.5822,    // Bishkek
    39.9042 to 116.4074,   // Beijing
    23.8103 to 90.4125     // Dhaka
)


@Composable
fun Earth3DView(){
    Box(modifier = Modifier.fillMaxSize()) {
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val environmentLoader = rememberEnvironmentLoader(engine)

        val centerNode = rememberNode(engine)

        val cameraNode = rememberCameraNode(engine) {
            position = Position(y = 0.5f, z = 3.0f)
            lookAt(centerNode)
            centerNode.addChildNode(this)
        }

        val markers = DEFAULT_LOCATIONS.map { (lat, lon) ->
            val instance = modelLoader.createModelInstance(
                assetFileLocation = "models/RedSphere.glb"
            )
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.02f
            ).apply {
                position = ecefToScenePos(geoToECEF(lat, lon, 1000000.0))
            }
        }
        val earthNode = rememberNode {
            ModelNode(
                modelInstance = modelLoader.createModelInstance(
                    assetFileLocation = "models/Earth_base.glb"
                ),
                scaleToUnits = 1.0f
            )
        }

        centerNode.apply {
            addChildNode(earthNode)
            addChildNode(cameraNode)
        }

        markers.forEach { marker ->
            centerNode.addChildNode(marker)
        }

        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = cameraNode.worldPosition,
                targetPosition = centerNode.worldPosition
            ),
            childNodes = listOf(centerNode),
            environment = environmentLoader.createHDREnvironment(
                assetFileLocation = "envs/sky_2k.hdr"
            )!!,
            onFrame = {
                cameraNode.lookAt(centerNode)
            },
            onGestureListener = rememberOnGestureListener(
            )
        )
    }
}