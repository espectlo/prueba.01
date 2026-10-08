package com.example.prueba01.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.prueba01.data.model.DoorState

@Composable
fun AnimatedDoorGraphic(
    doorState: DoorState,
    modifier: Modifier = Modifier
) {
    // Ángulo del servomotor SG90: 0° para cerrada, 120° para abierta
    val targetAngle = if (doorState == DoorState.UNLOCKED) 120f else 0f
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 800),
        label = "ServoAngleAnimation"
    )

    val doorColor = if (doorState == DoorState.UNLOCKED) Color(0xFFD32F2F) else Color(0xFF2E7D32)
    val frameColor = Color.DarkGray

    Box(
        modifier = modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Marco de la puerta (Frame)
            drawRoundRect(
                color = frameColor,
                topLeft = Offset(4f, 4f),
                size = Size(width - 8f, height - 8f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 4f)
            )

            // Bisagra / Eje del Servomotor SG90 (Hinge at top-left)
            drawCircle(
                color = Color.Black,
                radius = 6f,
                center = Offset(10f, 10f)
            )

            // Hoja de la puerta rotando desde la bisagra (10f, 10f)
            rotate(degrees = -animatedAngle, pivot = Offset(10f, 10f)) {
                drawRoundRect(
                    color = doorColor,
                    topLeft = Offset(10f, 10f),
                    size = Size(width - 20f, height - 20f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Pomo / Manilla de la puerta
                drawCircle(
                    color = Color.Yellow,
                    radius = 4f,
                    center = Offset(width - 26f, height / 2f)
                )
            }
        }
    }
}
