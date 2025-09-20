package com.example.gpssatelliteviewer.data.parsers

import android.annotation.SuppressLint
import com.example.gpssatelliteviewer.data.GGA
import com.example.gpssatelliteviewer.data.GSA
import com.example.gpssatelliteviewer.data.GSV
import com.example.gpssatelliteviewer.data.RMC
import com.example.gpssatelliteviewer.data.SatInfo
import com.example.gpssatelliteviewer.data.VTG


object NMEAParser {
    // Cache for message type validation to avoid repeated string operations
    private val messageTypeCache = mutableMapOf<String, String>()
    fun parseGGA(message: String): GGA? {
        // Quick validation: minimum realistic GGA message is around 40-45 chars
        if (message.isBlank() || message.length < 40) return null
        
        // Early validation before expensive split
        if (!message.startsWith("$") || !message.contains("GGA")) return null
        
        val parts = message.split(",")
        if (parts.size < 14) return null

        return try {
            GGA(
                time = formatNmeaTime(parts.getOrNull(1) ?: ""),
                latitude = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                latDirection = parts.getOrNull(3)?.firstOrNull() ?: 'N',
                longitude = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
                lonDirection = parts.getOrNull(5)?.firstOrNull() ?: 'E',
                fixQuality = parts.getOrNull(6)?.toIntOrNull() ?: 0,
                numSatellites = parts.getOrNull(7)?.toIntOrNull() ?: 0,
                horizontalDilution = parts.getOrNull(8)?.toDoubleOrNull() ?: 0.0,
                altitude = parts.getOrNull(9)?.toDoubleOrNull() ?: 0.0,
                altitudeUnits = parts.getOrNull(10)?.firstOrNull() ?: 'M',
                geoidSeparation = parts.getOrNull(11)?.toDoubleOrNull(),
                geoidSeparationUnits = parts.getOrNull(12)?.firstOrNull(),
                dgpsAge = parts.getOrNull(13)?.toDoubleOrNull(),
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
        // Quick validation: minimum GSV message is around 25-30 chars
        if (message.isBlank() || message.length < 25) return null
        
        // Early validation before expensive split
        if (!message.startsWith("$") || !message.contains("GSV")) return null
        
        val parts = message.split(",")
        if (parts.size < 4 || parts[0].length < 6) return null

        return try {
            val talker = parts[0].substring(1, 6)
            val totalMessages = parts.getOrNull(1)?.toIntOrNull() ?: return null
            val messageNumber = parts.getOrNull(2)?.toIntOrNull() ?: return null
            val satellitesInView = parts.getOrNull(3)?.toIntOrNull() ?: 0

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
        // Quick validation: minimum RMC message is around 50-60 chars
        if (message.isBlank() || message.length < 45) return null
        
        // Early validation before expensive split
        if (!message.startsWith("$") || !message.contains("RMC")) return null
        
        val parts = message.split(",")
        if (parts.size < 12) return null

        return try {
            RMC(
                time = formatNmeaTime(parts.getOrNull(1) ?: ""),
                status = parts.getOrNull(2)?.firstOrNull() ?: 'V',
                latitude = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0,
                latDirection = parts.getOrNull(4)?.firstOrNull() ?: 'N',
                longitude = parts.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
                lonDirection = parts.getOrNull(6)?.firstOrNull() ?: 'E',
                speedOverGround = parts.getOrNull(7)?.toDoubleOrNull() ?: 0.0,
                courseOverGround = parts.getOrNull(8)?.toDoubleOrNull() ?: 0.0,
                date = formatNmeaDate(parts.getOrNull(9) ?: ""),
                magneticVariation = parts.getOrNull(10)?.toDoubleOrNull(),
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
        return message.trim()
            .split(",")
            .drop(1)
            .dropLast(1)
    }


    fun getMessageType(message: String): String {
        if (message.isBlank()) return "UNKNOWN"
        
        val parts = message.split(",")
        if (parts.isEmpty() || !parts[0].startsWith("$") || parts[0].length <= 1) {
            return "UNKNOWN"
        }
        
        return parts[0].substring(1)
    }
    
    /**
     * Optimized message type extraction with caching to avoid repeated string operations
     * This is more efficient than the original getMessageType for frequent calls
     */
    fun getMessageTypeOptimized(message: String): String {
        if (message.isBlank() || !message.startsWith("$")) return "UNKNOWN"
        
        // Use first part of message as cache key (up to first comma or 10 chars)
        val cacheKey = message.substring(0, minOf(message.indexOf(',').takeIf { it > 0 } ?: 10, message.length))
        
        return messageTypeCache.getOrPut(cacheKey) {
            val firstComma = message.indexOf(',')
            if (firstComma > 1) {
                message.substring(1, firstComma)
            } else if (message.length > 6) {
                message.substring(1, minOf(10, message.length))
            } else {
                "UNKNOWN"
            }
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

