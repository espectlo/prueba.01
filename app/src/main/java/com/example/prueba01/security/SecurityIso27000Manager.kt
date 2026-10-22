package com.example.prueba01.security

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecurityIso27000Manager {

    private const val SECRET_KEY_STRING = "SafeDoorIoT2026S"
    private val aesKeySpec = SecretKeySpec(SECRET_KEY_STRING.toByteArray(StandardCharsets.UTF_8), "AES")
    private val hmacKeySpec = SecretKeySpec(SECRET_KEY_STRING.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")

    private val processedNonces = mutableSetOf<Long>()
    private const val MAX_NONCE_AGE_MS = 30000L // 30 segundos

    data class DecryptedFrameResult(
        val isValid: Boolean,
        val payload: String,
        val errorMessage: String? = null
    )

    fun encryptFrame(plainText: String): String {
        return try {
            val nonceTimestamp = System.currentTimeMillis()

            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, aesKeySpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val hexEncrypted = bytesToHex(encryptedBytes)

            val dataToSign = "$nonceTimestamp:$hexEncrypted"
            val hmacSignature = calculateHmacSha256Hex(dataToSign)

            "SEC_FRAME:$nonceTimestamp:$hexEncrypted:$hmacSignature"
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    fun decryptFrame(rawFrame: String): DecryptedFrameResult {
        if (!rawFrame.startsWith("SEC_FRAME:")) {
            return DecryptedFrameResult(isValid = true, payload = rawFrame)
        }

        val parts = rawFrame.split(":")
        if (parts.size != 4) {
            return DecryptedFrameResult(isValid = false, payload = "", errorMessage = "Estructura de trama inválida")
        }

        val nonceTimestamp = parts[1].toLongOrNull()
            ?: return DecryptedFrameResult(isValid = false, payload = "", errorMessage = "Formato de Nonce/Timestamp no válido")
        val hexEncrypted = parts[2]
        val expectedHmac = parts[3]

        val currentTime = System.currentTimeMillis()

        if (currentTime - nonceTimestamp > MAX_NONCE_AGE_MS) {
            return DecryptedFrameResult(
                isValid = false,
                payload = "",
                errorMessage = "REJECTED (Anti-Replay Attack): Trama expirada (> 30s)"
            )
        }

        if (processedNonces.contains(nonceTimestamp)) {
            return DecryptedFrameResult(
                isValid = false,
                payload = "",
                errorMessage = "REJECTED (Anti-Replay Attack): Nonce duplicado detectado."
            )
        }

        val dataToVerify = "$nonceTimestamp:$hexEncrypted"
        val calculatedHmac = calculateHmacSha256Hex(dataToVerify)

        if (!calculatedHmac.equals(expectedHmac, ignoreCase = true)) {
            return DecryptedFrameResult(
                isValid = false,
                payload = "",
                errorMessage = "REJECTED (Integridad/Firma): Firma HMAC-SHA256 no coincide."
            )
        }

        processedNonces.add(nonceTimestamp)
        if (processedNonces.size > 100) {
            processedNonces.clear()
        }

        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, aesKeySpec)
            val decryptedBytes = cipher.doFinal(hexToBytes(hexEncrypted))
            val plainText = String(decryptedBytes, StandardCharsets.UTF_8)
            DecryptedFrameResult(isValid = true, payload = plainText)
        } catch (e: Exception) {
            DecryptedFrameResult(isValid = false, payload = "", errorMessage = "Error en llave de cifrado AES: ${e.message}")
        }
    }

    private fun calculateHmacSha256Hex(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKeySpec)
        val hmacBytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(hmacBytes)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
