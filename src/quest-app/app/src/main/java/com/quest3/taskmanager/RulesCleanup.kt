package com.quest3.taskmanager

import android.content.Context

class RulesCleanup(private val context: Context) {
    fun run(): CleanResult {
        FileLogger.i("=== rules cleanup start ===")
        val repository = AppRepository(context)
        val policy = ProtectedAppsPolicy(context, context.packageName)
        val snapshot = RunningAppsProbe.collectRunningSnapshot()
        val candidates = snapshot.displayPackages.filter { !policy.isKillProtected(it) }

        val toKill = candidates.filter { BackgroundPolicy.isRunInBackgroundBlocked(it) }
        val skipped = candidates.size - toKill.size

        var killed = 0
        for (pkg in repository.orderedForKill(toKill)) {
            if (ShizukuShell.forceStop(pkg)) {
                killed++
                FileLogger.i("killed: $pkg")
            }
        }

        FileLogger.i("=== rules cleanup done killed=$killed candidates=${toKill.size} ===")
        return CleanResult(
            scannedCount = candidates.size,
            targetCount = toKill.size,
            killedCount = killed,
            skippedAllowedCount = skipped
        )
    }
}

data class CleanResult(
    val scannedCount: Int,
    val targetCount: Int,
    val killedCount: Int,
    val skippedAllowedCount: Int = 0
)
