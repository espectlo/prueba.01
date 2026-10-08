package com.example.prueba01.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba01.bluetooth.BluetoothDeviceItem
import com.example.prueba01.bluetooth.ConnectionState

@Composable
fun ConnectionScreen(
    connectionState: ConnectionState,
    isSimulationActive: Boolean,
    pairedDevices: List<BluetoothDeviceItem>,
    terminalLogs: List<String>,
    onToggleSimulation: (Boolean) -> Unit,
    onConnectDevice: (String) -> Unit,
    onRefreshDevices: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Conectividad IoT ESP32 (Bluetooth / WiFi)",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Text(
            text = "Gestión de vinculación con el microcontrolador ESP32 DevKit V1.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Connection Status Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = "Estado de Conexión:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    val (statusText, statusColor) = when (connectionState) {
                        ConnectionState.CONNECTED -> "CONECTADO ESP32" to Color(0xFF2E7D32)
                        ConnectionState.CONNECTING -> "CONECTANDO..." to Color(0xFFE65100)
                        ConnectionState.SIMULATED -> "MODO SIMULADOR" to Color(0xFF1976D2)
                        ConnectionState.DISCONNECTED -> "DESCONECTADO" to Color(0xFFD32F2F)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulation Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Modo Simulación Local", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Permite evaluar la UI sin hardware físico", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isSimulationActive,
                        onCheckedChange = onToggleSimulation
                    )
                }
            }
        }

        // Bluetooth Paired Devices List
        if (!isSimulationActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Dispositivos Bluetooth Emparejados", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedButton(
                            onClick = onRefreshDevices,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualizar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pairedDevices.isEmpty()) {
                        Text(
                            text = "No se encontraron dispositivos Bluetooth emparejados. Por favor vincule el ESP32 en la configuración de Android.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        pairedDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = device.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = device.address, fontSize = 10.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = { onConnectDevice(device.address) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "Conectar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Console / Terminal Output Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Monitor Serial y Consola de Sistema",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Black, shape = RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(terminalLogs) { logLine ->
                            Text(
                                text = logLine,
                                color = when {
                                    logLine.contains("!!") -> Color(0xFFFF5252)
                                    logLine.contains("[CMD]") -> Color(0xFF64B5F6)
                                    logLine.contains("[RX") -> Color(0xFF81C784)
                                    else -> Color(0xFFB0BEC5)
                                },
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
