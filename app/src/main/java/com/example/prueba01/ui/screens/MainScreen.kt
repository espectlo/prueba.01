package com.example.prueba01.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba01.bluetooth.ConnectionState
import com.example.prueba01.ui.components.EmergencyBanner
import com.example.prueba01.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val activeAlarmRooms = remember(uiState.rooms) {
        uiState.rooms.filter { it.isAlarmActive || it.gasValue >= it.gasThreshold }
    }

    val pairedDevices by viewModel.getPairedBluetoothDevices().collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SafeDoor IoT - Monitoreo de Puertas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (uiState.connectionState == ConnectionState.CONNECTED) "Conectado a ESP32" else "Modo Simulación Activo",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (activeAlarmRooms.isNotEmpty()) Color(0xFFD32F2F) else Color(0xFF1976D2)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(imageVector = Icons.Default.DoorFront, contentDescription = "Monitoreo") },
                    label = { Text("Monitoreo") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF1976D2))
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "Simulador") },
                    label = { Text("Simulador") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF1976D2))
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Historial") },
                    label = { Text("Historial") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF1976D2))
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(imageVector = Icons.Default.Bluetooth, contentDescription = "Conexión") },
                    label = { Text("Conexión") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF1976D2))
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EmergencyBanner(
                activeAlarmRooms = activeAlarmRooms,
                onUnlockAll = { viewModel.unlockAllDoors() }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    0 -> MonitoringScreen(
                        rooms = uiState.rooms,
                        onUnlockManual = { roomId -> viewModel.unlockDoorManually(roomId) },
                        onResetAlarm = { roomId -> viewModel.resetAlarm(roomId) },
                        onToggleMute = { roomId -> viewModel.toggleMuteAlarm(roomId) }
                    )

                    1 -> ControlSimulatorScreen(
                        rooms = uiState.rooms,
                        selectedRoomId = uiState.selectedRoomIdForSim,
                        globalThreshold = uiState.globalGasThreshold,
                        onSetGasValue = { roomId, gas -> viewModel.setSimulatedGasValue(roomId, gas) },
                        onSetDigitalLm393 = { roomId, active -> viewModel.setDigitalLm393Trigger(roomId, active) },
                        onSetThreshold = { threshold -> viewModel.setGlobalThreshold(threshold) },
                        onUnlockAll = { viewModel.unlockAllDoors() },
                        onResetAll = { uiState.rooms.forEach { viewModel.resetAlarm(it.id) } }
                    )

                    2 -> HistoryScreen(logs = uiState.logs)

                    3 -> ConnectionScreen(
                        connectionState = uiState.connectionState,
                        isSimulationActive = uiState.isSimulationActive,
                        pairedDevices = pairedDevices,
                        terminalLogs = terminalLogs,
                        onToggleSimulation = { viewModel.toggleSimulationMode(it) },
                        onConnectDevice = { viewModel.connectToBluetoothDevice(it) },
                        onRefreshDevices = { viewModel.refreshBluetoothDevices() }
                    )
                }
            }
        }
    }
}
