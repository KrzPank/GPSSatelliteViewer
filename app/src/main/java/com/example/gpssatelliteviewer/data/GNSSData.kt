package com.example.gpssatelliteviewer.data

import android.location.GnssCapabilities
import com.example.gpssatelliteviewer.data.GNSSCombinedData.Companion.from

data class GNSSStatusData(
    val constellation: String,
    val prn: Int,
    val cn0DbHz: Float,
    val usedInFix: Boolean,
    val azimuth: Float,
    val elevation: Float
)

data class GNSSHardwareInfo(
    val modelName: String? = null,
    val hardwareYear: Int? = null,
    val capabilities: GnssCapabilities? = null
)

data class AzElHistory(
    val firstAz: Float,
    val firstEl: Float,
    val lastAz: Float,
    val lastEl: Float
)

data class GNSSMeasurementData(
    val svid: Int,
    val constellation: String,
    val carrierFrequencyRangeHz: Float?, // carrier frequency
    val cn0DbHz: Double?,   // signal strength
    val snrInDb: Double?,   // signal to noise ratio
    val accumulatedDeltaRangeMeters: Double?,   // carrier phase
    val accumulatedDeltaRangeUncertaintyMeters: Double?, // carrier phase uncertainty
    val pseudorangeRateMetersPerSecond: Double?, // Doppler effect
    val pseudorangeRateUncertaintyMetersPerSecond: Double?, // doppler uncertainty
    val timeOffsetNanos: Double?    // clock drift
)

data class GNSSCombinedData(
    val constellation: String,
    val prn: Int,
    val svid: Int?,
    val cn0DbHz: Float,
    val snrInDb: Double?,
    val usedInFix: Boolean,
    val azimuth: Float,
    val elevation: Float,
    val carrierFrequencyRangeHz: Float?,
    val accumulatedDeltaRangeMeters: Double?,
    val accumulatedDeltaRangeUncertaintyMeters: Double?,
    val pseudorangeRateMetersPerSecond: Double?,
    val pseudorangeRateUncertaintyMetersPerSecond: Double?,
    val timeOffsetNanos: Double?
) {
    companion object {
        fun from(status: GNSSStatusData, measurement: GNSSMeasurementData?): GNSSCombinedData {
            return GNSSCombinedData(
                constellation = status.constellation,
                prn = status.prn,
                svid = measurement?.svid,
                cn0DbHz = measurement?.cn0DbHz?.toFloat() ?: status.cn0DbHz,
                snrInDb = measurement?.snrInDb,
                usedInFix = status.usedInFix,
                azimuth = status.azimuth,
                elevation = status.elevation,
                carrierFrequencyRangeHz = measurement?.carrierFrequencyRangeHz,
                accumulatedDeltaRangeMeters = measurement?.accumulatedDeltaRangeMeters,
                accumulatedDeltaRangeUncertaintyMeters = measurement?.accumulatedDeltaRangeUncertaintyMeters,
                pseudorangeRateMetersPerSecond = measurement?.pseudorangeRateMetersPerSecond,
                pseudorangeRateUncertaintyMetersPerSecond = measurement?.pseudorangeRateUncertaintyMetersPerSecond,
                timeOffsetNanos = measurement?.timeOffsetNanos
            )
        }
    }
}

fun mergeLists(
    statusList: List<GNSSStatusData>,
    measurementList: List<GNSSMeasurementData>
): List<GNSSCombinedData> {
    return statusList.map { status ->
        val measurement = measurementList.find {
            it.svid == status.prn && it.constellation == status.constellation
        }
        from(status, measurement)
    }
}
