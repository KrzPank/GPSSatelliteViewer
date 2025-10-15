package com.example.gpssatelliteviewer.mainscreen.screen.livenmea

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.app.theme.ValueText
import com.example.gpssatelliteviewer.data.NMEAMessage
import com.example.gpssatelliteviewer.data.parser.NMEAParser
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.mapFixQuality
import com.example.gpssatelliteviewer.utils.mapFixType
import com.example.gpssatelliteviewer.utils.mapTalker

@Composable
fun NMEAMessageCard(
    message: NMEAMessage,
    rawMessage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.Companion.padding(12.dp)) {
            Text(
                text = message.messageType,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.Companion.height(6.dp))
            Text("Raw message:", style = MaterialTheme.typography.bodyMedium)
            Text(rawMessage, style = MaterialTheme.typography.bodyMedium, color = ValueText)

            when (message) {
                is NMEAMessage.GGA -> {
                    RenderGGAInfo(message)
                }

                is NMEAMessage.RMC -> {
                    RenderRMCInfo(message)
                }

                is NMEAMessage.GSA -> {
                    RenderGSAInfo(message)
                }

                is NMEAMessage.GSV -> {
                    RenderGSVInfo(message)
                }

                is NMEAMessage.VTG -> {
                    RenderVTGInfo(message)
                }

                is NMEAMessage.Unknown -> {
                    InfoRow("Info", "Vendor defined message")
                    val parsed = NMEAParser.parseVendorMessage(rawMessage)
                    parsed.forEach { part ->
                        if (part != "") InfoRow("Data", part)
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderGGAInfo(gga: NMEAMessage.GGA) {
    InfoRow("Time (UTC)", gga.time)
    InfoRow("Latitude", CoordinateConverter.nmeaCoordinateToDMS(gga.latitude, gga.latDirection))
    InfoRow("Longitude", CoordinateConverter.nmeaCoordinateToDMS(gga.longitude, gga.lonDirection))
    InfoRow("Fix Quality", mapFixQuality(gga.fixQuality))
    InfoRow("Satellites", "${gga.satelliteCount}")
    InfoRow("HDOP", "${gga.horizontalDilution}")
    InfoRow("Altitude", "${gga.altitude} ${gga.altitudeUnits}")
    InfoRow("Geoid Separation", "${gga.geoidSeparation} ${gga.geoidSeparationUnits}")
    InfoRow("Dgps Age", "${gga.dgpsAge}")
    InfoRow("Dgps Station ID", "${gga.dgpsStationId}")
}

@Composable
private fun RenderRMCInfo(rmc: NMEAMessage.RMC) {
    InfoRow("Time (UTC)", rmc.time)
    InfoRow("Status", "${rmc.status}")
    InfoRow("Latitude", CoordinateConverter.nmeaCoordinateToDMS(rmc.latitude, rmc.latDirection))
    InfoRow("Longitude", CoordinateConverter.nmeaCoordinateToDMS(rmc.longitude, rmc.lonDirection))
    InfoRow("Speed (knots)", rmc.speedOverGround.toString())
    InfoRow("Course", rmc.courseOverGround.toString())
    InfoRow("Date", rmc.date)
    InfoRow("Magnetic Variation", rmc.magneticVariation?.toString() ?: "")
    InfoRow("Magnetic Variation Direction", rmc.variationDirection?.toString() ?: "")
}

@Composable
private fun RenderGSAInfo(gsa: NMEAMessage.GSA) {
    InfoRow("Mode", gsa.mode.toString())
    InfoRow("Fix Type", mapFixType(gsa.fixType))
    InfoRow("Satellites IDs", gsa.satelliteIds.joinToString(", "))
    InfoRow("PDOP", gsa.pdop.toString())
    InfoRow("HDOP", gsa.hdop.toString())
    InfoRow("VDOP", gsa.vdop.toString())
    InfoRow("System ID", gsa.systemId?.toString() ?: "")
}

@Composable
private fun RenderGSVInfo(gsv: NMEAMessage.GSV) {
    InfoRow("Constellation", "${gsv.talker}/${mapTalker(gsv.talker)}")
    InfoRow("Msg #${gsv.messageNumber}/${gsv.totalMessages}", "")
    InfoRow("SV in view", gsv.satellitesInView.toString())
    gsv.satellitesInfo.forEach { sat ->
        InfoRow("PRN ${sat.prn}", "Az:${sat.azimuth}° E:${sat.elevation}° SNR:${sat.snr}")
    }
    Spacer(modifier = Modifier.Companion.height(8.dp))
}

@Composable
fun RenderGSVInfo(
    gsvMessages: Map<String, NMEAMessage>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.Companion.padding(12.dp)) {
            Text(
                text = "GSV - Satellites in View",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.Companion.height(6.dp))

            gsvMessages.forEach { (_, message) ->
                if (message is NMEAMessage.GSV) {
                    InfoRow("Constellation", "${message.talker}/${mapTalker(message.talker)}")
                    InfoRow("Message", "${message.messageNumber}/${message.totalMessages}")
                    InfoRow("SV in view", message.satellitesInView.toString())
                    message.satellitesInfo.forEach { sat ->
                        InfoRow("PRN ${sat.prn}", "Az:${sat.azimuth}° E:${sat.elevation}° SNR:${sat.snr}")
                    }
                    Spacer(modifier = Modifier.Companion.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun RenderVTGInfo(vtg: NMEAMessage.VTG) {
    InfoRow("Course True", vtg.courseTrue.toString())
    InfoRow("Course Magnetic", vtg.courseMagnetic?.toString() ?: "N/A")
    InfoRow("Speed Knots", vtg.speedKnots.toString())
    InfoRow("Speed Km/h", vtg.speedKmph.toString())
}