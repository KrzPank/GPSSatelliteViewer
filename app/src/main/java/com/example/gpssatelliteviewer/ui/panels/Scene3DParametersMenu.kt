package com.example.gpssatelliteviewer.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.data.Scene3DParameters
import com.example.gpssatelliteviewer.data.Scene3DParametersState
import kotlin.math.log10
import kotlin.math.pow

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
            .background(Color(0xDD000000)) // Semi-transparent black background
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Text(
            text = "Scene3D Parameters",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Divider(color = Color.Gray, thickness = 1.dp)
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { 
                    parametersState.resetToDefaults()
                    onParametersChanged(parametersState.parameters)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Reset", color = Color.White, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Light Parameters Section
        ParameterSection("Light Parameters") {
            val params = parametersState.parameters
            
            // Light Type
            LightTypeSelector(
                currentType = params.lightType,
                onTypeChanged = { 
                    parametersState.updateLightType(it)
                    onParametersChanged(parametersState.parameters)
                }
            )
            
            // Light Intensity (Logarithmic)
            LogarithmicSliderParameter(
                label = "Light Intensity",
                value = params.lightIntensity,
                minValue = 10_000f,
                maxValue = 50_000_000f,
                onValueChange = { 
                    parametersState.updateLightIntensity(it)
                    onParametersChanged(parametersState.parameters)
                },
                valueFormatter = { "${(it / 1_000_000f).format(1)}M" }
            )
            
            // Light Color
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
            
            // Light Falloff (for point/spot lights)
            if (params.lightType == Scene3DParameters.LightType.POINT || 
                params.lightType == Scene3DParameters.LightType.SPOT) {
                SliderParameter(
                    label = "Light Falloff",
                    value = params.lightFalloff,
                    valueRange = 10f..5000f,
                    onValueChange = { 
                        parametersState.updateLightFalloff(it)
                        onParametersChanged(parametersState.parameters)
                    },
                    valueFormatter = { "${it.format(0)}m" }
                )
            }
        }
        
        // Earth Model Parameters Section
        ParameterSection("Earth Model") {
            val params = parametersState.parameters
            
            // Earth Model Selection
            ModelSelector(
                label = "Earth Model",
                options = Scene3DParameters.EARTH_MODEL_OPTIONS,
                currentPath = params.earthModelPath,
                onModelChanged = { 
                    parametersState.updateEarthModel(it)
                    onParametersChanged(parametersState.parameters)
                }
            )

        }
        
        // Satellite Parameters Section
        ParameterSection("Satellites") {
            val params = parametersState.parameters
            
            // Satellite Model Selection
            ModelSelector(
                label = "Satellite Model",
                options = Scene3DParameters.SATELLITE_MODEL_OPTIONS,
                currentPath = params.satelliteModelPath,
                onModelChanged = { 
                    parametersState.updateSatelliteModel(it)
                    onParametersChanged(parametersState.parameters)
                }
            )
        }
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
fun SliderParameter(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, fontSize = 14.sp)
            Text(text = valueFormatter(value), color = Color.White, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Green,
                inactiveTrackColor = Color.Gray
            )
        )
    }
}

@Composable
fun ColorParameter(
    label: String,
    red: Float,
    green: Float,
    blue: Float,
    onColorChanged: (Float, Float, Float) -> Unit
) {
    Column {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        
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
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Color(red, green, blue), RoundedCornerShape(4.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightTypeSelector(
    currentType: Scene3DParameters.LightType,
    onTypeChanged: (Scene3DParameters.LightType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(text = "Light Type", color = Color.White, fontSize = 14.sp)
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentType.displayName,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Green,
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Scene3DParameters.LightType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            onTypeChanged(type)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    label: String,
    options: List<Pair<String, String>>,
    currentPath: String,
    onModelChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentDisplayName = options.find { it.first == currentPath }?.second ?: "Unknown"
    
    Column {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        
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
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Green,
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
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
fun CheckboxParameter(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkmarkColor = Color.White,
                uncheckedColor = Color.White,
                checkedColor = Color.Green
            )
        )
    }
}

@Composable
fun LogarithmicSliderParameter(
    label: String,
    value: Float,
    minValue: Float,
    maxValue: Float,
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, fontSize = 14.sp)
            Text(text = valueFormatter(value), color = Color.White, fontSize = 12.sp)
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
                thumbColor = Color.White,
                activeTrackColor = Color.Green,
                inactiveTrackColor = Color.Gray
            )
        )
    }
}

// Extension function for number formatting
fun Float.format(digits: Int) = "%.${digits}f".format(this)
