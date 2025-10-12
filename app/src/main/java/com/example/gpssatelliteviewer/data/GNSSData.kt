package com.example.gpssatelliteviewer.data

import android.location.GnssCapabilities

data class GNSSStatusData(
    val constellation: String,
    val prn: Int,
    val snr: Float,
    val usedInFix: Boolean,
    val azimuth: Float,
    val elevation: Float
)

data class GNSSHardwareInfo(
    val modelName: String? = null,
    val hardwareYear: Int? = null,
    val capabilities: GnssCapabilities? = null
)

data class GNSSMeasurementData(
    val svid: Int,
    val constellation: String,
    val cn0DbHz: Double?,
    val snrInDb: Double?,
    val accumulatedDeltaRangeMeters: Double?,
    val pseudorangeRateMetersPerSecond: Double?,
    val accumulatedDeltaRangeUncertaintyMeters: Double?,
    val timeOffsetNanos: Double?
)

data class ListenerData(
    val time: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val verticalAccuracy: Float? = null,  // available API 26+
    val speedAccuracy: Float? = null,     // API 26+
    val bearingAccuracy: Float? = null,   // API 26+
    val provider: String = "",
    val latHemisphere: Char = 0.toChar(),
    val longHemisphere: Char = 0.toChar(),
    val elapsedRealtimeNanos: Long = 0L,
)

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
    val hdop: Double = 0.0,
    val altitude: Double = 0.0,
    val geoidHeight: Double = 0.0,
    val mslAltitude: Double = 0.0,
    val speedKnots: Double = 0.0,
    val course: Double = 0.0,
    val magneticVariation: Double = 0.0
)

data class SatInfo(
    val prn: Int,
    val elevation: Int?,
    val azimuth: Int?,
    val snr: Int?
)

// Sealed class hierarchy for NMEA messages
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
        val dgpsStationId: String?
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
