package com.example.prueba01.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.prueba01.data.model.EmergencyLog
import com.example.prueba01.data.model.EventType
import org.json.JSONArray
import org.json.JSONObject

class OfflineStorageRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("safedoor_offline_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PENDING_LOGS = "pending_offline_logs"
    }

    fun savePendingOfflineLog(log: EmergencyLog) {
        val currentLogs = getPendingOfflineLogs().toMutableList()
        currentLogs.add(log)
        saveLogsToPrefs(currentLogs)
    }

    fun getPendingOfflineLogs(): List<EmergencyLog> {
        val jsonString = prefs.getString(KEY_PENDING_LOGS, null) ?: return emptyList()
        val logs = mutableListOf<EmergencyLog>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                logs.add(
                    EmergencyLog(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        timestamp = obj.optString("timestamp", ""),
                        roomId = obj.optString("roomId", "1"),
                        roomName = obj.optString("roomName", "Desconocida"),
                        eventType = EventType.valueOf(obj.optString("eventType", EventType.GAS_ALARM_AUTOMATIC.name)),
                        gasLevel = obj.optInt("gasLevel", 0),
                        message = obj.optString("message", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return logs
    }

    fun clearPendingLogs() {
        prefs.edit().remove(KEY_PENDING_LOGS).apply()
    }

    private fun saveLogsToPrefs(logs: List<EmergencyLog>) {
        try {
            val jsonArray = JSONArray()
            logs.take(50).forEach { log ->
                val obj = JSONObject().apply {
                    put("id", log.id)
                    put("timestamp", log.timestamp)
                    put("roomId", log.roomId)
                    put("roomName", log.roomName)
                    put("eventType", log.eventType.name)
                    put("gasLevel", log.gasLevel)
                    put("message", log.message)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_PENDING_LOGS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
