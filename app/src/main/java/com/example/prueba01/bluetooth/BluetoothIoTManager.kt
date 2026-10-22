package com.example.prueba01.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.prueba01.security.SecurityIso27000Manager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    SIMULATED
}

data class BluetoothDeviceItem(
    val name: String,
    val address: String
)

class BluetoothIoTManager(context: Context) {

    private val _connectionState = MutableStateFlow(ConnectionState.SIMULATED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingData = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingData: SharedFlow<String> = _incomingData.asSharedFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceItem>> = _pairedDevices.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val managerScope = CoroutineScope(Dispatchers.IO + Job())
    private var listenJob: Job? = null

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    init {
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                val devices = adapter.bondedDevices.map {
                    BluetoothDeviceItem(it.name ?: "Dispositivo Desconocido", it.address)
                }
                _pairedDevices.value = devices
            } else {
                _pairedDevices.value = emptyList()
            }
        } catch (e: Throwable) {
            // Previene cierres inesperados en Android 12+ si el permiso BLUETOOTH_CONNECT aún no fue concedido
            _pairedDevices.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        managerScope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) {
                    _connectionState.value = ConnectionState.SIMULATED
                    return@launch
                }

                val device: BluetoothDevice = adapter.getRemoteDevice(address)
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket?.connect()
                inputStream = socket?.inputStream
                outputStream = socket?.outputStream
                _connectionState.value = ConnectionState.CONNECTED

                startListening()
            } catch (e: Throwable) {
                disconnect()
                _connectionState.value = ConnectionState.SIMULATED
            }
        }
    }

    fun enableSimulationMode() {
        disconnect()
        _connectionState.value = ConnectionState.SIMULATED
    }

    private fun startListening() {
        listenJob?.cancel()
        listenJob = managerScope.launch {
            val buffer = ByteArray(512)
            while (_connectionState.value == ConnectionState.CONNECTED) {
                try {
                    val stream = inputStream
                    if (stream != null && stream.available() > 0) {
                        val bytesRead = stream.read(buffer)
                        if (bytesRead > 0) {
                            val message = String(buffer, 0, bytesRead)
                            _incomingData.emit(message)
                        }
                    } else {
                        delay(250)
                    }
                } catch (e: Throwable) {
                    _connectionState.value = ConnectionState.SIMULATED
                    break
                }
            }
        }
    }

    fun sendCommand(command: String) {
        managerScope.launch {
            val encryptedFrame = SecurityIso27000Manager.encryptFrame(command)
            if (_connectionState.value == ConnectionState.CONNECTED) {
                try {
                    outputStream?.write((encryptedFrame + "\n").toByteArray())
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            } else {
                _incomingData.emit("[SIMULADOR CMD CIFRADO] -> $encryptedFrame")
            }
        }
    }

    fun disconnect() {
        listenJob?.cancel()
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            socket = null
            inputStream = null
            outputStream = null
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
}
