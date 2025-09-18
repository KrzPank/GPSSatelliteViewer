package com.example.gpssatelliteviewer.data.parsers

import android.annotation.SuppressLint
import com.example.gpssatelliteviewer.data.GGA
import com.example.gpssatelliteviewer.data.GSA
import com.example.gpssatelliteviewer.data.GSV
import com.example.gpssatelliteviewer.data.RMC
import com.example.gpssatelliteviewer.data.SatInfo
import com.example.gpssatelliteviewer.data.VTG


object NMEAParser {
    fun parseGGA(message: String): GGA? {
        val parts = message.split(",")
        if (parts.size < 14) return null

        return try {
            GGA(
                time = formatNmeaTime(parts[1]),
                latitude = parts[2].toDoubleOrNull() ?: 0.0,
                latDirection = parts[3].firstOrNull() ?: 'N',
                longitude = parts[4].toDoubleOrNull() ?: 0.0,
                lonDirection = parts[5].firstOrNull() ?: 'E',
                fixQuality = parts[6].toIntOrNull() ?: 0,
                numSatellites = parts[7].toIntOrNull() ?: 0,
                horizontalDilution = parts[8].toDoubleOrNull() ?: 0.0,
                altitude = parts[9].toDoubleOrNull() ?: 0.0,
                altitudeUnits = parts[10].firstOrNull() ?: 'M',
                geoidSeparation = parts[11].toDoubleOrNull(),
                geoidSeparationUnits = parts[12].firstOrNull(),
                dgpsAge = parts[13].toDoubleOrNull(),
                dgpsStationId = parts.getOrNull(14)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseGSA(message: String) : GSA? {
        val parts = message.split(",")
        if (parts.size < 15) return null

        return try {
            val mode = parts[1].firstOrNull() ?: 'M'
            val fixType = parts[2].toIntOrNull() ?: 1

            // Extract up to 12 PRN numbers (fields 3–14)
            val satelliteIds = parts.subList(3, 15)
                .mapNotNull { it.toIntOrNull() }

            val pdop = parts.getOrNull(15)?.toDoubleOrNull() ?: 0.0
            val hdop = parts.getOrNull(16)?.toDoubleOrNull() ?: 0.0
            val vdop = parts.getOrNull(17)?.substringBefore("*")?.toDoubleOrNull() ?: 0.0

            // NMEA 4.10 extension: System ID may exist at field 18
            val systemId = parts.getOrNull(18)?.substringBefore("*")?.toIntOrNull()

            GSA(
                mode = mode,
                fixType = fixType,
                satelliteIds = satelliteIds,
                pdop = pdop,
                hdop = hdop,
                vdop = vdop,
                systemId = systemId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseGSV(message: String): GSV? {
        val parts = message.split(",")

        if (parts.size < 4) return null // minimum required fields

        return try {
            val talker = parts[0].substring(1, 6)
            val totalMessages = parts[1].toIntOrNull() ?: return null
            val messageNumber = parts[2].toIntOrNull() ?: return null
            val satellitesInView = parts[3].toIntOrNull() ?: 0

            val satellites = mutableListOf<SatInfo>()

            // Each satellite has 4 fields: PRN, elevation, azimuth, SNR
            var index = 4
            while (index + 3 < parts.size) {
                val prn = parts[index].toIntOrNull()
                val elevation = parts[index + 1].toIntOrNull()
                val azimuth = parts[index + 2].toIntOrNull()
                val snr = parts[index + 3].toIntOrNull()


                if (prn != null) {satellites.add(
                    SatInfo(
                        prn = adjustPrn(talker, prn),
                        elevation = elevation,
                        azimuth = azimuth,
                        snr = snr
                    )
                )}

                index += 4
            }

            GSV(
                totalMessages = totalMessages,
                messageNumber = messageNumber,
                satellitesInView = satellitesInView,
                satellitesInfo = satellites,
                talker = talker
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseRMC(message: String): RMC? {
        val parts = message.split(",")
        if (parts.size < 12) return null

        return try {
            RMC(
                time = formatNmeaTime(parts[1]),
                status = parts[2].firstOrNull() ?: 'V',
                latitude = parts[3].toDoubleOrNull() ?: 0.0,
                latDirection = parts[4].firstOrNull() ?: 'N',
                longitude = parts[5].toDoubleOrNull() ?: 0.0,
                lonDirection = parts[6].firstOrNull() ?: 'E',
                speedOverGround = parts[7].toDoubleOrNull() ?: 0.0,
                courseOverGround = parts[8].toDoubleOrNull() ?: 0.0,
                date = formatNmeaDate(parts[9]),
                magneticVariation = parts[10].toDoubleOrNull(),
                variationDirection = parts.getOrNull(11)?.firstOrNull()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseVTG(message: String): VTG? {
        val parts = message.trim().split(",")
        if (parts.size < 9) return null

        return try {
            VTG(
                courseTrue = parts[1].toDoubleOrNull() ?: 0.0,
                courseMagnetic = parts[3].toDoubleOrNull(),
                speedKnots = parts[5].toDoubleOrNull() ?: 0.0,
                speedKmph = parts[7].toDoubleOrNull() ?: 0.0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseVendorMessage(message: String): List<String> {
        return message.trim().split(",")
    }


    fun getMessageType(message: String): String {
        return message.split(",")[0].substring(1)
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

    // Adjust PRN based on constellation talker ID
    private fun adjustPrn(talker: String, prn: Int): Int {
        return when (talker) {
            "GPGSV" -> if (prn > 32) prn + 87 else prn        // GPS/SBAS
            "GLGSV" -> prn - 64                                // GLONASS
            "GBGSV" -> prn - 100                               // BeiDou
            else -> prn                                     // GA, GQ, etc.
        }
    }
}

