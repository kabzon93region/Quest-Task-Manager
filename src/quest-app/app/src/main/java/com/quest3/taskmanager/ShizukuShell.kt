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
        Log.d(TAG, "shell exit=$exit cmd=${command.take(100)}")
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
        val safe = sanitizePackage(packageName)
        if (safe.isBlank()) return false

        run("am force-stop '$safe'", timeoutSec = 8)
        run("am kill '$safe'", timeoutSec = 8)

        val before = collectPids(safe)
        for (pid in before) {
            run("kill -9 $pid", timeoutSec = 3)
        }

        val after = collectPids(safe)
        val ok = after.isEmpty()
        FileLogger.i("force-stop $safe pids=${before.size} remaining=${after.size} ok=$ok")
        return ok
    }

    private fun collectPids(packageName: String): Set<Int> {
        val pids = linkedSetOf<Int>()
        val pidof = run("pidof '$packageName' 2>/dev/null").stdout.trim()
        if (pidof.isNotBlank()) {
            pidof.split(Regex("""\s+""")).mapNotNull { it.toIntOrNull() }.forEach { pids.add(it) }
        }
        val ps = run("ps -A -o PID,NAME").combined
        for (line in ps.lineSequence()) {
            val parts = line.trim().split(Regex("""\s+"""))
            if (parts.size < 2) continue
            val pid = parts[0].toIntOrNull() ?: continue
            val name = RunningAppsProbe.normalizePackageName(parts[1])
            if (name == packageName || parts[1].contains(packageName)) {
                pids.add(pid)
            }
        }
        return pids
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
