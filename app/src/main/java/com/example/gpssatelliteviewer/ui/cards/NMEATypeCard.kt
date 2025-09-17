package com.example.gpssatelliteviewer.ui.cards

import androidx.compose.runtime.Composable
import com.example.gpssatelliteviewer.data.GGA
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.gpssatelliteviewer.data.GSA
import com.example.gpssatelliteviewer.data.GSV
import com.example.gpssatelliteviewer.data.RMC
import com.example.gpssatelliteviewer.data.VTG
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.data.parsers.NMEAParser
import com.example.gpssatelliteviewer.utils.CoordinateConversion
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.mapFixQuality
import com.example.gpssatelliteviewer.utils.mapFixType
import com.example.gpssatelliteviewer.utils.mapTalker

@Composable
fun DefaultNMEATypeCard(
    type: String,
    gga: GGA?,
    rmc: RMC?,
    gsa: GSA?,
    vtg: VTG?,
    gsv: Map<String, GSV>,
    rawMessage: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = type,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            rawMessage?.let {
                InfoRow("Raw message", it)
            }

            when {
                type.contains("GGA", ignoreCase = true) && gga != null -> {
                    InfoRow("Time (UTC)", gga.time)
                    InfoRow(
                        "Latitude",
                        CoordinateConversion.geodeticToDMS(gga.latitude, gga.latDirection)
                    )
                    InfoRow(
                        "Longitude",
                        CoordinateConversion.geodeticToDMS(gga.longitude, gga.lonDirection)
                    )
                    InfoRow("Fix Quality", mapFixQuality(gga.fixQuality))
                    InfoRow("Satellites", gga.numSatellites.toString())
                    InfoRow("HDOP", gga.horizontalDilution.toString())
                    InfoRow("Altitude", "${gga.altitude} ${gga.altitudeUnits}")
                    InfoRow(
                        "Geoid Separation",
                        "${gga.geoidSeparation} ${gga.geoidSeparationUnits}"
                    )
                }

                type.contains("RMC", ignoreCase = true) && rmc != null -> {
                    InfoRow("Time (UTC)", rmc.time)
                    InfoRow("Date", rmc.date)
                    InfoRow(
                        "Latitude",
                        CoordinateConversion.geodeticToDMS(rmc.latitude, rmc.latDirection)
                    )
                    InfoRow(
                        "Longitude",
                        CoordinateConversion.geodeticToDMS(rmc.longitude, rmc.lonDirection)
                    )
                    InfoRow("Speed (knots)", rmc.speedOverGround.toString())
                    InfoRow("Course", rmc.courseOverGround.toString())
                    InfoRow(
                        "Magnetic Variation",
                        rmc.magneticVariation?.toString() ?: "-"
                    )
                }

                type.contains("GSA", ignoreCase = true) && gsa != null -> {
                    InfoRow("Mode", gsa.mode.toString())
                    InfoRow("Fix Type", mapFixType(gsa.fixType))
                    InfoRow("PDOP", gsa.pdop.toString())
                    InfoRow("HDOP", gsa.hdop.toString())
                    InfoRow("VDOP", gsa.vdop.toString())
                    InfoRow("Satellites IDs", gsa.satelliteIds.joinToString(", "))
                }

                type.contains("VTG", ignoreCase = true) && vtg != null -> {
                    InfoRow("Course True", vtg.courseTrue.toString())
                    InfoRow(
                        "Course Magnetic",
                        vtg.courseMagnetic?.toString() ?: "N/A"
                    )
                    InfoRow("Speed Knots", vtg.speedKnots.toString())
                    InfoRow("Speed Km/h", vtg.speedKmph.toString())
                }
                type.contains("GSV", ignoreCase = true) -> {
                    if (gsv.isEmpty()) InfoRow("Info", "No satellites")
                    else gsv.forEach { (system, msg) ->
                        InfoRow("Constellation", "${system}/${mapTalker(msg.talker)}")
                        InfoRow("Msg #${msg.messageNumber}/${msg.totalMessages}", "")
                        InfoRow("SV in view", msg.satellitesInView.toString())
                        msg.satellitesInfo.forEach { sat ->
                            InfoRow("PRN ${sat.prn}", "E:${sat.elevation}° Az:${sat.azimuth}° SNR:${sat.snr}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                else -> {
                    InfoRow("Info", "Vendor defined message")
                }
            }
        }
    }
}
