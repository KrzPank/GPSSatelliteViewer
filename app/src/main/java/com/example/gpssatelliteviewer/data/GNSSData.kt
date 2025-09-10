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
    val time: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val latHemisphere: String,
    val longHemisphere: String,
)
//*
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

 //*/

/*
data class NMEAData(
    val time: String?,
    val date: String?,
    val latitude: String?,
    val longitude: String?,
    val fixQuality: String?,
    val numSatellites: Int?,
    val hdop: Float?,
    val altitude: Double?,
    val geoidHeight: Double?,
    val mslAltitude: Double?,
    val speedKnots: Double?,
    val course: Double?,
    val magneticVariation: Double?,
    val gbsErrors: List<Double>?
)

 */
