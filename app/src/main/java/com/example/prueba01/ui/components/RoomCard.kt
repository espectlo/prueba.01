package com.example.prueba01.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba01.data.model.DoorState
import com.example.prueba01.data.model.GasTrend
import com.example.prueba01.data.model.Room

private val ColorRedDanger = Color(0xFFD32F2F)
private val ColorOrangeWarning = Color(0xFFFFA000)
private val ColorGreenSafe = Color(0xFF2E7D32)
private val ColorGreenBar = Color(0xFF4CAF50)
private val ColorBluePrimary = Color(0xFF1976D2)
private val ColorDangerBackground = Color(0xFFFFEBEE)

@Composable
fun RoomCard(
    room: Room,
    onUnlockManual: () -> Unit,
    onResetAlarm: () -> Unit,
    onToggleMute: () -> Unit = {}
) {
    val isDanger = room.gasValue >= room.gasThreshold || room.isAlarmActive
    val statusColor = when {
        room.isAlarmActive || isDanger -> ColorRedDanger
        room.doorState == DoorState.UNLOCKED -> ColorOrangeWarning
        else -> ColorBluePrimary
    }

    val progress = remember(room.gasValue) {
        (room.gasValue / 4095f).coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = if (isDanger) 2.dp else 1.dp,
                color = if (isDanger) ColorRedDanger else Color.LightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDanger) ColorDangerBackground else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Candado Icon Badge, Room Name, and Door Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ícono de Candado Principal para la puerta
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (room.doorState == DoorState.UNLOCKED)
                                    ColorRedDanger.copy(alpha = 0.15f)
                                else
                                    ColorBluePrimary.copy(alpha = 0.12f)
                            )
                            .padding(end = 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (room.doorState == DoorState.UNLOCKED) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Candado Puerta",
                            tint = if (room.doorState == DoorState.UNLOCKED) ColorRedDanger else ColorBluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = room.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        Text(
                            text = if (room.doorState == DoorState.UNLOCKED) "Cerradura: DESBLOQUEADA" else "Cerradura: BLOQUEADA",
                            fontSize = 11.sp,
                            color = if (room.doorState == DoorState.UNLOCKED) ColorRedDanger else Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Badge Estado Puerta
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (room.doorState == DoorState.UNLOCKED) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .height(14.dp)
                        )
                        Text(
                            text = if (room.doorState == DoorState.UNLOCKED) "DESBLOQUEADA" else "CERRADA",
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gas Sensor Reading (MQ-2) & Trend Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Sensor Gas",
                        tint = if (isDanger) ColorRedDanger else Color.Gray,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "MQ-2:",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Trend Badge
                    val (trendText, trendColor) = when (room.trend) {
                        GasTrend.RISING -> "▲ SUBIENDO" to ColorRedDanger
                        GasTrend.FALLING -> "▼ BAJANDO" to ColorGreenSafe
                        GasTrend.STABLE -> "⚡ ESTABLE" to Color.Gray
                    }

                    Text(
                        text = trendText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(trendColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${room.gasValue} / ${room.gasThreshold} ADC",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isDanger) ColorRedDanger else Color.Black
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    room.gasValue >= room.gasThreshold -> ColorRedDanger
                    room.gasValue >= 1000 -> ColorOrangeWarning
                    else -> ColorGreenBar
                },
                trackColor = Color.LightGray.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (room.isAlarmActive) {
                    OutlinedButton(
                        onClick = onToggleMute,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Mute",
                            tint = if (room.isMuted) Color.Gray else ColorRedDanger,
                            modifier = Modifier.height(16.dp)
                        )
                    }
                }

                if (room.isAlarmActive || room.doorState == DoorState.UNLOCKED) {
                    OutlinedButton(
                        onClick = onResetAlarm,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restablecer",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(text = "Restablecer", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onUnlockManual,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (room.doorState == DoorState.UNLOCKED) Color.Gray else ColorBluePrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (room.doorState == DoorState.UNLOCKED) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Desbloquear",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = if (room.doorState == DoorState.UNLOCKED) "Puerta Abierta" else "Abrir Puerta",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
