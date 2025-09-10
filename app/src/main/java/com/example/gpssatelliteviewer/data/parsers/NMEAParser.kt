package com.example.gpssatelliteviewer.data.parsers

import android.annotation.SuppressLint
import com.example.gpssatelliteviewer.data.NMEAData

object NMEAParser {
    fun parseGGA(message: String, existing: NMEAData = NMEAData()): NMEAData {
        val parts = message.split(",")
        if (parts.size < 14) return existing

        return try {
            val time = formatNmeaTime(parts[1])
            val lat = parts[2].toDoubleOrNull()
            val latHem = parts[3]
            val lon = parts[4].toDoubleOrNull()
            val lonHem = parts[5]
            val fixQ = parts[6].toIntOrNull()
            val numSatellites = parts[7].toIntOrNull()
            val hdop = parts[8].toFloatOrNull()
            val altitude = parts[9].toDoubleOrNull()
            val geoidHeight = parts[11].toDoubleOrNull()
            val mslAltitude = if (altitude != null && geoidHeight != null) altitude + geoidHeight else existing.mslAltitude

            existing.copy(
                time = if (time.isNotEmpty()) time else existing.time,
                latitude = lat ?: existing.latitude,
                latHemisphere = if (latHem.isNotEmpty()) latHem else existing.latHemisphere,
                longitude = lon ?: existing.longitude,
                lonHemisphere = if (lonHem.isNotEmpty()) lonHem else existing.lonHemisphere,
                fixQuality = fixQ?.let { mapFixQuality(it) } ?: existing.fixQuality,
                numSatellites = numSatellites ?: existing.numSatellites,
                hdop = hdop ?: existing.hdop,
                altitude = altitude ?: existing.altitude,
                geoidHeight = geoidHeight ?: existing.geoidHeight,
                mslAltitude = mslAltitude
            )
        } catch (e: Exception) {
            e.printStackTrace()
            existing
        }
    }

    fun parseRMC(message: String, existing: NMEAData = NMEAData()): NMEAData {
        val parts = message.split(",")
        if (parts.size < 12) return existing

        return try {
            val time = formatNmeaTime(parts[1])
            val lat = parts[3].toDoubleOrNull()
            val latHem = parts[4]
            val lon = parts[5].toDoubleOrNull()
            val lonHem = parts[6]
            val speedKnots = parts[7].toDoubleOrNull()
            val course = parts[8].toDoubleOrNull()
            val date = formatNmeaDate(parts[9])
            val magVar = parts[10].toDoubleOrNull()
            val magVarHem = parts.getOrNull(11)
            val magneticVariation = if (magVar != null && magVarHem != null && magVarHem.equals("W", true)) -magVar else magVar ?: existing.magneticVariation

            existing.copy(
                time = if (time.isNotEmpty()) time else existing.time,
                date = if (date.isNotEmpty()) date else existing.date,
                latitude = lat ?: existing.latitude,
                latHemisphere = if (latHem.isNotEmpty()) latHem else existing.latHemisphere,
                longitude = lon ?: existing.longitude,
                lonHemisphere = if (lonHem.isNotEmpty()) lonHem else existing.lonHemisphere,
                speedKnots = speedKnots ?: existing.speedKnots,
                course = course ?: existing.course,
                magneticVariation = magneticVariation
            )
        } catch (e: Exception) {
            e.printStackTrace()
            existing
        }
    }

    fun parseGBS(message: String, existing: NMEAData = NMEAData()): NMEAData {
        val parts = message.split(",")
        if (parts.size < 5) return existing

        return try {
            val errors = listOfNotNull(
                parts[2].toDoubleOrNull(),
                parts[3].toDoubleOrNull(),
                parts[4].toDoubleOrNull()
            )
            existing.copy(gbsErrors = errors)
        } catch (e: Exception) {
            e.printStackTrace()
            existing
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatNmeaDate(datestr: String): String {
        if (datestr.isBlank() || datestr.length != 6) return ""
        else{
            val day = datestr.substring(0, 2).toInt()
            val month = datestr.substring(2, 4).toInt()
            val year = datestr.substring(4, 6).toInt()
            val fullyear = 2000 + year

            //return String.format("%04d-%02d-%02d", fullyear, month, day)
            return String.format("%02d-%02d-%03d", day, month, fullyear)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatNmeaTime(nmeaTime: String): String {
        if (nmeaTime.isBlank() || nmeaTime.length < 6) return ""
        else {
            val hh = nmeaTime.substring(0, 2).toInt()
            val mm = nmeaTime.substring(2, 4).toInt()
            val ss = nmeaTime.substring(4, 6).toInt()
            return String.format("%02d:%02d:%02d", hh, mm, ss)
        }
    }

    private fun mapFixQuality(fixQuality: Int): String {
        return when(fixQuality) {
            0 -> "Brak danych"
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
}

