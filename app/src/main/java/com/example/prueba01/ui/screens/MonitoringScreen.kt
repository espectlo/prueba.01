package com.example.prueba01.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba01.data.model.DoorState
import com.example.prueba01.data.model.Room
import com.example.prueba01.ui.components.RoomCard

@Composable
fun MonitoringScreen(
    rooms: List<Room>,
    onUnlockManual: (String) -> Unit,
    onResetAlarm: (String) -> Unit,
    onToggleMute: (String) -> Unit
) {
    val lockedCount = remember(rooms) { rooms.count { it.doorState == DoorState.LOCKED } }
    val unlockedCount = remember(rooms) { rooms.count { it.doorState == DoorState.UNLOCKED } }
    val maxGasLevel = remember(rooms) { rooms.maxOfOrNull { it.gasValue } ?: 0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Summary Metrics Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Puertas Cerradas",
                    value = "$lockedCount",
                    icon = Icons.Default.Lock,
                    color = Color(0xFF2E7D32)
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Desbloqueadas",
                    value = "$unlockedCount",
                    icon = Icons.Default.LockOpen,
                    color = if (unlockedCount > 0) Color(0xFFD32F2F) else Color(0xFF1976D2)
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Max Gas MQ-2",
                    value = "$maxGasLevel",
                    icon = Icons.Default.Sensors,
                    color = if (maxGasLevel >= 1800) Color(0xFFD32F2F) else Color(0xFFE65100)
                )
            }
        }

        item {
            Text(
                text = "Monitoreo en Tiempo Real de Salas (${rooms.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Room Cards List
        items(rooms, key = { it.id }) { room ->
            RoomCard(
                room = room,
                onUnlockManual = { onUnlockManual(room.id) },
                onResetAlarm = { onResetAlarm(room.id) },
                onToggleMute = { onToggleMute(room.id) }
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.height(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
