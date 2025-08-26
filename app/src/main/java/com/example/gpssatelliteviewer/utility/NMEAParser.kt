package com.example.gpssatelliteviewer.utility

import com.example.gpssatelliteviewer.data.NMEAData

fun parseGGA(message: String): NMEAData? {
    try {
        val parts = message.split(",")
        if (parts.size < 15) return null

        val time = parts[1]

        val lat = parts[2].toDoubleOrNull() ?: 0.0
        val latHem = parts[3]
        val lon = parts[4].toDoubleOrNull() ?: 0.0
        val lonHem = parts[5]

        val fixQuality = parts[6].toIntOrNull() ?: 0
        val numSatellites = parts[7].toIntOrNull() ?: 0
        val horizontalDilution = parts[8].toFloatOrNull() ?: 0f
        val altitude = parts[9].toDoubleOrNull() ?: 0.0
        val heightOfGeoid = parts[11].toDoubleOrNull() ?: 0.0

        val latitude = convertToDecimalDegrees(lat, latHem)
        val longitude = convertToDecimalDegrees(lon, lonHem)

        return NMEAData(
            time,
            latitude,
            longitude,
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

private fun convertToDecimalDegrees(value: Double, hemisphere: String): Double {
    val degrees = (value / 100).toInt()
    val minutes = value - (degrees * 100)
    var decimal = degrees + minutes / 60
    if (hemisphere == "S" || hemisphere == "W") decimal *= -1
    return decimal
}
