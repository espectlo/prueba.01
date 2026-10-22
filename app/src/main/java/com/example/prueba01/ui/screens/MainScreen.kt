package com.example.prueba01.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.prueba01.bluetooth.ConnectionState
import com.example.prueba01.security.BiometricPromptHelper
import com.example.prueba01.ui.components.EmergencyBanner
import com.example.prueba01.ui.components.SecurityAuthDialog
import com.example.prueba01.ui.viewmodel.MainViewModel

data class PendingAuth(
    val title: String,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Si el usuario no se ha autenticado mediante Login en SQLite, muestra la pantalla de Inicio de Sesión
    if (!uiState.isLoggedIn) {
        LoginScreen(
            hasAnyUser = viewModel.hasAnyUser(),
            onLoginSuccess = { _ -> },
            onAuthenticateWithHash = { username, password -> viewModel.login(username, password) },
            onRegisterUser = { username, password -> viewModel.registerUser(username, password) }
        )
        return
    }

    var pendingAuth by remember { mutableStateOf<PendingAuth?>(null) }

    val activeAlarmRooms = remember(uiState.rooms) {
        uiState.rooms.filter { it.isAlarmActive || it.gasValue >= it.gasThreshold }
    }

    val pairedDevices by viewModel.getPairedBluetoothDevices().collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()

    // Dialog de Autenticación de Seguridad (PIN de 4 dígitos o Biometría)
    pendingAuth?.let { auth ->
        SecurityAuthDialog(
            actionTitle = auth.title,
            onAuthSuccess = {
                auth.action()
                pendingAuth = null
            },
            onDismiss = { pendingAuth = null },
            onTriggerBiometric = {
                (context as? FragmentActivity)?.let { activity ->
                    BiometricPromptHelper.showBiometricPrompt(
                        activity = activity,
                        title = "Autenticación SafeDoor",
                        subtitle = auth.title,
                        onSuccess = {
                            auth.action()
                            pendingAuth = null
                        },
                        onError = { _ -> }
                    )
                }
            }
        )
    }

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
                            text = "Operador: ${uiState.currentUser ?: "Admin"} | ${if (uiState.connectionState == ConnectionState.CONNECTED) "Conectado a ESP32" else "Modo Simulación"}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = Color.White
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
                onUnlockAll = {
                    pendingAuth = PendingAuth("EVACUACIÓN GENERAL - Abrir Todas las Puertas") {
                        viewModel.unlockAllDoors()
                    }
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    0 -> MonitoringScreen(
                        rooms = uiState.rooms,
                        onUnlockManual = { roomId ->
                            val room = uiState.rooms.find { it.id == roomId }
                            pendingAuth = PendingAuth("Apertura de Puerta en ${room?.name ?: "Sala"}") {
                                viewModel.unlockDoorManually(roomId)
                            }
                        },
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
                        onUnlockAll = {
                            pendingAuth = PendingAuth("Apertura General Masiva") {
                                viewModel.unlockAllDoors()
                            }
                        },
                        onResetAll = { uiState.rooms.forEach { viewModel.resetAlarm(it.id) } }
                    )

                    2 -> HistoryScreen(
                        logs = uiState.logs,
                        pendingOfflineCount = uiState.pendingOfflineCount,
                        onSyncNow = { viewModel.syncPendingOfflineLogs() }
                    )

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
