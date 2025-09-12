package com.example.gpssatelliteviewer.data

data class GNSSStatusData(
    val constellation: String,
    val prn: Int,
    val snr: Float,
    val usedInFix: Boolean,
    val azimuth: Float,
    val elevation: Float
)

/**         *** TO DO ***
data class GNSSNavigationData(

)
*/

data class ListenerData(
    val time: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val latHemisphere: String = "",
    val longHemisphere: String = "",
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

data class GGA(
    val message: String,
    val time: String,
    val latitude: Double,
    val latDirection: Char,
    val longitude: Double,
    val lonDirection: Char,
    val fixQuality: Int,
    val numSatellites: Int,
    val horizontalDilution: Double,
    val altitude: Double,
    val altitudeUnits: Char,
    val geoidSeparation: Double?,
    val geoidSeparationUnits: Char?,
    val dgpsAge: Double?,
    val dgpsStationId: String?
)

data class GSA(
    val message: String,
    val mode: Char,
    val fixType: Int,
    val satelliteIds: List<Int>,
    val pdop: Double,
    val hdop: Double,
    val vdop: Double,
    val systemId: Int?
)

data class SatInfo(
    val prn: Int,
    val elevation: Int?,
    val azimuth: Int?,
    val snr: Int?
)

data class GSV(
    val message: String,
    val totalMessages: Int,
    val messageNumber: Int,
    val satellitesInView: Int,
    val satellitesInfo: List<SatInfo>
)

data class RMC(
    val message: String,
    val time: String,
    val status: Char,            // A=active, V=void
    val latitude: Double,
    val latDirection: Char,
    val longitude: Double,
    val lonDirection: Char,
    val speedOverGround: Double,
    val courseOverGround: Double,
    val date: String,            // ddmmyy
    val magneticVariation: Double?,
    val variationDirection: Char?
)

data class VTG(
    val message: String,
    val courseTrue: Double,
    val courseMagnetic: Double?,
    val speedKnots: Double,
    val speedKmph: Double
)

data class ACCURACY(
    val message: String,
    val horizontalAccuracy: Double?,
    val verticalAccuracy: Double?
)
