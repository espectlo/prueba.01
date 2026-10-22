package com.example.prueba01.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityIso27000Manager {

    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 100000 // 100,000 Iteraciones según recomendaciones NIST

    // Contador de secuencia de salida (Outbound Sequence Number)
    private val outboundSequenceCounter = AtomicLong(System.currentTimeMillis())

    // Registro estricto del último número de secuencia entrante recibido (Inbound Anti-Replay Verification)
    private var lastReceivedSeqNum: Long = -1L

    private var masterKey: String = "UserConfiguredKey2026"

    private var aesKey: SecretKeySpec? = null
    private var hmacKey: SecretKeySpec? = null

    init {
        deriveKeys(masterKey)
    }

    fun setMasterKey(newKey: String) {
        if (newKey.isNotBlank()) {
            masterKey = newKey
            deriveKeys(masterKey)
        }
    }

    fun getMasterKey(): String = masterKey

    /**
     * Deriva dos claves independientes con PBKDF2 (100,000 iteraciones) y sales distintas:
     * 1. Clave de Cifrado AES-256-GCM
     * 2. Clave de Firma HMAC-SHA256 (Defensa en Profundidad)
     */
    private fun deriveKeys(password: String) {
        try {
            val encSalt = "AES_GCM_SALT_2026_SAFEDOOR".toByteArray(StandardCharsets.UTF_8)
            val hmacSalt = "HMAC_SHA256_SALT_2026_SAFEDOOR".toByteArray(StandardCharsets.UTF_8)

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

            val encSpec = PBEKeySpec(password.toCharArray(), encSalt, PBKDF2_ITERATIONS, 256)
            aesKey = SecretKeySpec(factory.generateSecret(encSpec).encoded, "AES")

            val hmacSpec = PBEKeySpec(password.toCharArray(), hmacSalt, PBKDF2_ITERATIONS, 256)
            hmacKey = SecretKeySpec(factory.generateSecret(hmacSpec).encoded, "HmacSHA256")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class DecryptedFrameResult(
        val isValid: Boolean,
        val payload: String,
        val errorMessage: String? = null
    )

    fun encryptFrame(plainText: String): String {
        val currentAesKey = aesKey ?: return plainText

        return try {
            val seqNum = outboundSequenceCounter.incrementAndGet()

            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, currentAesKey, spec)

            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

            val hexIv = bytesToHex(iv)
            val hexPayload = bytesToHex(encryptedBytes)

            // Firma HMAC-SHA256 sobre (SeqNum + IV + Payload) - Defensa en Profundidad
            val dataToSign = "$seqNum:$hexIv:$hexPayload"
            val hmacSignature = calculateHmacSha256Hex(dataToSign)

            "SEC_FRAME:$seqNum:$hexIv:$hexPayload:$hmacSignature"
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    /**
     * Descifrado y Verificación Anti-Replay Estricta sobre Tramas Entrantes.
     */
    fun decryptFrame(rawFrame: String): DecryptedFrameResult {
        if (!rawFrame.startsWith("SEC_FRAME:")) {
            return DecryptedFrameResult(
                isValid = false,
                payload = "",
                errorMessage = "REJECTED: Trama descartada por no contener protocolo seguro SEC_FRAME (No se admite texto plano)."
            )
        }

        val parts = rawFrame.split(":")
        if (parts.size != 5) {
            return DecryptedFrameResult(
                isValid = false,
                payload = "",
                errorMessage = "REJECTED: Estructura de trama de seguridad inválida."
            )
        }

        val incomingSeqNum = parts[1].toLongOrNull()
            ?: return DecryptedFrameResult(isValid = false, payload = "", errorMessage = "REJECTED: Número de secuencia no válido.")
        val hexIv = parts[2]
        val hexPayload = parts[3]
        val expectedHmacHex = parts[4]

        // 1. Verificación Estricta Anti-Replay (Número de secuencia estrictamente creciente)
        if (incomingSeqNum <= lastReceivedSeqNum) {
            return DecryptedFrameResult(
                isValid = false,
                payload = "",
                errorMessage = "REJECTED (Anti-Replay Attack): Número de secuencia $incomingSeqNum <= $lastReceivedSeqNum (Trama retransmitida)."
            )
        }

        // 2. Verificación de Firma HMAC-SHA256 en TIEMPO CONSTANTE (MessageDigest.isEqual)
        val dataToVerify = "$incomingSeqNum:$hexIv:$hexPayload"
        val calculatedHmacHex = calculateHmacSha256Hex(dataToVerify)

        val expectedHmacBytes = hexToBytes(expectedHmacHex)
        val calculatedHmacBytes = hexToBytes(calculatedHmacHex)

        if (!MessageDigest.isEqual(calculatedHmacBytes, expectedHmacBytes)) {
            return DecryptedFrameResult(
                isValid = false,
                payload = "",
                errorMessage = "REJECTED (Integridad/Firma): Firma HMAC no coincide (Ataque MitM o alteración de datos)."
            )
        }

        // Actualizar último número de secuencia válido
        lastReceivedSeqNum = incomingSeqNum

        // 3. Descifrado Autenticado AES-256-GCM
        val currentAesKey = aesKey ?: return DecryptedFrameResult(isValid = false, payload = "", errorMessage = "Clave AES no inicializada.")

        return try {
            val iv = hexToBytes(hexIv)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, currentAesKey, spec)

            val decryptedBytes = cipher.doFinal(hexToBytes(hexPayload))
            val plainText = String(decryptedBytes, StandardCharsets.UTF_8)
            DecryptedFrameResult(isValid = true, payload = plainText)
        } catch (e: Exception) {
            DecryptedFrameResult(
                isValid = false,
                payload = "",
                errorMessage = "REJECTED: Fallo en descifrado AES-GCM (Clave incorrecta o etiqueta GCM alterada): ${e.message}"
            )
        }
    }

    private fun calculateHmacSha256Hex(data: String): String {
        val currentHmacKey = hmacKey ?: return ""
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(currentHmacKey)
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
