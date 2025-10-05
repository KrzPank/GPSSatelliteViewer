package com.example.gpssatelliteviewer.ui.component.card

import android.location.GnssCapabilities
import android.os.Build
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
import com.example.gpssatelliteviewer.data.GnssHardwareInfo
import com.example.gpssatelliteviewer.utils.CapabilityRow
import com.example.gpssatelliteviewer.utils.InfoRow


@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun GNSSChipsetInfoCard(
    info: GnssHardwareInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Hardware Info",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))

            InfoRow("Chipset", prettifyGNSSModelName(info.modelName))
            InfoRow("Hardware Year", info.hardwareYear?.toString() ?: "Unknown")
        }
    }
}

@Composable
fun GNSSChipsetCapabilitiesCard(
    info: GnssHardwareInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (info.capabilities != null) {
                Text(
                    "Capabilities",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(8.dp))

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
                    Spacer(Modifier.height(4.dp))
                    capabilitiesMap.forEach { (label, supported) ->
                        CapabilityRow(label, supported)
                    }

                    // GNSS signal types
                    val signals = caps.gnssSignalTypes
                    if (signals.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Supported Signal Types:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        signals.forEach { sig ->
                            Text(
                                text = "• ${sig.constellationType} (${sig.carrierFrequencyHz / 1_000_000} MHz)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
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

                    Spacer(Modifier.height(4.dp))
                    capabilitiesMap.forEach { (label, supported) ->
                        CapabilityRow(label, supported)
                    }

                    // Below Android 12
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "GNSS capability details not available on this Android version.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text("No specific capabilities detected", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun prettifyGNSSModelName(
    rawName: String?,
): String {
    val hardware = Build.HARDWARE

    // Make it more human-readable
    val clean = rawName
        ?.split(",")
        ?.firstOrNull()
        ?.replace("_default", "", ignoreCase = true)
        ?.replace("_ver", "", ignoreCase = true)
        ?.replace("ver_", "", ignoreCase = true)
        ?.replace("fw_", "", ignoreCase = true)
        ?.replace("_", " ")
        ?.trim()
        ?: return "Unknown Chipset"

    return when {
        clean.contains("MTK", true) -> "MediaTek $hardware ($clean)"
        clean.contains("Qualcomm", true) || clean.contains("SDM", true) -> "Qualcomm $hardware ($clean)"
        clean.contains("Broadcom", true) || clean.contains("BCM", true) -> "Broadcom $hardware ($clean)"
        clean.contains("u-blox", true) -> "u-blox GNSS $hardware ($clean)"
        else -> clean
    }
}