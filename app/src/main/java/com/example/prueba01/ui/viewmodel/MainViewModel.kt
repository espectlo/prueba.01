package com.example.prueba01.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba01.bluetooth.BluetoothIoTManager
import com.example.prueba01.bluetooth.ConnectionState
import com.example.prueba01.data.db.AppDatabaseHelper
import com.example.prueba01.data.model.DoorState
import com.example.prueba01.data.model.EmergencyLog
import com.example.prueba01.data.model.EventType
import com.example.prueba01.data.model.Room
import com.example.prueba01.data.repository.OfflineStorageRepository
import com.example.prueba01.notification.EmergencyNotificationManager
import com.example.prueba01.security.SecurityIso27000Manager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiState(
    val rooms: List<Room> = listOf(
        Room(id = "1", name = "Sala 101 - Laboratorio IoT", gasValue = 450, previousGasValue = 450, gasThreshold = 1800),
        Room(id = "2", name = "Sala 102 - Informática", gasValue = 320, previousGasValue = 320, gasThreshold = 1800),
        Room(id = "3", name = "Sala 103 - Servidores", gasValue = 680, previousGasValue = 680, gasThreshold = 1200),
        Room(id = "4", name = "Sala 104 - Electrónica", gasValue = 290, previousGasValue = 290, gasThreshold = 1800),
        Room(id = "5", name = "Sala 105 - Taller Robótica", gasValue = 510, previousGasValue = 510, gasThreshold = 1800)
    ),
    val logs: List<EmergencyLog> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.SIMULATED,
    val selectedTab: Int = 0,
    val isSimulationActive: Boolean = true,
    val selectedRoomIdForSim: String = "1",
    val globalGasThreshold: Int = 1800,
    val pendingOfflineCount: Int = 0,
    val isLoggedIn: Boolean = false,
    val currentUser: String? = null
)

