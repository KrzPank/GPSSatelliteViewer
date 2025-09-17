package com.example.gpssatelliteviewer.utils

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.utils.CoordinateConversion.azElToECEF
import com.google.android.filament.Engine
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberOnGestureListener


class Scene3D(
    private val modifier: Modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .imePadding(),
    private val environmentLoader: EnvironmentLoader,
    private val modelLoader: ModelLoader,
    private val engine: Engine
) {
    /*
    Orbit heights were taken from https://en.wikipedia.org/wiki/Satellite_navigation
    and https://en.wikipedia.org/wiki/List_of_BeiDou_satellites
    */
    private val keysFor35786000 = setOf(1,2,3,4,5,6,7,8,9,10,13,16,31,38,39,40,56,59,60,61,62)
    private val constellationAltitudes = mapOf(
        "GLONASS" to 19100000f,
        "Galileo" to 23222000f,
        "QZSS" to 35800000f, // average height (elliptical orbit 32,600–39,000 km)
        "IRNSS" to 36000000f,
        "SBAS" to 35786000f, // Usually GEO
        "BeiDou" to 21500000f,
        "GPS" to 20180000f,
        "Unknown" to 0f,
        "Other" to 0f
    )
    private val centerNode = Node(engine)
    private val cameraNode = CameraNode(engine).apply {
        position = Float3(y = 0.5f, z = 3.0f)
        lookAt(centerNode)
        centerNode.addChildNode(this)
    }

    private val satelliteNodes = mutableListOf<ModelNode>()
    private var earthNode: ModelNode? = null

    var menuVisible: Boolean = true

    init {
        setupScene()
    }

    private fun setupScene() {
        earthNode = ModelNode(
            modelInstance = modelLoader.createModelInstance("models/Earth_base.glb"),
            scaleToUnits = 1.0f
        ).also { centerNode.addChildNode(it) }
    }

    @Composable
    fun Render() {
        Scene(
            modifier = modifier,
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = cameraNode.worldPosition,
                targetPosition = centerNode.worldPosition
            ),
            childNodes = listOf(centerNode),
            environment = environmentLoader.createHDREnvironment("envs/sky_2k.hdr")!!,
            onFrame = { updateCameraAndSatellites() },
            onGestureListener = rememberOnGestureListener (
                onDoubleTap = {_, node ->
                    toggleMenu()
                }
            )
        )
    }

    private fun updateCameraAndSatellites() {
        cameraNode.lookAt(centerNode)
        satelliteNodes.forEach { satellite ->
            satellite.lookAt(cameraNode.worldPosition)
        }
    }

    fun updateSatellites(satelliteList: List<GNSSStatusData>, userLocation: Triple<Float, Float, Float>) {
        cleanupSatellites()

        satelliteList.forEach { sat ->
            val instance = modelLoader.createModelInstance("models/RedCircle.glb")
            val satelliteNode = ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.05f
            ).apply {
                val altitude = calculateSatelliteAltitude(sat)
                val pos = CoordinateConversion.ecefToScenePos(
                    azElToECEF(
                        sat.azimuth,
                        sat.elevation,
                        userLocation,
                        altitude
                    )
                )

                if (altitude == 0f) Log.e("SatPos", "Sid:${sat.constellation} position: ${pos.x}, y:${pos.y}, z:${pos.z}")

                position = pos
                centerNode.addChildNode(this)
            }
            satelliteNodes.add(satelliteNode)
        }
    }

    private fun toggleMenu(): Boolean {
        menuVisible = !menuVisible
        return menuVisible
    }


    private fun calculateSatelliteAltitude(sat: GNSSStatusData): Float {
        return when {
            sat.constellation == "BeiDou" && sat.prn in keysFor35786000 -> 35786000f
            sat.constellation in constellationAltitudes -> constellationAltitudes[sat.constellation]!!
            else -> 0f
        }
    }

    fun cleanup() {
        cleanupSatellites()
        earthNode?.destroy()
        cameraNode.destroy()
        centerNode.destroy()
    }

    private fun cleanupSatellites() {
        satelliteNodes.forEach { node ->
            node.destroy()
        }
        satelliteNodes.clear()
    }
}