package com.example.gpssatelliteviewer.data

import java.sql.Time

data class GNSSData(
    val constellation: String,
    val id: Int,
    val snr: Float,
    val usedInFix: Boolean
)

data class NMEAData(
    val time: String,
    val latitude: Double,
    val longitude: Double,
    val fixQuality: Int,
    val numSatellites: Int,
    val horizontalDilution: Float,
    val altitude: Double,
    val heightOfGeoid: Double
)
