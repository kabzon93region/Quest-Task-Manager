package com.quest3.taskmanager

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private const val TAG = "QTaskMgr"
    private val lock = Any()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val utf8 = StandardCharsets.UTF_8

    private var enabled = true
    private var logPath = "/sdcard/Download/QTaskManager.log"

    fun configure(context: Context) {
        val prefs = AppSettings.prefs(context)
        enabled = prefs.getBoolean(AppSettings.KEY_LOGGING, true)
        logPath = prefs.getString(AppSettings.KEY_LOG_PATH, logPath) ?: logPath
    }

    fun setEnabled(value: Boolean) { enabled = value }
    fun setLogPath(path: String) { logPath = path }

    fun i(message: String) = write("INFO", message)
    fun w(message: String) = write("WARN", message)
    fun d(message: String) = write("DEBUG", message)
    fun e(message: String, error: Throwable? = null) {
        val details = if (error != null) "$message | ${error.message}" else message
        write("ERROR", details)
    }

    private fun write(level: String, message: String) {
        when (level) {
            "ERROR" -> Log.e(TAG, message)
            "WARN" -> Log.w(TAG, message)
            "DEBUG" -> Log.d(TAG, message)
            else -> Log.i(TAG, message)
        }
        if (!enabled) return
        val line = "${timeFormat.format(Date())} [$level] $message"
        synchronized(lock) {
            if (appendFile(line)) return
            appendViaShizuku(line)
        }
    }

    private fun appendFile(line: String): Boolean {
        return try {
            val file = File(logPath)
            file.parentFile?.mkdirs()
            FileOutputStream(file, true).use {
                it.write("$line\n".toByteArray(utf8))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun appendViaShizuku(line: String) {
        if (!ShizukuShell.isAvailable()) return
        val encoded = Base64.encodeToString("$line\n".toByteArray(utf8), Base64.NO_WRAP)
        ShizukuShell.run("echo '$encoded' | base64 -d >> '$logPath'", timeoutSec = 5)
    }
}
