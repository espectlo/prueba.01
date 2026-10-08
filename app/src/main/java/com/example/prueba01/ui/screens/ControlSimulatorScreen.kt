package com.example.prueba01.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba01.data.model.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlSimulatorScreen(
    rooms: List<Room>,
    selectedRoomId: String,
    globalThreshold: Int,
    onSetGasValue: (String, Int) -> Unit,
    onSetDigitalLm393: (String, Boolean) -> Unit,
    onSetThreshold: (Int) -> Unit,
    onUnlockAll: () -> Unit,
    onResetAll: () -> Unit
) {
    val selectedRoom = rooms.find { it.id == selectedRoomId } ?: rooms.firstOrNull() ?: return
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Panel de Control y Simulador de Pruebas",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Text(
            text = "Herramientas para simular presencia de humo, probar actuadores y validar apertura de emergencia.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Card 1: Room Selection & Gas Slider Simulation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Seleccionar Sala para Pruebas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedRoom.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        rooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text(room.name) },
                                onClick = {
                                    onSetGasValue(room.id, room.gasValue)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gas Level Slider
                Text(
                    text = "Nivel Analógico MQ-2: ${selectedRoom.gasValue} ADC (0-4095)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (selectedRoom.gasValue >= selectedRoom.gasThreshold) Color(0xFFD32F2F) else Color.DarkGray
                )

                Slider(
                    value = selectedRoom.gasValue.toFloat(),
                    onValueChange = { onSetGasValue(selectedRoom.id, it.toInt()) },
                    valueRange = 0f..4095f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (selectedRoom.gasValue >= selectedRoom.gasThreshold) Color(0xFFD32F2F) else Color(0xFF1976D2),
                        activeTrackColor = if (selectedRoom.gasValue >= selectedRoom.gasThreshold) Color(0xFFD32F2F) else Color(0xFF1976D2)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "0 (Normal)", fontSize = 11.sp, color = Color.Gray)
                    Text(text = "Umbral: ${selectedRoom.gasThreshold}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                    Text(text = "4095 (Max)", fontSize = 11.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Preset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSetGasValue(selectedRoom.id, 2200) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sensors, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(text = "Simular Humo (2200)", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { onSetGasValue(selectedRoom.id, 400) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Aire Limpio (400)", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Switch LM393 Digital Sensor Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Señal Digital LM393", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Disparo por hardware comparador", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = selectedRoom.lm393DigitalActive,
                        onCheckedChange = { onSetDigitalLm393(selectedRoom.id, it) }
                    )
                }
            }
        }

        // Card 2: Global Threshold & Emergency Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Control de Umbral y Evacuación Global",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onUnlockAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(text = "DESBLOQUEO GLOBAL DE TODAS LAS PUERTAS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onResetAll,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(text = "RESTABLECER TODAS LAS ALARMAS Y CERRADURAS", fontSize = 12.sp)
                }
            }
        }
    }
}
