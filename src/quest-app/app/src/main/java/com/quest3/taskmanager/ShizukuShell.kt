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
        val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        val stdout = readStream(process.inputStream, timeoutSec)
        val stderr = readStream(process.errorStream, 5)
        val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
        if (!finished) {
            process.destroy()
            return ShellResult(-1, stdout, stderr.ifBlank { "timeout" })
        }
        val exit = process.exitValue()
        FileLogger.d("shell exit=$exit cmd=${command.take(100)}")
        return ShellResult(exit, stdout, stderr)
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
        val safe = packageName.replace("'", "").replace(";", "")
        if (safe.isBlank()) return false
        val result = run("am force-stop '$safe'", timeoutSec = 8)
        FileLogger.i("force-stop $safe exit=${result.exitCode}")
        return result.exitCode == 0
    }
}

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val combined: String get() = if (stderr.isBlank()) stdout else "$stdout\n$stderr"
}
