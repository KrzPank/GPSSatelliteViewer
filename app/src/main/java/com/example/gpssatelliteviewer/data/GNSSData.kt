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
/*
data class NMEAData(
    val time: String? = null,
    val date: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val fixQuality: String? = null,
    val numSatellites: Int? = null,
    val hdop: Float? = null,
    val altitude: Double? = null,
    val geoidHeight: Double? = null,
    val mslAltitude: Double? = null,
    val speedKnots: Double? = null,
    val course: Double? = null,
    val magneticVariation: Double? = null,
    val gbsErrors: List<Double>? = null
)

 */

//*
data class NMEAData(
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
//*/
