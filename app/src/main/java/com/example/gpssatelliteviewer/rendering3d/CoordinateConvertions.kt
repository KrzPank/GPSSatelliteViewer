package com.example.gpssatelliteviewer.rendering3d

import android.util.Log
import dev.romainguy.kotlin.math.Float3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


fun dmsToDecimal(degrees: Int, minutes: Int, seconds: Double, direction: Char): Double {
    var decimal = degrees + minutes / 60.0 + seconds / 3600.0
    if (direction == 'S' || direction == 'W') {
        decimal *= -1
    }
    return decimal
}

fun geoToECEF(latDeg: Double, lonDeg: Double, alt: Double = 0.0): Float3 {
    val a = 6378137.0        // semi-major axis
    val e2 = 6.69437999014e-3 // eccentricity squared

    val lat = Math.toRadians(latDeg)
    val lon = Math.toRadians(lonDeg)

    val N = a / sqrt(1 - e2 * sin(lat) * sin(lat))

    val x = (N + alt) * cos(lat) * cos(lon)
    val y = (N + alt) * cos(lat) * sin(lon)
    val z = (N * (1 - e2) + alt) * sin(lat)

    return Float3(x.toFloat(), y.toFloat(), z.toFloat())
}

// Scale & remap ECEF → SceneView
fun ecefToScenePos(
    ecef: Float3,
    modelRadius: Float = 0.5f,     // Earth node radius in world units
    earthRadius: Float = 6378137f, // meters
    yawDeg: Float = -2f,            // optional prime-meridian texture offset
    flipLon: Boolean = false       // set true if East/West appears mirrored
): Float3 {
    val s = modelRadius / earthRadius
    // Map axes: X→X, Z→Y (north up), Y→Z (east on equator). Negate Z to match RH/Z-forward.
    var v = Float3(ecef.x * s, ecef.z * s, -ecef.y * s)

    // Optional: flip longitude if east/west looks mirrored
    if (flipLon) v = Float3(v.x, v.y, -v.z)

    // Optional: apply a yaw around Y if your Earth texture’s 0° meridian isn’t aligned
    if (yawDeg != 0f) {
        val r = Math.toRadians(yawDeg.toDouble())
        val c = kotlin.math.cos(r).toFloat()
        val si = kotlin.math.sin(r).toFloat()
        v = Float3(v.x * c - v.z * si, v.y, v.x * si + v.z * c)
    }

    //Log.e("SceneView", "Model location x:${v.x}, y:${v.y}, z:${v.z}")
    return v
}

fun azElToScenePos(azimuth: Float, elevation: Float, radius: Double): Float3 {
    val az = Math.toRadians(azimuth.toDouble())
    val el = Math.toRadians(elevation.toDouble())

    val x = radius * cos(el) * sin(az)
    val y = radius * sin(el)
    val z = radius * cos(el) * cos(az)

    return Float3(x.toFloat(), y.toFloat(), z.toFloat())
}

