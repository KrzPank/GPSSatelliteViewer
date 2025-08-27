package com.example.gpssatelliteviewer.utility

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

        val latitudeDecimal = convertToDecimalDegrees(lat, latHem)
        val longitudeDecimal = convertToDecimalDegrees(lon, longHem)

        val latitude = toDMS(latitudeDecimal, latHem)
        val longitude = toDMS(longitudeDecimal, longHem)

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

private fun toDMS(value: Double, hemisphere: String): String {
    val degrees = value.toInt()
    val minutesDecimal = (Math.abs(value - degrees)) * 60
    val minutes = minutesDecimal.toInt()
    val seconds = ((minutesDecimal - minutes) * 60).toInt()
    return String.format("%d°%d'%d\" %s", degrees, minutes, seconds, hemisphere)
}


private fun convertToDecimalDegrees(value: Double, hemisphere: String): Double {
    val degrees = (value / 100).toInt()
    val minutes = value - (degrees * 100)
    var decimal = degrees + minutes / 60
    if (hemisphere == "S" || hemisphere == "W") decimal *= -1
    return decimal
}

private fun formatNmeaTime(nmeaTime: String): String {
    return try {
        if (nmeaTime.length >= 6) {
            val hh = nmeaTime.substring(0, 2)
            val mm = nmeaTime.substring(2, 4)
            val ss = nmeaTime.substring(4, 6)
            "$hh:$mm:$ss"
        } else nmeaTime
    } catch (e: Exception) {
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
