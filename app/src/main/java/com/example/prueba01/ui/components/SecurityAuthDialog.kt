package com.example.prueba01.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SecurityAuthDialog(
    actionTitle: String,
    onVerifyPin: (String) -> Boolean,
    onAuthSuccess: () -> Unit,
    onDismiss: () -> Unit,
    onTriggerBiometric: () -> Unit = {}
) {
    var pinText by remember { mutableStateOf("") }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isLockedOut = failedAttempts >= 3

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Autenticación Requerida",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Acción protegida: $actionTitle",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = "Ingrese su PIN de Seguridad de 4 dígitos o use Biometría.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 4 && !isLockedOut) {
                            pinText = it
                            errorMessage = null
                        }
                    },
                    enabled = !isLockedOut,
                    label = { Text(if (isLockedOut) "BLOQUEADO" else "PIN de 4 dígitos") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = errorMessage != null || isLockedOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (isLockedOut) {
                    Text(
                        text = "🔒 Demasiados intentos fallidos (3/3). Acceso bloqueado temporalmente por seguridad.",
                        color = Color(0xFFD32F2F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFD32F2F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onTriggerBiometric,
                    enabled = !isLockedOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometría",
                        tint = if (isLockedOut) Color.Gray else Color(0xFF1976D2),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(text = "Validar Huella Dactilar / Face ID", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isLockedOut) return@Button
                    val isCorrect = onVerifyPin(pinText)
                    if (isCorrect) {
                        onAuthSuccess()
                    } else {
                        failedAttempts++
                        errorMessage = "PIN incorrecto. Intentos fallidos: $failedAttempts/3"
                    }
                },
                enabled = !isLockedOut && pinText.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
