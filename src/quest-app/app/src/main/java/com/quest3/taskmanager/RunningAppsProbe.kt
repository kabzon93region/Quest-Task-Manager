package com.quest3.taskmanager

data class RunningSnapshot(
    val psPackages: Set<String>,
    val psActiveNames: Set<String>,
    val ramMap: MemoryProbe.RamMap,
    val extendedRam: Map<String, Long>
) {
    val displayPackages: Set<String>
        get() = (ramMap.byPackage.filter { (_, kb) -> kb > 0 }.keys +
            psPackages +
            ramMap.activePackages +
            ramMap.cachedOnlyPackages)
            .filter { !RunningAppsProbe.isNativeProcessName(it) }
            .toSet()

    fun processState(pkg: String): ProcessState = when {
        pkg in ramMap.activePackages || pkg in psPackages -> ProcessState.ACTIVE
        pkg in ramMap.cachedOnlyPackages -> ProcessState.CACHED
        (ramMap.byPackage[pkg] ?: 0) > 0 -> ProcessState.CACHED
        else -> ProcessState.NONE
    }

    fun daemonProcessState(name: String): ProcessState = when {
        name in psActiveNames -> ProcessState.ACTIVE
        (extendedRam[name] ?: 0) > 0 -> ProcessState.CACHED
        else -> ProcessState.NONE
    }
}

object RunningAppsProbe {
    private val processRecordPkg = Regex("""ProcessRecord\{[^}]*\s+\d+:\s*([a-z][a-z0-9_.]+)/""")
    private val pkgFromPsArgs = Regex("""\b([a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]+)+)\b""")

    private val PS_SKIP = setOf("sh", "shizuku", "shizuku_server", "[sh]", "logd", "lmkd", "servicemanager")

    fun collectRunningSnapshot(): RunningSnapshot {
        val psPackages = collectPsPackages()
        val psActiveNames = collectPsAllNames()
        val dumpsysPackages = collectDumpsysProcessPackages()
        val mergedPs = psPackages + dumpsysPackages
        val ramMap = MemoryProbe.loadRamMap(mergedPs)
        val extendedRam = MemoryProbe.loadExtendedProcessRam()
        FileLogger.i(
            "running: ps=${psPackages.size} active=${psActiveNames.size} dumpsys=${dumpsysPackages.size} " +
                "ram=${ramMap.byPackage.size} extRam=${extendedRam.size}"
        )
        return RunningSnapshot(mergedPs, psActiveNames, ramMap, extendedRam)
    }

    fun collectDaemonNames(snapshot: RunningSnapshot, installed: Set<String>): Set<String> {
        val candidates = snapshot.psActiveNames +
            snapshot.extendedRam.filter { (_, kb) -> kb > 0 }.keys
        return candidates
            .filter { isDaemonCandidate(it, installed) }
            .filter { snapshot.daemonProcessState(it) != ProcessState.NONE }
            .toSet()
    }

    /** Не установленное APK и не «обычное» приложение — демон/нативный процесс. */
    fun isDaemonCandidate(name: String, installed: Set<String>): Boolean {
        if (name.isBlank() || name in PS_SKIP) return false
        if (name in installed && isLikelyPackageName(name) && !isNativeProcessName(name)) return false
        return true
    }

    fun collectPsPackages(): Set<String> {
        val ps = ShizukuShell.run("ps -A -o NAME").combined
        if (ps.isBlank()) return emptySet()
        return parsePs(ps, appPackagesOnly = true)
    }

    fun collectPsAllNames(): Set<String> {
        val ps = ShizukuShell.run("ps -A -o NAME").combined
        if (ps.isBlank()) return emptySet()
        return parsePs(ps, appPackagesOnly = false)
    }

    private fun collectDumpsysProcessPackages(): Set<String> {
        val out = ShizukuShell.run("dumpsys activity processes", timeoutSec = 30).combined
        if (out.isBlank()) return emptySet()
        return processRecordPkg.findAll(out)
            .map { normalizePackageName(it.groupValues[1]) }
            .filter { isLikelyPackageName(it) }
            .toSet()
    }

    internal fun normalizePackageName(name: String): String {
        var current = name.trim().substringBefore("/").substringBefore(":")
        while (true) {
            val dot = current.lastIndexOf('.')
            if (dot <= 0) break
            val segment = current.substring(dot + 1)
            if (segment.isEmpty() || !segment[0].isUpperCase()) break
            current = current.substring(0, dot)
        }
        return current
    }

    private val PACKAGE_ROOT_PREFIXES = listOf(
        "com.", "org.", "net.", "io.", "ru.", "de.", "uk.", "jp.", "fr.",
        "games.", "unity.", "su.", "mr.", "proton.", "dev.", "app.", "me.",
    )

    private val NATIVE_PROCESS_PREFIXES = listOf(
        "android.", "media.", "hidl.", "vendor.", "system.", "webview",
    )

    internal fun isNativeProcessName(name: String): Boolean {
        if (!name.contains('.')) return true
        if (name.contains('@') || name.contains(':')) return true
        return NATIVE_PROCESS_PREFIXES.any { name.startsWith(it) }
    }

    internal fun isLikelyPackageName(name: String): Boolean {
        if (!name.contains('.')) return false
        if (isNativeProcessName(name)) return false
        if (!PACKAGE_ROOT_PREFIXES.any { name.startsWith(it) }) return false
        val segments = name.split('.')
        if (segments.size < 3) return false
        return segments.all { s ->
            s.isNotEmpty() && s[0].isLowerCase() && s.all { c -> c.isLowerCase() || c.isDigit() || c == '_' }
        }
    }

    private fun parsePs(text: String, appPackagesOnly: Boolean): Set<String> {
        val result = linkedSetOf<String>()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("NAME")) continue
            val candidate = normalizePackageName(trimmed.split(Regex("""\s+""")).firstOrNull() ?: continue)
            if (candidate.isBlank() || candidate in PS_SKIP) continue
            if (appPackagesOnly) {
                if (isLikelyPackageName(candidate)) {
                    result.add(candidate)
                }
                pkgFromPsArgs.findAll(trimmed).forEach { match ->
                    val pkg = normalizePackageName(match.groupValues[1])
                    if (isLikelyPackageName(pkg) && pkg !in PS_SKIP) result.add(pkg)
                }
            } else {
                result.add(candidate)
                pkgFromPsArgs.findAll(trimmed).forEach { match ->
                    val pkg = normalizePackageName(match.groupValues[1])
                    if (pkg.isNotBlank() && pkg !in PS_SKIP) result.add(pkg)
                }
            }
        }
        return result
    }
}
