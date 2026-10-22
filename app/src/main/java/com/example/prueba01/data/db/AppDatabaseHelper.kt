package com.example.prueba01.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.prueba01.data.model.EmergencyLog
import com.example.prueba01.data.model.EventType
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "safedoor_database.db"
        private const val DATABASE_VERSION = 1

        // Tabla Usuarios (Login con HASH SHA-256)
        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD_HASH = "password_hash"

        // Tabla Historial de Eventos (Persistencia permanente de logs)
        private const val TABLE_LOGS = "emergency_logs"
        private const val COLUMN_LOG_ID = "id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_ROOM_ID = "room_id"
        private const val COLUMN_ROOM_NAME = "room_name"
        private const val COLUMN_EVENT_TYPE = "event_type"
        private const val COLUMN_GAS_LEVEL = "gas_level"
        private const val COLUMN_MESSAGE = "message"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear Tabla Usuarios
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD_HASH TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createUsersTable)

        // Crear Tabla Historial de Eventos
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

        // Insertar usuario administrador por defecto: admin / admin123 (Guardando HASH SHA-256)
        val defaultAdminHash = hashSha256("admin123")
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, "admin")
            put(COLUMN_PASSWORD_HASH, defaultAdminHash)
        }
        db.insert(TABLE_USERS, null, values)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        onCreate(db)
    }

    // --- MÉTODOS DE AUTENTICACIÓN Y LOGIN CON HASH SHA-256 ---

    fun hashSha256(plainText: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(plainText.toByteArray(StandardCharsets.UTF_8))
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            plainText
        }
    }

    fun authenticateUser(username: String, plainPassword: String): Boolean {
        val inputHash = hashSha256(plainPassword)
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_USER_ID),
            "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD_HASH = ?",
            arrayOf(username, inputHash),
            null,
            null,
            null
        )
        val isValid = cursor.count > 0
        cursor.close()
        return isValid
    }

    fun registerUser(username: String, plainPassword: String): Boolean {
        val passwordHash = hashSha256(plainPassword)
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD_HASH, passwordHash)
        }
        val result = db.insert(TABLE_USERS, null, values)
        return result != -1L
    }

    // --- MÉTODOS DE HISTORIAL DE EVENTOS PERSISTENTE ---

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
}
