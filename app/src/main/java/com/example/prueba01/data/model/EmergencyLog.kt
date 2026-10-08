package com.example.prueba01.data.model

import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EventType {
    GAS_ALARM_AUTOMATIC,
    MANUAL_DOOR_UNLOCK,
    ALARM_RESET,
    DISCONNECTED,
    CONNECTED
}

@Immutable
data class EmergencyLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String = getCurrentTimeString(),
    val roomId: String,
    val roomName: String,
    val eventType: EventType,
    val gasLevel: Int,
    val message: String
)

private fun getCurrentTimeString(): String {
    return SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault()).format(Date())
}
