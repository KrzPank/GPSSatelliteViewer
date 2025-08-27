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
