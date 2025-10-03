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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.ui.theme.TextLabel
import com.example.gpssatelliteviewer.ui.theme.ValueText
import com.example.gpssatelliteviewer.ui.theme.GreenPrimary
import com.example.gpssatelliteviewer.ui.theme.TextHint

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextLabel,
            fontSize = 15.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ValueText,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
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
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = TextLabel,
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
                    checkmarkColor = TextLabel,
                    uncheckedColor = TextLabel,
                    checkedColor = GreenPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = TextLabel,
                fontSize = 14.sp
            )
        }

        // Only show description if provided
        description?.let {
            Text(
                text = it,
                color = TextHint,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 15.dp)
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

// Extension function for number formatting
fun Float.format(digits: Int) = "%.${digits}f".format(this)
