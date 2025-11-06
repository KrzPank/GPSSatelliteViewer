package com.example.gpssatelliteviewer.scene3d.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.scene3d.Scene3DParameters
import com.example.gpssatelliteviewer.scene3d.Scene3DParametersState
import com.example.gpssatelliteviewer.utils.ParameterSection
import com.example.gpssatelliteviewer.app.theme.TextLabelColor
import com.example.gpssatelliteviewer.app.theme.GreenPrimaryColor
import com.example.gpssatelliteviewer.app.theme.StatusErrorColor
import com.example.gpssatelliteviewer.app.theme.OutlineColor
import kotlin.math.log10
import kotlin.math.pow
import androidx.compose.runtime.*
import com.example.gpssatelliteviewer.app.theme.DarkBackgroundColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Scene3DParametersMenu(
    parametersState: Scene3DParametersState,
    onParametersChanged: (Scene3DParameters) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(DarkBackgroundColor.copy(alpha = 0.95f))
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val params = parametersState.parameters
        // Header
        Text(
            text = "Scene3D Parameters",
            color = TextLabelColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider(thickness = 1.dp, color = OutlineColor)

        Row(
            modifier = Modifier.Companion.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    parametersState.resetToDefaults()
                    onParametersChanged(parametersState.parameters)
                },
                modifier = Modifier.Companion.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StatusErrorColor)
            ) {
                Text("Reset", color = TextLabelColor, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.Companion.height(8.dp))

        ParameterSection("Light Parameters") {
            LogarithmicSliderParameter(
                label = "Light Intensity",
                value = params.lightIntensity,
                minValue = 10_000f,
                maxValue = 100_000_000f,
                onValueChange = {
                    parametersState.updateLightIntensity(it)
                    onParametersChanged(parametersState.parameters)
                },
                valueFormatter = { "${(it / 100_000f).format(1)}e5" }
            )

            ColorParameter(
                label = "Light Color",
                red = params.lightColor.x,
                green = params.lightColor.y,
                blue = params.lightColor.z,
                onColorChanged = { r, g, b ->
                    parametersState.updateLightColor(r, g, b)
                    onParametersChanged(parametersState.parameters)
                }
            )
        }

        ParameterSection("Earth Model") {
            ModelSelector(
                label = "Earth Model",
                options = Scene3DParameters.Companion.EARTH_MODEL_OPTIONS,
                currentPath = params.earthModelPath,
                onModelChanged = {
                    parametersState.updateEarthModel(it)
                    onParametersChanged(parametersState.parameters)
                }
            )
        }


        ParameterSection("Satellites") {
            ModelSelector(
                label = "Satellite Model",
                options = Scene3DParameters.Companion.SATELLITE_MODEL_OPTIONS,
                currentPath = params.satelliteModelPath,
                onModelChanged = {
                    parametersState.updateSatelliteModel(it)
                    onParametersChanged(parametersState.parameters)
                }
            )
        }

        ParameterSection("Rendering Quality") {
            QualitySelector(
                label = "HDR Color Buffer",
                current = params.hdrColorBufferQuality,
                onQualitySelected = {
                    parametersState.updateHdrColorBufferQuality(it)
                    onParametersChanged(parametersState.parameters)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Dynamic Resolution", color = TextLabelColor, fontSize = 14.sp)
                Switch(
                    checked = params.dynamicResolutionEnabled,
                    onCheckedChange = {
                        parametersState.updateDynamicResolutionEnabled(it)
                        onParametersChanged(parametersState.parameters)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimaryColor)
                )
            }

            if (params.dynamicResolutionEnabled) {
                QualitySelector(
                    label = "Dynamic Resolution Quality",
                    current = params.dynamicResolutionQuality,
                    onQualitySelected = {
                        parametersState.updateDynamicResolutionQuality(it)
                        onParametersChanged(parametersState.parameters)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "MSAA (Multi-sample AA)", color = TextLabelColor, fontSize = 14.sp)
                Switch(
                    checked = params.msaaEnabled,
                    onCheckedChange = {
                        parametersState.updateMsaaEnabled(it)
                        onParametersChanged(parametersState.parameters)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimaryColor)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "FXAA (Fast AA)", color = TextLabelColor, fontSize = 14.sp)
                Switch(
                    checked = params.fxaaEnabled,
                    onCheckedChange = {
                        parametersState.updateFxaaEnabled(it)
                        onParametersChanged(parametersState.parameters)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimaryColor)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Temporal Anti-Aliasing (TAA)", color = TextLabelColor, fontSize = 14.sp)
                Switch(
                    checked = params.temporalAntiAliasingEnabled,
                    onCheckedChange = {
                        parametersState.updateTemporalAntiAliasingEnabled(it)
                        onParametersChanged(parametersState.parameters)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimaryColor)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Ambient Occlusion", color = TextLabelColor, fontSize = 14.sp)
                Switch(
                    checked = params.ambientOcclusionEnabled,
                    onCheckedChange = {
                        parametersState.updateAmbientOcclusionEnabled(it)
                        onParametersChanged(parametersState.parameters)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimaryColor)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Bloom", color = TextLabelColor, fontSize = 14.sp)
                Switch(
                    checked = params.bloomEnabled,
                    onCheckedChange = {
                        parametersState.updateBloomEnabled(it)
                        onParametersChanged(parametersState.parameters)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimaryColor)
                )
            }
        }
    }
}

@Composable
private fun SliderParameter(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.Companion.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextLabelColor, fontSize = 14.sp)
            Text(text = valueFormatter(value), color = TextLabelColor, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = TextLabelColor,
                activeTrackColor = GreenPrimaryColor,
                inactiveTrackColor = OutlineColor
            )
        )
    }
}

@Composable
private fun ColorParameter(
    label: String,
    red: Float,
    green: Float,
    blue: Float,
    onColorChanged: (Float, Float, Float) -> Unit
) {
    Column {
        Text(text = label, color = TextLabelColor, fontSize = 14.sp)

        // Red component
        SliderParameter(
            label = "Red",
            value = red,
            valueRange = 0f..1f,
            onValueChange = { onColorChanged(it, green, blue) },
            valueFormatter = { "${(it * 255).toInt()}" }
        )

        // Green component
        SliderParameter(
            label = "Green",
            value = green,
            valueRange = 0f..1f,
            onValueChange = { onColorChanged(red, it, blue) },
            valueFormatter = { "${(it * 255).toInt()}" }
        )

        // Blue component
        SliderParameter(
            label = "Blue",
            value = blue,
            valueRange = 0f..1f,
            onValueChange = { onColorChanged(red, green, it) },
            valueFormatter = { "${(it * 255).toInt()}" }
        )

        // Color preview
        Box(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .height(30.dp)
                .background(Color(red, green, blue), RoundedCornerShape(4.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    label: String,
    options: List<Pair<String, String>>,
    currentPath: String,
    onModelChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentDisplayName = options.find { it.first == currentPath }?.second ?: "Unknown"

    Column {
        Text(text = label, color = TextLabelColor, fontSize = 14.sp)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentDisplayName,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLabelColor,
                    unfocusedTextColor = TextLabelColor,
                    focusedBorderColor = GreenPrimaryColor,
                    unfocusedBorderColor = OutlineColor
                ),
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (path, displayName) ->
                    DropdownMenuItem(
                        text = { Text(displayName) },
                        onClick = {
                            onModelChanged(path)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LogarithmicSliderParameter(
    label: String,
    value: Float,
    minValue: Float = 10_000f,
    maxValue: Float = 100_000_000f,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String
) {
    // Convert actual value to slider position (0.0 to 1.0)
    val logMin = log10(minValue)
    val logMax = log10(maxValue)
    val logValue = log10(value.coerceIn(minValue, maxValue))
    val sliderPosition = ((logValue - logMin) / (logMax - logMin)).coerceIn(0f, 1f)

    Column {
        Row(
            modifier = Modifier.Companion.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextLabelColor, fontSize = 14.sp)
            Text(text = valueFormatter(value), color = TextLabelColor, fontSize = 12.sp)
        }
        Slider(
            value = sliderPosition,
            onValueChange = { position ->
                // Convert slider position back to actual logarithmic value
                val logValue = logMin + position * (logMax - logMin)
                val actualValue = 10f.pow(logValue)
                onValueChange(actualValue)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = TextLabelColor,
                activeTrackColor = GreenPrimaryColor,
                inactiveTrackColor = OutlineColor
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySelector(
    label: String,
    current: Scene3DParameters.QualityLevel,
    onQualitySelected: (Scene3DParameters.QualityLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentDisplayName = current.displayName

    Column {
        Text(text = label, color = TextLabelColor, fontSize = 14.sp)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentDisplayName,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLabelColor,
                    unfocusedTextColor = TextLabelColor,
                    focusedBorderColor = GreenPrimaryColor,
                    unfocusedBorderColor = OutlineColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Scene3DParameters.QualityLevel.entries.forEach { q ->
                    DropdownMenuItem(
                        text = { Text(q.displayName) },
                        onClick = {
                            onQualitySelected(q)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


private fun Float.format(digits: Int) = "%.${digits}f".format(this)
