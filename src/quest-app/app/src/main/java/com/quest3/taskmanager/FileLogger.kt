package com.quest3.taskmanager

import android.content.Context
import android.os.Environment
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
    private var logPath = AppSettings.DEFAULT_LOG_PATH
    private var internalLogFile: File? = null
    private var inShizukuAppend = false

    fun configure(context: Context) {
        val prefs = AppSettings.prefs(context)
        enabled = prefs.getBoolean(AppSettings.KEY_LOGGING, true)
        logPath = prefs.getString(AppSettings.KEY_LOG_PATH, logPath) ?: logPath
        internalLogFile = File(context.getExternalFilesDir(null), "QTaskManager.log")
    }

    fun setEnabled(value: Boolean) { enabled = value }
    fun setLogPath(path: String) { logPath = path }

    /** Очистка лога при каждом запуске приложения из лаунчера. */
    fun onAppLaunch(context: Context) {
        configure(context)
        clearLog()
        i("Quest Task Manager started")
    }

    fun clearLog() {
        if (!enabled) return
        synchronized(lock) {
            truncateInternal()
            if (isExternalPath(logPath) && ShizukuShell.isAvailable()) {
                truncateViaShizuku(logPath)
            } else {
                truncateDirect(logPath)
            }
        }
    }

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
        if (!enabled || inShizukuAppend) return
        val line = "${timeFormat.format(Date())} [$level] $message"
        synchronized(lock) {
            appendInternal(line)
            if (isExternalPath(logPath) && ShizukuShell.isAvailable()) {
                appendViaShizuku(line, logPath)
            } else if (!appendDirect(line, logPath)) {
                appendViaShizuku(line, logPath)
            }
        }
    }

    private fun isExternalPath(path: String): Boolean =
        path.startsWith("/sdcard") || path.startsWith("/storage/")

    private fun externalPaths(): List<String> = listOf(
        logPath,
        "/storage/emulated/0/Download/QTaskManager.log",
        "/sdcard/Download/QTaskManager.log"
    ).distinct()

    private fun truncateInternal() {
        try {
            internalLogFile?.parentFile?.mkdirs()
            internalLogFile?.writeText("")
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun appendInternal(line: String): Boolean {
        val file = internalLogFile ?: return false
        return try {
            file.parentFile?.mkdirs()
            FileOutputStream(file, true).use {
                it.write("$line\n".toByteArray(utf8))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun truncateDirect(path: String): Boolean {
        for (p in externalPaths()) {
            if (truncateDirectSingle(p)) return true
        }
        try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(downloads, "QTaskManager.log").writeText("")
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun truncateDirectSingle(path: String): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText("")
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun truncateViaShizuku(path: String) {
        if (!ShizukuShell.isAvailable() || inShizukuAppend) return
        inShizukuAppend = true
        try {
            val safe = path.replace("'", "")
            ShizukuShell.run("truncate -s 0 '$safe'", timeoutSec = 5)
        } finally {
            inShizukuAppend = false
        }
    }

    private fun appendDirect(line: String, path: String): Boolean {
        val bytes = "$line\n".toByteArray(utf8)
        for (p in externalPaths()) {
            try {
                val file = File(p)
                file.parentFile?.mkdirs()
                FileOutputStream(file, true).use { it.write(bytes) }
                return true
            } catch (_: Exception) {
                // try next
            }
        }
        try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloads, "QTaskManager.log")
            file.parentFile?.mkdirs()
            FileOutputStream(file, true).use { it.write(bytes) }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun appendViaShizuku(line: String, path: String) {
        if (!ShizukuShell.isAvailable() || inShizukuAppend) return
        inShizukuAppend = true
        try {
            val encoded = Base64.encodeToString("$line\n".toByteArray(utf8), Base64.NO_WRAP)
            val safe = path.replace("'", "")
            ShizukuShell.run("echo '$encoded' | base64 -d >> '$safe'", timeoutSec = 5)
        } finally {
            inShizukuAppend = false
        }
    }
}
