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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba01.data.model.EmergencyLog
import com.example.prueba01.data.model.EventType

@Composable
fun HistoryScreen(
    logs: List<EmergencyLog>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Historial de Eventos de Emergencia (${logs.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(bottom = 8.dp)
                    )
                    Text(
                        text = "No se han registrado eventos de alerta.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Las emergencias o aperturas manuales aparecerán aquí.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: EmergencyLog) {
    val (color, icon, title) = when (log.eventType) {
        EventType.GAS_ALARM_AUTOMATIC -> Triple(Color(0xFFD32F2F), Icons.Default.Warning, "¡ALARMA AUTOMÁTICA DE HUMO!")
        EventType.MANUAL_DOOR_UNLOCK -> Triple(Color(0xFFE65100), Icons.Default.LockOpen, "DESBLOQUEO MANUAL DESDE APP")
        EventType.ALARM_RESET -> Triple(Color(0xFF2E7D32), Icons.Default.Refresh, "RESTABLECIMIENTO DE ALARMA")
        else -> Triple(Color.Gray, Icons.Default.History, "EVENTO DE SISTEMA")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.height(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = color
                    )
                    Text(
                        text = log.timestamp,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Lugar: ${log.roomName} | Gas MQ-2: ${log.gasLevel}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color.Black
                )

                Text(
                    text = log.message,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
