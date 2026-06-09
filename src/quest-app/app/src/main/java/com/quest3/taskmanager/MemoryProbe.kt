package com.quest3.taskmanager

object MemoryProbe {
    private val totalPssRegex = Regex("""TOTAL\s+(\d+)""")

    data class RamMap(
        val byPackage: Map<String, Long>,
        val activePackages: Set<String>,
        val cachedOnlyPackages: Set<String>
    )

    fun loadRamMap(psPackages: Set<String>): RamMap {
        val byPackage = mutableMapOf<String, Long>()
        loadPsRss().forEach { (pkg, kb) -> byPackage[pkg] = (byPackage[pkg] ?: 0) + kb }
        parseMeminfo(ShizukuShell.run("dumpsys meminfo", timeoutSec = 45).combined)
            .forEach { (pkg, kb) -> byPackage[pkg] = maxOf(byPackage[pkg] ?: 0, kb) }

        val cachedOnly = byPackage.filter { (pkg, kb) -> kb > 0 && pkg !in psPackages }.keys
        return RamMap(byPackage, psPackages, cachedOnly)
    }

    private fun parseMeminfo(output: String): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        var currentPkg: String? = null
        var pss = 0L
        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("** MEMINFO in pid")) {
                if (currentPkg != null && pss > 0) {
                    result[currentPkg] = (result[currentPkg] ?: 0) + pss
                }
                currentPkg = Regex("""\[(.+?)\]""").find(trimmed)?.groupValues?.get(1)
                    ?.let { RunningAppsProbe.normalizePackageName(it) }
                pss = 0
                continue
            }
            if (currentPkg != null && trimmed.startsWith("TOTAL")) {
                totalPssRegex.find(trimmed)?.groupValues?.get(1)?.toLongOrNull()?.let { pss = it }
            }
        }
        if (currentPkg != null && pss > 0) {
            result[currentPkg] = (result[currentPkg] ?: 0) + pss
        }
        return result
    }

    private fun loadPsRss(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        ShizukuShell.run("ps -A -o NAME,RSS").combined.lines().forEach { line ->
            val parts = line.trim().split(Regex("""\s+"""))
            if (parts.size < 2) return@forEach
            val pkg = RunningAppsProbe.normalizePackageName(parts[0])
            if (!pkg.contains('.')) return@forEach
            val rss = parts.last().toLongOrNull() ?: return@forEach
            result[pkg] = (result[pkg] ?: 0) + rss
        }
        return result
    }
}
