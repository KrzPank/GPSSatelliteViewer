package com.example.gpssatelliteviewer.utility

import android.annotation.SuppressLint
import com.example.gpssatelliteviewer.data.NMEAData

fun parseGGA(message: String): NMEAData? {
    try {
        val parts = message.split(",")
        if (parts.size < 15) return null

        val t = parts[1]
        val time = formatNmeaTime(t)

        val lat = parts[2].toDoubleOrNull() ?: 0.0
        val latHem = parts[3]
        val lon = parts[4].toDoubleOrNull() ?: 0.0
        val longHem = parts[5]

        val fixQ = parts[6].toIntOrNull() ?: 0
        val fixQuality = mapFixQuality(fixQ)
        val numSatellites = parts[7].toIntOrNull() ?: 0
        val horizontalDilution = parts[8].toFloatOrNull() ?: 0f
        val altitude = parts[9].toDoubleOrNull() ?: 0.0
        val heightOfGeoid = parts[11].toDoubleOrNull() ?: 0.0

        val latitude = convertToDMS(lat, latHem)
        val longitude = convertToDMS(lon, longHem)

        return NMEAData(
            time,
            latitude,
            latHem,
            longitude,
            longHem,
            fixQuality,
            numSatellites,
            horizontalDilution,
            altitude,
            heightOfGeoid
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}


@SuppressLint("DefaultLocale")
fun convertToDMS(value: Double, hemisphere: String): String {
    if (value == 0.0) return "0°0'0\""

    val degrees = (value / 100).toInt()          // extract degrees
    val minutesFull = value - (degrees * 100)    // extract minutes (with decimals)
    val minutes = minutesFull.toInt()
    val seconds = ((minutesFull - minutes) * 60)

    return String.format("%d°%d'%.3f\" %s", degrees, minutes, seconds, hemisphere)
}


private fun formatNmeaTime(nmeaTime: String): String {
    return try {
        if (nmeaTime.length >= 6) {
            val hh = nmeaTime.substring(0, 2)
            val mm = nmeaTime.substring(2, 4)
            val ss = nmeaTime.substring(4, 6)
            "$hh:$mm:$ss"
        } else nmeaTime
    } catch (_: Exception) {
        nmeaTime
    }
}

private fun mapFixQuality(fixQuality: Int): String {
    return when(fixQuality) {
        0 -> "Brak"
        1 -> "GPS"
        2 -> "DGPS"
        3 -> "PPS"
        4 -> "RTK"
        5 -> "Float RTK"
        6 -> "Estymowany"
        7 -> "Manualny"
        8 -> "Simulowany"
        else -> "Nieznany"
    }
}
