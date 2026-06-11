package com.quest3.taskmanager

import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object ShizukuShell {
    private const val TAG = "QTaskMgr"

    fun isAvailable(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean =
        Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun run(command: String, timeoutSec: Long = 25): ShellResult {
        val process = try {
            Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        } catch (e: Exception) {
            Log.w(TAG, "newProcess failed", e)
            return ShellResult(-1, "", e.message.orEmpty())
        }

        val stdout = readStream(process.inputStream, timeoutSec)
        val stderr = readStream(process.errorStream, minOf(5L, timeoutSec))

        val exit = try {
            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                destroyQuietly(process)
                -1
            } else {
                exitValueQuietly(process)
            }
        } catch (e: Exception) {
            Log.w(TAG, "shell wait failed cmd=${command.take(80)}", e)
            destroyQuietly(process)
            -1
        }

        Log.d(TAG, "shell exit=$exit cmd=${command.take(100)}")
        return ShellResult(exit, stdout, if (exit < 0 && stderr.isBlank()) "timeout" else stderr)
    }

    private fun exitValueQuietly(process: Process): Int =
        try {
            process.exitValue()
        } catch (_: IllegalArgumentException) {
            // Shizuku: waitFor=true, но процесс ещё не отчитался — для am force-stop это нормально
            0
        } catch (_: Exception) {
            -1
        }

    private fun destroyQuietly(process: Process) {
        try {
            process.destroy()
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun readStream(stream: java.io.InputStream, timeoutSec: Long): String {
        val builder = StringBuilder()
        val thread = Thread {
            try {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { input ->
                    val buffer = CharArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        builder.append(buffer, 0, read)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "readStream", e)
            }
        }
        thread.start()
        thread.join(timeoutSec * 1000)
        return builder.toString()
    }

    fun forceStop(packageName: String): Boolean {
        val safe = sanitizePackage(packageName)
        if (safe.isBlank()) return false

        return try {
            val stop = run("am force-stop '$safe'", timeoutSec = 8)
            run("am kill '$safe'", timeoutSec = 5)
            // am force-stop достаточно; не учитываем фоновые *.$package.service.* в ps
            val ok = stop.exitCode == 0
            FileLogger.i("force-stop $safe exit=${stop.exitCode} ok=$ok")
            ok
        } catch (e: Exception) {
            FileLogger.e("force-stop failed: $safe", e)
            false
        }
    }

    private fun sanitizePackage(packageName: String): String =
        packageName.replace("'", "").replace(";", "").trim()
}

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val combined: String get() = if (stderr.isBlank()) stdout else "$stdout\n$stderr"
}
