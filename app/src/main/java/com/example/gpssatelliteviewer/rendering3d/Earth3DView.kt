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
import androidx.compose.ui.Modifier


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

        // Test locations
        val userLat  = dmsToDecimal(52, 24, 0.8, 'N')
        val userLon  = dmsToDecimal(16, 57, 19.92, 'E')

        val ecef = geoToECEF(userLat, userLon, 0.0)
        val userPos = ecefToScenePos(ecef)

        val userNode = rememberNode {
            ModelNode(
                modelInstance = modelLoader.createModelInstance(
                    assetFileLocation = "models/RedSphere.glb"
                ),
                scaleToUnits = 0.02f
            ).apply {
                position = userPos
            }
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
            childNodes = listOf(
                centerNode,
                userNode,
                rememberNode {
                    ModelNode(
                        modelInstance = modelLoader.createModelInstance(
                            assetFileLocation = "models/Earth_base.glb"
                        ),
                        scaleToUnits = 1.0f
                    )
                },
            ),
            environment = environmentLoader.createHDREnvironment(
                assetFileLocation = "envs/sky_2k.hdr"
            )!!,
            onFrame = {
                cameraNode.lookAt(centerNode)
            },
            onGestureListener = rememberOnGestureListener(
                onDoubleTap = { _, node ->
                    node?.apply {
                        scale *= 2.0f
                    }
                }
            )
        )
    }
}