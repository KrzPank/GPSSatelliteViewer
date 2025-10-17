package com.example.gpssatelliteviewer.mainscreen.screen.chipsetinfo

import android.location.GnssCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.app.theme.ValueText
import com.example.gpssatelliteviewer.data.GNSSHardwareInfo
import com.example.gpssatelliteviewer.utils.CapabilityRow
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.ValueText
import kotlin.math.sign

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun GNSSChipsetInfoCard(
    info: GNSSHardwareInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.Companion.padding(12.dp)) {
            Text(
                "Hardware Info",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 20.sp
            )
            Spacer(Modifier.Companion.height(8.dp))

            // mediatek ???getting different chip names??? sometimes good sometimes sh
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                InfoRow("SOC Manufacturer", Build.SOC_MANUFACTURER)
                InfoRow("SOC Model", Build.SOC_MODEL)
            } else {
                InfoRow("SOC Manufacturer", tryToGetSOCManufacturer(info.modelName))
                InfoRow("SOC Model", Build.HARDWARE.ifBlank { "Unknown" })
            }

            InfoRow("Hardware Year", info.hardwareYear?.toString() ?: "Unknown")

            Spacer(Modifier.Companion.height(8.dp))
            Text(
                "Model name including vendor and hardware/software version",
                style = MaterialTheme.typography.bodyMedium
            )

            val modelName = info.modelName.toString().replace(",", ", ")
            ValueText(
                value = modelName
            )
        }
    }
}

@Composable
fun GNSSChipsetCapabilitiesCard(
    info: GNSSHardwareInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.Companion.padding(12.dp)) {
            if (info.capabilities != null) {
                Text(
                    "Capabilities",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 20.sp
                )
                Spacer(Modifier.Companion.height(8.dp))

                val caps = info.capabilities

                // Android 14+ (UpsideDownCake)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val capabilitiesMap = mapOf(
                        "Supports Scheduling" to caps.hasScheduling(),
                        "Single-Shot Fix" to caps.hasSingleShotFix(),
                        "On-Demand Time Updates" to caps.hasOnDemandTime(),
                        "Geofencing" to caps.hasGeofencing(),
                        "Satellite Blocklist" to caps.hasSatelliteBlocklist(),
                        "Satellite PVT" to caps.hasSatellitePvt(),
                        "Antenna Info" to caps.hasAntennaInfo(),
                        "Measurement Corrections" to caps.hasMeasurementCorrections(),
                        "Measurement Corrections (Driving)" to caps.hasMeasurementCorrectionsForDriving(),
                        "Measurement Corrections (LOS Satellites)" to caps.hasMeasurementCorrectionsLosSats(),
                        "Measurement Corrections (Excess Path Length)" to caps.hasMeasurementCorrectionsExcessPathLength(),
                        "Measurement Corrections (Reflecting Plane)" to caps.hasMeasurementCorrectionsReflectingPlane(),
                        "Measurement Correlation Vectors" to caps.hasMeasurementCorrelationVectors(),
                        "Raw Measurements" to caps.hasMeasurements(),
                        "Navigation Messages" to caps.hasNavigationMessages(),
                        "Power Measurement (Total)" to caps.hasPowerTotal(),
                        "Power Measurement (Single-band Acquisition)" to caps.hasPowerSinglebandAcquisition(),
                        "Power Measurement (Single-band Tracking)" to caps.hasPowerSinglebandTracking(),
                        "Power Measurement (Multi-band Acquisition)" to caps.hasPowerMultibandAcquisition(),
                        "Power Measurement (Multi-band Tracking)" to caps.hasPowerMultibandTracking(),
                        "Power Measurement (Other Modes)" to caps.hasPowerOtherModes(),
                        "Low Power Mode" to caps.hasLowPowerMode(),
                        "Mobile Station Assisted (MSA)" to caps.hasMsa(),
                        "Mobile Station Based (MSB)" to caps.hasMsb(),
                        "Accumulated Delta Range" to (caps.hasAccumulatedDeltaRange() == GnssCapabilities.CAPABILITY_SUPPORTED)
                    )

                    // Display each capability with icon
                    Spacer(Modifier.Companion.height(4.dp))
                    capabilitiesMap.forEach { (label, supported) ->
                        CapabilityRow(label, supported)
                    }

                    // GNSS signal types
                    val signals = caps.gnssSignalTypes
                    //Log.d("SignalTypes", "Signal types: ${signals}")
                    if (signals.isNotEmpty()) {
                        Spacer(Modifier.Companion.height(8.dp))
                        Text(
                            "Supported Signal Types:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        signals.forEach { sig ->
                            Text(
                                text = "• ${sig.constellationType} (${sig.carrierFrequencyHz / 1_000_000} MHz)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.Companion.padding(start = 16.dp, top = 2.dp)
                            )
                        }
                    }

                    // Android 12–13 (S–T)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val capabilitiesMap = mapOf(
                        "Antenna Info (deprecated)" to caps.hasAntennaInfo(),
                        "Raw Measurements" to caps.hasMeasurements(),
                        "Navigation Messages" to caps.hasNavigationMessages()
                    )

                    Spacer(Modifier.Companion.height(4.dp))
                    capabilitiesMap.forEach { (label, supported) ->
                        CapabilityRow(label, supported)
                    }

                    // Below Android 12
                } else {
                    Spacer(Modifier.Companion.height(8.dp))
                    Text(
                        "GNSS capability details not available on this Android version.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "No specific capabilities detected",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun tryToGetSOCManufacturer(
    rawName: String?,
): String {
    if (rawName.isNullOrBlank()) return "Unknown SOC Manufacturer"

    val vendor = when {
        rawName.contains("mediatek", true) || rawName.contains("mtk", true) -> "MediaTek"
        rawName.contains("qualcomm", true) || rawName.contains("sdm", true) || rawName.contains("sm", true) -> "Qualcomm"
        rawName.contains("broadcom", true) || rawName.contains("bcm", true) -> "Broadcom"
        rawName.contains("u-blox", true) || rawName.contains("ublox", true) -> "u-blox"
        rawName.contains("samsung", true) || rawName.contains("exynos", true) -> "Samsung"
        rawName.contains("hisilicon", true) || rawName.contains("kirin", true) -> "HiSilicon"
        else -> "Unknown SOC Manufacturer"
    }

    return vendor
}