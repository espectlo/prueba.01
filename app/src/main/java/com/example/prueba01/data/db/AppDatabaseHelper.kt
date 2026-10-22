package com.example.prueba01.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.prueba01.data.model.EmergencyLog
import com.example.prueba01.data.model.EventType
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "safedoor_database.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD_HASH = "password_hash"
        private const val COLUMN_SALT = "salt"

        private const val TABLE_LOGS = "emergency_logs"
        private const val COLUMN_LOG_ID = "id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_ROOM_ID = "room_id"
        private const val COLUMN_ROOM_NAME = "room_name"
        private const val COLUMN_EVENT_TYPE = "event_type"
        private const val COLUMN_GAS_LEVEL = "gas_level"
        private const val COLUMN_MESSAGE = "message"

        private const val PBKDF2_ITERATIONS = 10000
        private const val HASH_KEY_LENGTH = 256
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD_HASH TEXT NOT NULL,
                $COLUMN_SALT TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createUsersTable)

        val createLogsTable = """
            CREATE TABLE $TABLE_LOGS (
                $COLUMN_LOG_ID TEXT PRIMARY KEY,
                $COLUMN_TIMESTAMP TEXT NOT NULL,
                $COLUMN_ROOM_ID TEXT NOT NULL,
                $COLUMN_ROOM_NAME TEXT NOT NULL,
                $COLUMN_EVENT_TYPE TEXT NOT NULL,
                $COLUMN_GAS_LEVEL INTEGER NOT NULL,
                $COLUMN_MESSAGE TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createLogsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        onCreate(db)
    }

    // --- AUTENTICACIÓN PBKDF2 CON SAL ALEATORIA ---

    fun hasAnyUser(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_USERS", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count > 0
    }

    private fun generateRandomSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun hashPbkdf2(password: String, salt: ByteArray): String {
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_KEY_LENGTH)
            val keyBytes = factory.generateSecret(spec).encoded
            bytesToHex(keyBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            password
        }
    }

    fun registerUser(username: String, plainPassword: String): Boolean {
        if (username.isBlank() || plainPassword.isBlank()) return false
        val salt = generateRandomSalt()
        val saltHex = bytesToHex(salt)
        val passwordHash = hashPbkdf2(plainPassword, salt)

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD_HASH, passwordHash)
            put(COLUMN_SALT, saltHex)
        }
        val result = db.insert(TABLE_USERS, null, values)
        return result != -1L
    }

    fun authenticateUser(username: String, plainPassword: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_PASSWORD_HASH, COLUMN_SALT),
            "$COLUMN_USERNAME = ?",
            arrayOf(username),
            null,
            null,
            null
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            return false
        }

        val storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH))
        val storedSaltHex = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SALT))
        cursor.close()

        val salt = hexToBytes(storedSaltHex)
        val inputHash = hashPbkdf2(plainPassword, salt)

        // Comparación de Hash en Tiempo Constante
        return MessageDigest.isEqual(
            inputHash.toByteArray(StandardCharsets.UTF_8),
            storedHash.toByteArray(StandardCharsets.UTF_8)
        )
    }

    // --- HISTORIAL DE EVENTOS ---

    fun insertEmergencyLog(log: EmergencyLog) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LOG_ID, log.id)
            put(COLUMN_TIMESTAMP, log.timestamp)
            put(COLUMN_ROOM_ID, log.roomId)
            put(COLUMN_ROOM_NAME, log.roomName)
            put(COLUMN_EVENT_TYPE, log.eventType.name)
            put(COLUMN_GAS_LEVEL, log.gasLevel)
            put(COLUMN_MESSAGE, log.message)
        }
        db.insertWithOnConflict(TABLE_LOGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllEmergencyLogs(): List<EmergencyLog> {
        val logs = mutableListOf<EmergencyLog>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_LOGS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_ID))
                val timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                val roomId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM_ID))
                val roomName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM_NAME))
                val eventTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TYPE))
                val gasLevel = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAS_LEVEL))
                val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))

                val eventType = try {
                    EventType.valueOf(eventTypeStr)
                } catch (e: Exception) {
                    EventType.GAS_ALARM_AUTOMATIC
                }

                logs.add(
                    EmergencyLog(
                        id = id,
                        timestamp = timestamp,
                        roomId = roomId,
                        roomName = roomName,
                        eventType = eventType,
                        gasLevel = gasLevel,
                        message = message
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return logs
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
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
