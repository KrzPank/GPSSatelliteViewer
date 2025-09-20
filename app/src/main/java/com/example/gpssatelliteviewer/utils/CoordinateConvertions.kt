package com.example.gpssatelliteviewer.utils

import android.annotation.SuppressLint
import android.util.Log
import com.example.gpssatelliteviewer.data.NMEALocationData
import dev.romainguy.kotlin.math.Float3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6378137f
private const val EARTH_RADIUS_WORLD_UNIT = 0.5f
private const val E2 = 6.69437999014e-3

object CoordinateConversion {

    fun dmsToGeodetic(degrees: Int, minutes: Int, seconds: Double, direction: Char): Double {
        var decimal = degrees + minutes / 60.0 + seconds / 3600.0
        if (direction == 'S' || direction == 'W') {
            decimal *= -1
        }
        return decimal
    }

    fun geodeticToECEF(latDeg: Double, lonDeg: Double, alt: Double = 0.0): Float3 {
        val lat = Math.toRadians(latDeg)
        val lon = Math.toRadians(lonDeg)

        val N = EARTH_RADIUS_METERS / sqrt(1 - E2 * sin(lat) * sin(lat))

        val x = (N + alt) * cos(lat) * cos(lon)
        val y = (N + alt) * cos(lat) * sin(lon)
        val z = (N * (1 - E2) + alt) * sin(lat)

        return Float3(x.toFloat(), y.toFloat(), z.toFloat())
    }

    // Scale & remap ECEF → SceneView
    fun ecefToScenePos(
        ecef: Float3,
        yawDeg: Float = -2f,            // optional prime-meridian texture offset
        flipLon: Boolean = false       // set true if East/West appears mirrored
    ): Float3 {
        val s = EARTH_RADIUS_WORLD_UNIT / EARTH_RADIUS_METERS
        // Map axes: X→X, Z→Y (north up), Y→Z (east on equator). Negate Z to match RH/Z-forward.
        var v = Float3(ecef.x * s, ecef.z * s, -ecef.y * s)

        // Optional: flip longitude if east/west looks mirrored
        if (flipLon) v = Float3(v.x, v.y, -v.z)

        // Optional: apply a yaw around Y if your Earth texture’s 0° meridian isn’t aligned
        if (yawDeg != 0f) {
            val r = Math.toRadians(yawDeg.toDouble())
            val c = cos(r).toFloat()
            val si = sin(r).toFloat()
            v = Float3(v.x * c - v.z * si, v.y, v.x * si + v.z * c)
        }

        //Log.e("SceneView", "Model location x:${v.x}, y:${v.y}, z:${v.z}")
        return v
    }

    fun enuToECEF(e: Double, n: Double, u: Double, lat: Double, lon: Double): Triple<Double, Double, Double> {
        val radLat = Math.toRadians(lat)
        val radLon = Math.toRadians(lon)

        val x = -sin(radLon) * e - sin(radLat) * cos(radLon) * n + cos(radLat) * cos(radLon) * u
        val y = cos(radLon) * e - sin(radLat) * sin(radLon) * n + cos(radLat) * sin(radLon) * u
        val z = cos(radLat) * n + sin(radLat) * u

        return Triple(x, y, z)
    }

    fun azElToECEF(
        azimuth: Float,
        elevation: Float,
        userLocation: Triple<Float, Float, Float>, // lat, lon, alt
        altitude: Float = 20200000.0f                // default satellite distance in meters (example for GPS)
    ): Float3 {
        val (lat, lon, alt) = userLocation

        // Convert user position to ECEF
        val (userX, userY, userZ) = geodeticToECEF(lat.toDouble(), lon.toDouble(), alt.toDouble())

        // Convert az/el to ENU vector
        val azRad = Math.toRadians(azimuth.toDouble())
        val elRad = Math.toRadians(elevation.toDouble())

        val e = altitude * cos(elRad) * sin(azRad)
        val n = altitude * cos(elRad) * cos(azRad)
        val u = altitude * sin(elRad)

        // Rotate ENU to ECEF
        val (dx, dy, dz) = enuToECEF(e, n, u, lat.toDouble(), lon.toDouble())

        // Final ECEF coordinates of satellite
        return Float3(
            (userX + dx).toFloat(),
            (userY + dy).toFloat(),
            (userZ + dz).toFloat()
        )
    }

    @SuppressLint("DefaultLocale")
    fun geodeticToDMS(value: Double, hemisphere: Char): String {
        if (value == 0.0) return "0°0'0\" N/A"

        // NMEA coordinates are in DDMM.MMMM format (degrees + minutes as decimal)
        val degrees = (value / 100).toInt()
        val minutesDecimal = value - (degrees * 100)
        val minutes = minutesDecimal.toInt()
        val seconds = (minutesDecimal - minutes) * 60

        return String.format("%d°%02d'%06.3f\" %c", degrees, minutes, seconds, hemisphere)
    }

    @SuppressLint("DefaultLocale")
    fun nmeaCoordinateToDecimal(nmeaCoord: Double, hemisphere: Char): Double {
        if (nmeaCoord == 0.0) return 0.0
        
        val degrees = (nmeaCoord / 100).toInt()
        val minutes = nmeaCoord - (degrees * 100)
        var decimal = degrees + (minutes / 60.0)
        
        if (hemisphere == 'S' || hemisphere == 'W') {
            decimal *= -1
        }
        return decimal
    }

    /**
     * Gets accuracy estimate based on HDOP and satellite count as a Float value
     */
    @SuppressLint("DefaultLocale")
    fun getAccuracyEstimate(locationNMEA: NMEALocationData): String? {
        if (locationNMEA.hdop <= 0 || locationNMEA.numSatellites <= 0) return null

        // Rough accuracy estimation based on HDOP
        val baseAccuracy = 5.0f // meters
        val hdopFactor = locationNMEA.hdop.toFloat()

        val satelliteFactor = when {
            locationNMEA.numSatellites >= 12 -> 0.8f
            locationNMEA.numSatellites >= 8 -> 1.0f
            locationNMEA.numSatellites >= 6 -> 1.2f
            locationNMEA.numSatellites >= 4 -> 1.5f
            else -> 2.0f
        }

        val accuracy = baseAccuracy * hdopFactor * satelliteFactor
        return String.format("%.1f", accuracy)
    }
}
