package com.example.gnssmap.data

// Sealed class hierarchy for NMEA messages

// check parser if they get all of the data ??
sealed class NMEAMessage {
    abstract val messageType: String

    data class GGA(
        val time: String,
        val latitude: Double,
        val latDirection: Char,
        val longitude: Double,
        val lonDirection: Char,
        val fixQuality: Int,
        val satelliteCount: Int,
        val horizontalDilution: Double,
        val altitude: Double,
        val altitudeUnits: Char,
        val geoidSeparation: Double?,
        val geoidSeparationUnits: Char?,
        val dgpsAge: Double?,
        val checksum: String?
    ) : NMEAMessage() {
        override val messageType = "GGA"
    }

    data class RMC(
        val time: String,
        val status: Char,
        val latitude: Double,
        val latDirection: Char,
        val longitude: Double,
        val lonDirection: Char,
        val speedOverGround: Double,
        val courseOverGround: Double,
        val date: String,
        val magneticVariation: Double?,
        val variationDirection: Char?
    ) : NMEAMessage() {
        override val messageType = "RMC"
    }

    data class GSA(
        val mode: Char,
        val fixType: Int,
        val satelliteIds: List<Int>,
        val pdop: Double,
        val hdop: Double,
        val vdop: Double,
        val systemId: Int?
    ) : NMEAMessage() {
        override val messageType = "GSA"
    }

    data class GSV(
        val totalMessages: Int,
        val messageNumber: Int,
        val satellitesInView: Int,
        val satellitesInfo: List<SatInfo>,
        val talker: String
    ) : NMEAMessage() {
        override val messageType = "GSV"
    }

    data class VTG(
        val courseTrue: Double,
        val courseMagnetic: Double?,
        val speedKnots: Double,
        val speedKmph: Double
    ) : NMEAMessage() {
        override val messageType = "VTG"
    }

    data class Unknown(
        val rawMessage: String,
        val type: String
    ) : NMEAMessage() {
        override val messageType = type
    }
}

data class NMEALocationData(
    val time: String = "",
    val date: String = "",
    val latitude: Double = 0.0,
    val latHemisphere: Char = 0.toChar(),
    val longitude: Double = 0.0,
    val lonHemisphere: Char = 0.toChar(),
    val fixQuality: Int = 0,
    val fixType: Int = 0,
    val numSatellites: Int = 0,
    val pdop: Double = 99.0,
    val hdop: Double = 99.0,
    val vdop: Double = 99.0,
    val altitude: Double = 0.0,
    val geoidHeight: Double = 0.0,
    val mslAltitude: Double = 0.0,
    val speedKnots: Double = 0.0,
    val course: Double = 0.0,
    val magneticVariation: Double = 0.0
) {
    companion object {
        fun combine(messages: Map<String, NMEAMessage>): NMEALocationData {
            val gga = messages["GGA"] as? NMEAMessage.GGA
            val rmc = messages["RMC"] as? NMEAMessage.RMC
            val gsa = messages["GSA"] as? NMEAMessage.GSA

            return NMEALocationData(
                time = rmc?.time ?: gga?.time ?: "",
                date = rmc?.date ?: "",
                latitude = gga?.latitude?.takeIf { it != 0.0 } ?: (rmc?.latitude ?: 0.0),
                latHemisphere = gga?.latDirection ?: rmc?.latDirection ?: 'N',
                longitude = gga?.longitude?.takeIf { it != 0.0 } ?: (rmc?.longitude ?: 0.0),
                lonHemisphere = gga?.lonDirection ?: rmc?.lonDirection ?: 'E',
                fixQuality = gga?.fixQuality ?: 0,
                fixType = gsa?.fixType ?: 0,
                numSatellites = gga?.satelliteCount ?: 0,
                pdop = gsa?.pdop ?: 99.0,
                hdop = gsa?.hdop ?: 99.0,
                vdop = gsa?.vdop ?: 99.0,
                altitude = gga?.altitude ?: 0.0,
                geoidHeight = gga?.geoidSeparation ?: 0.0,
                mslAltitude = gga?.let { it.altitude + (it.geoidSeparation ?: 0.0) } ?: 0.0,
                speedKnots = rmc?.speedOverGround ?: 0.0,
                course = rmc?.courseOverGround ?: 0.0,
                magneticVariation = rmc?.magneticVariation ?: 0.0
            )
        }
    }
}

data class SatInfo(
    val prn: Int,
    val elevation: Int?,
    val azimuth: Int?,
    val snr: Int?
)
