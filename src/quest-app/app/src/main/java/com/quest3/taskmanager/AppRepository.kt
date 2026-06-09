package com.quest3.taskmanager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    private val pm = context.packageManager
    private val policy = ProtectedAppsPolicy(context, context.packageName)
    private val ownPackage = context.packageName

    suspend fun loadRunningEntries(): List<AppEntry> = withContext(Dispatchers.IO) {
        requireShizuku()
        val snapshot = RunningAppsProbe.collectRunningSnapshot()
        val packages = snapshot.displayPackages
        val disk = StorageProbe.loadDiskSizes(packages)
        buildEntries(
            packageNames = packages,
            snapshot = snapshot,
            disk = disk,
            includePolicies = false,
            policyCtx = null
        )
    }

    suspend fun loadAllEntries(): List<AppEntry> = withContext(Dispatchers.IO) {
        requireShizuku()
        val snapshot = RunningAppsProbe.collectRunningSnapshot()
        val policyCtx = BackgroundPolicy.loadContext()
        val allPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { it.packageName }
            .toSet()
        val disk = StorageProbe.loadDiskSizes(allPackages)
        buildEntries(
            packageNames = allPackages,
            snapshot = snapshot,
            disk = disk,
            includePolicies = true,
            policyCtx = policyCtx
        )
    }

    suspend fun killPackages(packages: Collection<String>): Int = withContext(Dispatchers.IO) {
        var killed = 0
        for (pkg in orderedForKill(packages)) {
            if (policy.isKillProtected(pkg) && pkg != ownPackage) continue
            if (ShizukuShell.forceStop(pkg)) killed++
        }
        killed
    }

    suspend fun killByRules(candidates: Collection<String>): Int = withContext(Dispatchers.IO) {
        val targets = candidates.filter {
            !policy.isKillProtected(it) && BackgroundPolicy.isRunInBackgroundBlocked(it)
        }
        killPackages(targets)
    }

    fun orderedForKill(packages: Collection<String>): List<String> {
        val (self, others) = packages.distinct().partition { it == ownPackage }
        return others + self
    }

    fun isKillProtected(packageName: String): Boolean = policy.isKillProtected(packageName)

    private fun buildEntries(
        packageNames: Set<String>,
        snapshot: RunningSnapshot,
        disk: Map<String, Long>,
        includePolicies: Boolean,
        policyCtx: PolicyContext?
    ): List<AppEntry> {
        return packageNames
            .mapNotNull { pkg ->
                toEntry(pkg, snapshot, disk, includePolicies, policyCtx)
            }
            .sortedWith(
                compareByDescending<AppEntry> { it.processState != ProcessState.NONE }
                    .thenByDescending { it.ramUsageKb ?: 0 }
                    .thenBy { it.label.lowercase() }
            )
    }

    private fun toEntry(
        packageName: String,
        snapshot: RunningSnapshot,
        disk: Map<String, Long>,
        includePolicies: Boolean,
        policyCtx: PolicyContext?
    ): AppEntry? {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            val processState = snapshot.processState(packageName)
            val ramKb = snapshot.ramMap.byPackage[packageName]
            val isSystem = policy.isSystemForFilter(
                packageName,
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )

            val runAllowed = if (includePolicies && policyCtx != null) {
                !BackgroundPolicy.isRunInBackgroundBlocked(packageName)
            } else null

            val dataAllowed = if (includePolicies && policyCtx != null) {
                !BackgroundPolicy.isBackgroundDataBlocked(packageName, policyCtx)
            } else null

            AppEntry(
                packageName = packageName,
                label = label,
                isSystem = isSystem,
                processState = processState,
                diskSizeKb = disk[packageName],
                ramUsageKb = if (processState != ProcessState.NONE) (ramKb ?: 0L) else null,
                runInBackgroundAllowed = runAllowed,
                backgroundDataAllowed = dataAllowed,
                icon = icon
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun requireShizuku() {
        check(ShizukuShell.isAvailable() && ShizukuShell.hasPermission()) {
            "Shizuku unavailable"
        }
    }
}