private data class AlarmEvaluationResult(
    val room: Room,
    val newAlarmTriggered: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothIoTManager(application)
    private val offlineRepository = OfflineStorageRepository(application)
    private val dbHelper = AppDatabaseHelper(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(listOf("[SISTEMA] Cifrado ISO 27000 (AES-256-GCM / HMAC-SHA256) Habilitado."))
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private var simulationJob: Job? = null
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        EmergencyNotificationManager.createNotificationChannel(application)
        loadPersistedLogsFromSQLite()
        refreshPendingOfflineCount()
        observeBluetooth()
        startSimulationLoop()
    }

    private fun loadPersistedLogsFromSQLite() {
        viewModelScope.launch {
            val savedLogs = dbHelper.getAllEmergencyLogs()
            _uiState.update { it.copy(logs = savedLogs) }
            addTerminalLog("[BD SQLite] Cargados ${savedLogs.size} eventos guardados permanentemente.")
        }
    }

    // --- AUTENTICACIÓN LOGIN CON PBKDF2 + SAL ---

    fun hasAnyUser(): Boolean = dbHelper.hasAnyUser()

    fun login(username: String, plainPassword: String): Boolean {
        val isValid = dbHelper.authenticateUser(username, plainPassword)
        if (isValid) {
            _uiState.update { it.copy(isLoggedIn = true, currentUser = username) }
            addTerminalLog("[AUTENTICACIÓN] Inicio de sesión exitoso: usuario '$username'")
        } else {
            addTerminalLog("!! [AUTENTICACIÓN] Fallo de inicio de sesión: usuario '$username'")
        }
        return isValid
    }

    fun registerUser(username: String, plainPassword: String): Boolean {
        val success = dbHelper.registerUser(username, plainPassword)
        if (success) {
            addTerminalLog("[AUTENTICACIÓN] Registrado nuevo usuario en SQLite con PBKDF2 y Sal: '$username'")
        }
        return success
    }

    fun logout() {
        _uiState.update { it.copy(isLoggedIn = false, currentUser = null) }
        addTerminalLog("[AUTENTICACIÓN] Sesión cerrada.")
    }

    fun updateMasterKey(newMasterKey: String) {
        SecurityIso27000Manager.setMasterKey(newMasterKey)
        addTerminalLog("[SEGURIDAD] Clave maestra de cifrado AES-256-GCM actualizada.")
    }

    private fun observeBluetooth() {
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
                addTerminalLog("[CONEXIÓN] Estado cambiado a: $connState")
                if (connState == ConnectionState.CONNECTED) {
                    syncPendingOfflineLogs()
                }
            }
        }

        viewModelScope.launch {
            bluetoothManager.incomingData.collect { rawData ->
                addTerminalLog("[RX ESP32] $rawData")
                parseEsp32Data(rawData)
            }
        }
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(3500)
                if (_uiState.value.isSimulationActive) {
                    val currentRooms = _uiState.value.rooms
                    var hasChanges = false
                    val triggeredRooms = mutableListOf<Room>()

                    val newRoomList = currentRooms.map { room ->
                        if (room.id != _uiState.value.selectedRoomIdForSim) {
                            val variation = (-25..25).random()
                            val newGas = (room.gasValue + variation).coerceIn(100, 4095)
                            if (kotlin.math.abs(newGas - room.gasValue) > 5) {
                                hasChanges = true
                                val eval = checkRoomAlarmState(room.copy(previousGasValue = room.gasValue, gasValue = newGas))
                                if (eval.newAlarmTriggered) triggeredRooms.add(eval.room)
                                eval.room
                            } else room
                        } else {
                            val eval = checkRoomAlarmState(room)
                            if (eval.room != room) {
                                hasChanges = true
                                if (eval.newAlarmTriggered) triggeredRooms.add(eval.room)
                                eval.room
                            } else room
                        }
                    }

                    if (hasChanges) {
                        _uiState.update { it.copy(rooms = newRoomList) }
                        triggeredRooms.forEach { room ->
                            handleAlarmTriggered(room, "¡Detección de Humo/Gas en ${room.name}!")
                        }
                    }
                }
            }
        }
    }

    private fun checkRoomAlarmState(room: Room): AlarmEvaluationResult {
        val isOverThreshold = room.gasValue >= room.gasThreshold || room.lm393DigitalActive
        val wasAlarmActive = room.isAlarmActive

        return if (isOverThreshold && !wasAlarmActive) {
            AlarmEvaluationResult(
                room = room.copy(
                    doorState = DoorState.UNLOCKED,
                    isAlarmActive = true,
                    lastUpdatedTime = getCurrentTimeShort()
                ),
                newAlarmTriggered = true
            )
        } else {
            AlarmEvaluationResult(
                room = room,
                newAlarmTriggered = false
            )
        }
    }

    private fun handleAlarmTriggered(room: Room, customTitle: String) {
        val message = "Nivel gas MQ-2: ${room.gasValue} >= ${room.gasThreshold}. Puerta desbloqueada automáticamente."
        logEvent(
            roomId = room.id,
            roomName = room.name,
            eventType = EventType.GAS_ALARM_AUTOMATIC,
            gasLevel = room.gasValue,
            message = message
        )
        addTerminalLog("!! [ALERTA ESP32] ${room.name} superó umbral (${room.gasValue}). Activando servos a 120°.")

        EmergencyNotificationManager.sendEmergencyNotification(
            context = getApplication(),
            title = customTitle,
            message = "$message Puerta en ${room.name} abierta para evacuación."
        )
    }

    fun syncPendingOfflineLogs() {
        viewModelScope.launch {
            val pendingLogs = offlineRepository.getPendingOfflineLogs()
            if (pendingLogs.isNotEmpty()) {
                val count = pendingLogs.size
                pendingLogs.forEach { log -> dbHelper.insertEmergencyLog(log) }

                _uiState.update { state ->
                    val allSaved = dbHelper.getAllEmergencyLogs()
                    state.copy(logs = allSaved, pendingOfflineCount = 0)
                }
                offlineRepository.clearPendingLogs()
                addTerminalLog("[REGISTRO LOCAL] Registrados $count eventos locales acumulados en la base de datos.")
                bluetoothManager.sendCommand("SYNC_NOTIFY_COUNT=$count")
            } else {
                refreshPendingOfflineCount()
            }
        }
    }

    private fun refreshPendingOfflineCount() {
        val count = offlineRepository.getPendingOfflineLogs().size
        _uiState.update { it.copy(pendingOfflineCount = count) }
    }

    fun setSimulatedGasValue(roomId: String, newGasValue: Int) {
        var triggeredRoom: Room? = null

        _uiState.update { state ->
            val updatedRooms = state.rooms.map { room ->
                if (room.id == roomId) {
                    val gasUpdated = room.copy(previousGasValue = room.gasValue, gasValue = newGasValue)
                    val eval = checkRoomAlarmState(gasUpdated)
                    if (eval.newAlarmTriggered) triggeredRoom = eval.room
                    eval.room
                } else room
            }
            state.copy(rooms = updatedRooms, selectedRoomIdForSim = roomId)
        }

        triggeredRoom?.let { room ->
            handleAlarmTriggered(room, "🚨 ¡ALERTA CRÍTICA DE HUMO/GAS!")
        }
    }

    fun setDigitalLm393Trigger(roomId: String, active: Boolean) {
        var triggeredRoom: Room? = null

        _uiState.update { state ->
            val updatedRooms = state.rooms.map { room ->
                if (room.id == roomId) {
                    val updated = room.copy(lm393DigitalActive = active)
                    val eval = checkRoomAlarmState(updated)
                    if (eval.newAlarmTriggered) triggeredRoom = eval.room
                    eval.room
                } else room
            }
            state.copy(rooms = updatedRooms)
        }

        triggeredRoom?.let { room ->
            handleAlarmTriggered(room, "🚨 ¡DISPARO DIGITAL LM393!")
        }
    }

    fun toggleMuteAlarm(roomId: String) {
        _uiState.update { state ->
            val updatedRooms = state.rooms.map { room ->
                if (room.id == roomId) {
                    val newMute = !room.isMuted
                    addTerminalLog("[CMD] Alarma sonora en ${room.name} ${if (newMute) "SILENCIADA" else "ACTIVADA"}")
                    bluetoothManager.sendCommand("MUTE_${room.id}_${if (newMute) 1 else 0}")
                    room.copy(isMuted = newMute)
                } else room
            }
            state.copy(rooms = updatedRooms)
        }
    }

    fun unlockDoorManually(roomId: String) {
        var unlockedRoom: Room? = null

        _uiState.update { state ->
            val updatedRooms = state.rooms.map { room ->
                if (room.id == roomId) {
                    val updated = room.copy(doorState = DoorState.UNLOCKED, isAlarmActive = true)
                    unlockedRoom = updated
                    updated
                } else room
            }
            state.copy(rooms = updatedRooms)
        }

        unlockedRoom?.let { room ->
            logEvent(
                roomId = room.id,
                roomName = room.name,
                eventType = EventType.MANUAL_DOOR_UNLOCK,
                gasLevel = room.gasValue,
                message = "Apertura manual forzada de emergencia ejecutada desde la App Móvil."
            )
            addTerminalLog("[CMD] Desbloqueo manual forzado en ${room.name}")
            bluetoothManager.sendCommand("UNLOCK_${room.id}")

            EmergencyNotificationManager.sendEmergencyNotification(
                context = getApplication(),
                title = "🔓 Puerta Desbloqueada Manualmente",
                message = "Se ejecutó apertura de emergencia en ${room.name} desde la aplicación."
            )
        }
    }

    fun unlockAllDoors() {
        var roomsToLog: List<Room> = emptyList()

        _uiState.update { state ->
            val updatedRooms = state.rooms.map { room ->
                room.copy(doorState = DoorState.UNLOCKED, isAlarmActive = true)
            }
            roomsToLog = updatedRooms
            state.copy(rooms = updatedRooms)
        }

        roomsToLog.forEach { room ->
            logEvent(
                roomId = room.id,
                roomName = room.name,
                eventType = EventType.MANUAL_DOOR_UNLOCK,
                gasLevel = room.gasValue,
                message = "Evacuación Global: Desbloqueo manual masivo de todas las salas."
            )
        }
        addTerminalLog("[CMD GLOBAL] ¡EVACUACIÓN! Desbloqueo enviado a todas las puertas.")
        bluetoothManager.sendCommand("UNLOCK_ALL")

        EmergencyNotificationManager.sendEmergencyNotification(
            context = getApplication(),
            title = "⚠️ ¡EVACUACIÓN GENERAL ACTIVADA!",
            message = "Todas las puertas de las salas han sido desbloqueadas automáticamente."
        )
    }

    fun resetAlarm(roomId: String) {
        var resetRoom: Room? = null

        _uiState.update { state ->
            val updatedRooms = state.rooms.map { room ->
                if (room.id == roomId) {
                    val updated = room.copy(
                        doorState = DoorState.LOCKED,
                        isAlarmActive = false,
                        isMuted = false,
                        gasValue = (room.gasValue).coerceAtMost(room.gasThreshold - 100)
                    )
                    resetRoom = updated
                    updated
                } else room
            }
            state.copy(rooms = updatedRooms)
        }

        resetRoom?.let { room ->
            logEvent(
                roomId = room.id,
                roomName = room.name,
                eventType = EventType.ALARM_RESET,
                gasLevel = room.gasValue,
                message = "Alarma y servos restablecidos a estado normal (Puerta bloqueada)."
            )
            addTerminalLog("[CMD] Restableciendo alarma y cerradura en ${room.name}")
            bluetoothManager.sendCommand("RESET_${room.id}")
        }
    }

    fun setGlobalThreshold(newThreshold: Int) {
        _uiState.update { state ->
            val updatedRooms = state.rooms.map { it.copy(gasThreshold = newThreshold) }
            state.copy(rooms = updatedRooms, globalGasThreshold = newThreshold)
        }
        addTerminalLog("[CONFIG] Umbral de gas actualizado a: $newThreshold")
        bluetoothManager.sendCommand("SET_THRESHOLD=$newThreshold")
    }

    fun setSelectedTab(tabIndex: Int) {
        if (_uiState.value.selectedTab != tabIndex) {
            _uiState.update { it.copy(selectedTab = tabIndex) }
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        _uiState.update { it.copy(isSimulationActive = enabled) }
        if (enabled) {
            bluetoothManager.enableSimulationMode()
            addTerminalLog("[MODO] Simulación local activada.")
        }
    }

    fun connectToBluetoothDevice(address: String) {
        bluetoothManager.connectToDevice(address)
    }

    fun refreshBluetoothDevices() {
        bluetoothManager.refreshPairedDevices()
    }

    fun getPairedBluetoothDevices() = bluetoothManager.pairedDevices

    private fun parseEsp32Data(rawData: String) {
        try {
            val decryptedResult = SecurityIso27000Manager.decryptFrame(rawData)
            if (!decryptedResult.isValid) {
                addTerminalLog("!! [SEGURIDAD ISO 27000] Error de Integridad: ${decryptedResult.errorMessage}. Trama descartada.")
                return
            }

            val payload = decryptedResult.payload
            if (payload.contains("GAS:")) {
                val parts = payload.split(",")
                var gas = 0
                parts.forEach { part ->
                    val kv = part.trim().split(":")
                    if (kv.size == 2 && kv[0].uppercase() == "GAS") {
                        gas = kv[1].toIntOrNull() ?: 0
                    }
                }
                setSimulatedGasValue("1", gas)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun logEvent(roomId: String, roomName: String, eventType: EventType, gasLevel: Int, message: String) {
        val newLog = EmergencyLog(
            roomId = roomId,
            roomName = roomName,
            eventType = eventType,
            gasLevel = gasLevel,
            message = message
        )

        dbHelper.insertEmergencyLog(newLog)

        if (_uiState.value.connectionState != ConnectionState.CONNECTED) {
            offlineRepository.savePendingOfflineLog(newLog)
            refreshPendingOfflineCount()
        }

        val allLogs = dbHelper.getAllEmergencyLogs()
        _uiState.update { it.copy(logs = allLogs) }
    }

    private fun addTerminalLog(msg: String) {
        val timestamp = getCurrentTimeShort()
        _terminalLogs.update { current ->
            listOf("[$timestamp] $msg") + current.take(15)
        }
    }

    private fun getCurrentTimeShort(): String {
        return timeFormatter.format(Date())
    }
}
