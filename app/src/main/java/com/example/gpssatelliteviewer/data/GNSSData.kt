package com.example.gpssatelliteviewer.data

data class GNSSStatusData(
    val constellation: String,
    val id: Int,
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
    val latHemisphere: String = "",
    val longitude: Double = 0.0,
    val lonHemisphere: String = "",
    val fixQuality: String = "",
    val numSatellites: Int = 0,
    val hdop: Float = 0f,
    val altitude: Double = 0.0,
    val geoidHeight: Double = 0.0,
    val mslAltitude: Double = 0.0,
    val speedKnots: Double = 0.0,
    val course: Double = 0.0,
    val magneticVariation: Double = 0.0,
    val gbsErrors: List<Double> = emptyList()
)

data class GGA(
    val time: String,            // UTC time hhmmss.ss
    val latitude: Double,
    val latDirection: Char,      // N or S
    val longitude: Double,
    val lonDirection: Char,      // E or W
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
    val mode: Char,              // Auto/Manual
    val fixType: Int,            // 1 = no fix, 2 = 2D fix, 3 = 3D fix
    val satelliteIds: List<Int>,
    val pdop: Double,
    val hdop: Double,
    val vdop: Double
)

data class GSV(
    val totalMessages: Int,
    val messageNumber: Int,
    val satellitesInView: Int,
    val satellitesInfo: List<GNSSStatusData>
)

data class RMC(
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
    val courseTrue: Double,
    val courseMagnetic: Double?,
    val speedKnots: Double,
    val speedKmph: Double
)

data class ACCURACY(
    val horizontalAccuracy: Double?,
    val verticalAccuracy: Double?
)
