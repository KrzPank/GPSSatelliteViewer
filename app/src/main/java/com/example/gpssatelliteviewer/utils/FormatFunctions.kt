package com.example.gpssatelliteviewer.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.Companion.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Companion.Bold,
            fontSize = 17.sp
        )
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp
        )
        Text(
            text = value,
            color = Color.Green,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ParameterSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            content()
        }
    }
}

@Composable
fun CustomCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkmarkColor = Color.White,
                    uncheckedColor = Color.White,
                    checkedColor = Color.Green
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        // Only show description if provided
        description?.let {
            Text(
                text = it,
                color = Color(0xFFCCCCCC),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 40.dp)
            )
        }
    }
}

fun mapFixQuality(fixQuality: Int): String {
    return when(fixQuality) {
        0 -> "No fix"
        1 -> "GPS"
        2 -> "DGPS"
        3 -> "PPS"
        4 -> "RTK"
        5 -> "Float RTK"
        6 -> "Estimated"
        7 -> "Manual"
        8 -> "Simulated"
        else -> "Unknown"
    }
}
fun mapFixType(fixType: Int): String {
    return when(fixType) {
        1 -> "N/A"
        2 -> "2D Fix"
        3 -> "3D Fix"
        else -> "Unknown"
    }
}

fun mapTalker(talker: String): String {
    return when (talker) {
        "GPGSV" -> "GPS/SBAS"
        "GLGSV" -> "GLONASS"
        "GBGSV" -> "BeiDou"
        "GAGSV" -> "Galileo"
        "GQGSV" -> "QZSS"
        else -> "Unknown"
    }
}
