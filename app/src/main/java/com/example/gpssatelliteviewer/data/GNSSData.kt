package com.example.gpssatelliteviewer.data


data class GNSSData(
    val constellation: String,
    val id: Int,
    val snr: Float,
    val usedInFix: Boolean
)

data class ListenerData(
    val time: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val latHemisphere: String,
    val longHemisphere: String,
)

// ** OLD DATA TYPE **
/*
data class NMEAData(
    val time: String,
    val latitude: String,
    val latHemisphere: String,
    val longitude: String,
    val longHemisphere: String,
    val fixQuality: String,
    val numSatellites: Int,
    val horizontalDilution: Float,
    val altitude: Double,
    val heightOfGeoid: Double
)
*/


data class NMEAData(
    var time: String? = null,
    var date: String? = null,
    var latitude: String? = null,
    //var latHemisphere: String? = null,
    var longitude: String? = null,
    //var lonHemisphere: String? = null,
    var fixQuality: String? = null,
    var numSatellites: Int? = null,
    var hdop: Float? = null,
    var altitude: Double? = null,
    var geoidHeight: Double? = null,
    var speedKnots: Double? = null,
    var course: Double? = null,
    var magneticVariation: Double? = null,
    var gbsErrors: List<Double>? = null
)