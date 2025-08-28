package com.example.gpssatelliteviewer.utility

import android.annotation.SuppressLint
import com.example.gpssatelliteviewer.data.NMEAData

fun parseGGA(message: String, existing: NMEAData? = null): NMEAData? {
    val parts = message.split(",")
    if (parts.size < 14) return existing

    val data = existing ?: NMEAData()

    try {
        val time = formatNmeaTime(parts[1])
        val lat = parts[2].toDoubleOrNull() ?: 0.0
        val latHem = parts[3]
        val lon = parts[4].toDoubleOrNull() ?: 0.0
        val lonHem = parts[5]
        val fixQ = parts[6].toIntOrNull() ?: 0
        val numSatellites = parts[7].toIntOrNull()
        val hdop = parts[8].toFloatOrNull()
        val altitude = parts[9].toDoubleOrNull()
        val geoidHeight = parts[11].toDoubleOrNull()

        data.time = time
        if (lat != 0.0 && lon != 0.0) {
            data.latitude = convertToDMS(lat, latHem)
            data.longitude = convertToDMS(lon, lonHem)
        }

        data.fixQuality = mapFixQuality(fixQ)
        data.numSatellites = numSatellites
        data.hdop = hdop
        data.altitude = altitude
        data.geoidHeight = geoidHeight

    } catch (e: Exception) {
        e.printStackTrace()
    }

    return data
}

fun parseRMC(message: String, existing: NMEAData? = null): NMEAData? {
    val parts = message.split(",")
    if (parts.size < 12) return existing

    val data = existing ?: NMEAData()

    try {
        val time = formatNmeaTime(parts[1])
        val status = parts[2] // A=active, V=void
        val lat = parts[3].toDoubleOrNull() ?: 0.0
        val latHem = parts[4]
        val lon = parts[5].toDoubleOrNull() ?: 0.0
        val lonHem = parts[6]
        val speedKnots = parts[7].toDoubleOrNull()
        val course = parts[8].toDoubleOrNull()
        val date = formatNmeaDate(parts[9])
        val magVar = parts[10].toDoubleOrNull()
        val magVarHem = parts.getOrNull(11)

        data.time = time
        data.date = date

        if (lat != 0.0 && lon != 0.0) {
            data.latitude = convertToDMS(lat, latHem)
            data.longitude = convertToDMS(lon, lonHem)
        }

        data.speedKnots = speedKnots
        data.course = course

        data.magneticVariation = if (magVar != null && magVarHem != null) {
            if (magVarHem.equals("W", ignoreCase = true)) -magVar else magVar
        } else magVar

    } catch (e: Exception) {
        e.printStackTrace()
    }

    return data
}

fun parseGBS(message: String, existing: NMEAData? = null): NMEAData? {
    val parts = message.split(",")
    if (parts.size < 5) return existing

    val data = existing ?: NMEAData()

    try {
        val errLat = parts[2].toDoubleOrNull()
        val errLon = parts[3].toDoubleOrNull()
        val errAlt = parts[4].toDoubleOrNull()

        data.gbsErrors = listOfNotNull(errLat, errLon, errAlt)

    } catch (e: Exception) {
        e.printStackTrace()
    }

    return data
}

fun approximateLocationAccuracy(existing: NMEAData? = null, sensorAccuracyM: Double = 5.0): String {
    val data = existing ?: NMEAData()

    val hdopAccuracy = data.hdop?.times(sensorAccuracyM)

    val gbsError = data.gbsErrors?.let { list ->
        if (list.isNotEmpty()) list.sum() else null
    }

    val totalAccuracy = listOfNotNull(hdopAccuracy, gbsError).sum()

    return if (totalAccuracy > 0) {
        "%.1f m".format(totalAccuracy)
    } else {
        "Brak danych"
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

@SuppressLint("DefaultLocale")
private fun formatNmeaDate(datestr: String?): String? {
    if (datestr.isNullOrBlank() || datestr.length != 6) return null
    else{
        val day = datestr.substring(0, 2).toInt()
        val month = datestr.substring(2, 4).toInt()
        val year = datestr.substring(4, 6).toInt()
        val fullyear = 2000 + year

        return String.format("%04d-%02d-%02d", fullyear, month, day)
    }
}

@SuppressLint("DefaultLocale")
private fun formatNmeaTime(nmeaTime: String?): String? {
    if (nmeaTime.isNullOrBlank() || nmeaTime.length < 6) return null
    else {
        val hh = nmeaTime.substring(0, 2).toInt()
        val mm = nmeaTime.substring(2, 4).toInt()
        val ss = nmeaTime.substring(4, 6).toInt()
        return String.format("%02d:%02d:%02d", hh, mm, ss)
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
