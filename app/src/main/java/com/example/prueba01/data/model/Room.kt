package com.example.prueba01.data.model

import androidx.compose.runtime.Immutable

enum class DoorState {
    LOCKED,      // Puerta Bloqueada (Estado Normal - Servo a 0°)
    UNLOCKED,    // Puerta Desbloqueada (Emergencia/Manual - Servo a 120°)
    ERROR        // Error de sensor/mecanismo
}

enum class GasTrend {
    STABLE,   // Lectura estable
    RISING,   // Subiendo (Peligro potencial)
    FALLING   // Bajando (Disipación de humo)
}

@Immutable
data class Room(
    val id: String,
    val name: String,
    val gasValue: Int = 0,               // Lectura analógica MQ-2 (0 - 4095)
    val previousGasValue: Int = 0,       // Lectura previa para calcular tendencia
    val gasThreshold: Int = 1800,         // Umbral de disparo por sala
    val doorState: DoorState = DoorState.LOCKED,
    val isAlarmActive: Boolean = false,   // Alarma activa (Buzzer + LED + Servos)
    val isMuted: Boolean = false,         // Mute temporal de alarma
    val isConnected: Boolean = true,
    val lm393DigitalActive: Boolean = false,
    val lastUpdatedTime: String = "Ahora"
) {
    val isGasDanger: Boolean get() = gasValue >= gasThreshold
    val trend: GasTrend get() = when {
        gasValue > previousGasValue + 8 -> GasTrend.RISING
        gasValue < previousGasValue - 8 -> GasTrend.FALLING
        else -> GasTrend.STABLE
    }
}
