package com.example.gpssatelliteviewer.ui.cards

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.utils.CoordinateConversion
import com.example.gpssatelliteviewer.utils.CoordinateConversion.azElToECEF
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener

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

/*
Orbit heights were taken from https://en.wikipedia.org/wiki/Satellite_navigation
and https://en.wikipedia.org/wiki/List_of_BeiDou_satellites
 */
val keysFor35786000 = setOf(1,2,3,4,5,6,7,8,9,10,13,16,31,38,39,40,56,59,60,61,62)

val constellationAltitudes = mapOf(
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

@Composable
fun Earth3DView(
    satelliteList: List<GNSSStatusData>,
    userLocation: Triple<Float, Float, Float>
) {
    Box(modifier = Modifier.Companion.fillMaxSize()) {
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val environmentLoader = rememberEnvironmentLoader(engine)

        val centerNode = rememberNode(engine)

        val cameraNode = rememberCameraNode(engine) {
            position = Float3(y = 0.5f, z = 3.0f)
            lookAt(centerNode)
            centerNode.addChildNode(this)
        }
    /* *** NEED TO ADD DESTRUCTION OF PREVIOUS SATELLITENODES RIGHT NOW IM JUST RENDERING NEW ONES WITHOUT CLEANING UP PREVIOUS
    *       OR ADD A CHECK TO ONLY ADD SATELLITES WHICH ARE NEW     WILL SEE
    *  */
        val satelliteNode = satelliteList.map { sat ->
            val instance = modelLoader.createModelInstance(
                assetFileLocation = "models/RedCircle.glb"
            )
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.05f
            ).apply {
                val altitude = when {
                    sat.constellation == "BeiDou" && sat.prn in keysFor35786000 -> 35786000f
                    sat.constellation in constellationAltitudes -> constellationAltitudes[sat.constellation]!!
                    else -> 0f
                }

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

        satelliteNode.forEach { satNode ->
            centerNode.addChildNode(satNode)
        }

        Scene(
            modifier = Modifier.Companion.fillMaxSize(),
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
                satelliteNode.forEach { satellite ->
                    satellite.lookAt(cameraNode.worldPosition)
                }
            },
            onGestureListener = rememberOnGestureListener(
            )
        )
    }
}