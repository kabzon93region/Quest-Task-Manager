package com.quest3.taskmanager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    private val pm = context.packageManager
    private val policy = ProtectedAppsPolicy(context, context.packageName)
    private val ownPackage = context.packageName

    suspend fun loadRunningEntries(): List<AppEntry> = withContext(Dispatchers.IO) {
        requireShizuku()
        val snapshot = RunningAppsProbe.collectRunningSnapshot()
        val installed = installedPackageNames()
        val packages = snapshot.displayPackages
            .filter { it in installed && !RunningAppsProbe.isNativeProcessName(it) }
            .toSet()
        val daemonNames = RunningAppsProbe.collectDaemonNames(snapshot, installed)
        FileLogger.d(
            "running filter: apps=${packages.size} daemons=${daemonNames.size}"
        )
        val disk = StorageProbe.loadDiskSizes(packages)
        val appEntries = buildEntries(
            packageNames = packages,
            snapshot = snapshot,
            disk = disk,
            includePolicies = false,
            policyCtx = null
        )
        val daemonEntries = buildDaemonEntries(
            names = daemonNames,
            snapshot = snapshot,
            includePolicies = false,
            policyCtx = null
        )
        mergeSorted(appEntries, daemonEntries)
    }

    suspend fun loadAllEntries(): List<AppEntry> = withContext(Dispatchers.IO) {
        requireShizuku()
        val snapshot = RunningAppsProbe.collectRunningSnapshot()
        val installed = installedPackageNames()
        val policyCtx = BackgroundPolicy.loadContext()
        val allPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { it.packageName }
            .toSet()
        val daemonNames = RunningAppsProbe.collectDaemonNames(snapshot, installed)
        val disk = StorageProbe.loadDiskSizes(allPackages)
        val appEntries = buildEntries(
            packageNames = allPackages,
            snapshot = snapshot,
            disk = disk,
            includePolicies = true,
            policyCtx = policyCtx
        )
        val daemonEntries = buildDaemonEntries(
            names = daemonNames,
            snapshot = snapshot,
            includePolicies = true,
            policyCtx = policyCtx
        )
        mergeSorted(appEntries, daemonEntries)
    }

    suspend fun killPackages(packages: Collection<String>): KillResult = withContext(Dispatchers.IO) {
        var killed = 0
        var skippedProtected = 0
        var failed = 0
        for (pkg in orderedForKill(packages)) {
            if (pkg == ownPackage) {
                skippedProtected++
                FileLogger.w("kill skipped (self): $pkg")
                continue
            }
            if (ShizukuShell.killTarget(pkg)) killed++ else failed++
        }
        KillResult(killed, skippedProtected, failed)
    }

    suspend fun killByRules(candidates: Collection<String>): KillResult = withContext(Dispatchers.IO) {
        val targets = candidates.filter {
            it != ownPackage && BackgroundPolicy.isRunInBackgroundBlocked(it)
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
    }

    private fun buildDaemonEntries(
        names: Set<String>,
        snapshot: RunningSnapshot,
        includePolicies: Boolean,
        policyCtx: PolicyContext?
    ): List<AppEntry> {
        return names.mapNotNull { name ->
            toDaemonEntry(name, snapshot, includePolicies, policyCtx)
        }
    }

    private fun mergeSorted(apps: List<AppEntry>, daemons: List<AppEntry>): List<AppEntry> =
        (apps + daemons).sortedWith(
            compareByDescending<AppEntry> { it.processState != ProcessState.NONE }
                .thenByDescending { it.ramUsageKb ?: 0 }
                .thenBy { it.label.lowercase() }
        )

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
            val isSystem = classifyIsSystem(packageName, appInfo)

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
                isDaemon = false,
                processState = processState,
                diskSizeKb = disk[packageName],
                ramUsageKb = if (processState != ProcessState.NONE) (ramKb ?: 0L) else null,
                runInBackgroundAllowed = runAllowed,
                backgroundDataAllowed = dataAllowed,
                icon = icon
            )
        } catch (_: PackageManager.NameNotFoundException) {
            buildUnknownEntry(packageName, snapshot, disk)
        }
    }

    private fun toDaemonEntry(
        name: String,
        snapshot: RunningSnapshot,
        includePolicies: Boolean,
        policyCtx: PolicyContext?
    ): AppEntry? {
        val processState = snapshot.daemonProcessState(name)
        if (processState == ProcessState.NONE) return null
        val ramKb = snapshot.extendedRam[name] ?: 0L
        val appInfo = try {
            pm.getApplicationInfo(name, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: name
        val icon = appInfo?.let { pm.getApplicationIcon(it) } ?: pm.getDefaultActivityIcon()
        val policies = resolvePolicies(name, appInfo != null, includePolicies, policyCtx)

        return AppEntry(
            packageName = name,
            label = label,
            isSystem = true,
            isDaemon = true,
            processState = processState,
            diskSizeKb = null,
            ramUsageKb = ramKb,
            runInBackgroundAllowed = policies.first,
            backgroundDataAllowed = policies.second,
            icon = icon
        )
    }

    private fun resolvePolicies(
        name: String,
        hasAppInfo: Boolean,
        includePolicies: Boolean,
        policyCtx: PolicyContext?
    ): Pair<Boolean?, Boolean?> {
        if (!includePolicies || policyCtx == null) return null to null
        if (!hasAppInfo && RunningAppsProbe.isNativeProcessName(name)) return null to null
        return try {
            !BackgroundPolicy.isRunInBackgroundBlocked(name) to
                !BackgroundPolicy.isBackgroundDataBlocked(name, policyCtx)
        } catch (_: Exception) {
            null to null
        }
    }

    private fun buildUnknownEntry(
        packageName: String,
        snapshot: RunningSnapshot,
        disk: Map<String, Long>
    ): AppEntry? {
        if (!RunningAppsProbe.isLikelyPackageName(packageName)) return null
        val processState = snapshot.processState(packageName)
        val ramKb = snapshot.ramMap.byPackage[packageName]
        return AppEntry(
            packageName = packageName,
            label = packageName,
            isSystem = classifyIsSystem(packageName, null),
            isDaemon = false,
            processState = processState,
            diskSizeKb = disk[packageName],
            ramUsageKb = if (processState != ProcessState.NONE) (ramKb ?: 0L) else null,
            runInBackgroundAllowed = null,
            backgroundDataAllowed = null,
            icon = pm.getDefaultActivityIcon()
        )
    }

    private fun classifyIsSystem(packageName: String, appInfo: ApplicationInfo?): Boolean {
        val systemFlag = when {
            appInfo != null -> isSystemApp(appInfo) || appInfo.uid < Process.FIRST_APPLICATION_UID
            else -> try {
                pm.getPackageUid(packageName, 0) < Process.FIRST_APPLICATION_UID
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
        return policy.isSystemForFilter(packageName, systemFlag)
    }

    private fun installedPackageNames(): Set<String> =
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { it.packageName }
            .toSet()

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean =
        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    private fun requireShizuku() {
        check(ShizukuShell.isAvailable() && ShizukuShell.hasPermission()) {
            "Shizuku unavailable"
        }
    }
}
