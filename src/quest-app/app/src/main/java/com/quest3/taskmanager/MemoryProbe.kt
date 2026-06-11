package com.quest3.taskmanager

object MemoryProbe {
    private val totalPssRegex = Regex("""TOTAL\s+(\d+)""")
    private val summaryPssRegex = Regex("""^\s*([\d,]+)\s*K:\s+([a-z][a-z0-9_.]+)""")

    data class RamMap(
        val byPackage: Map<String, Long>,
        val activePackages: Set<String>,
        val cachedOnlyPackages: Set<String>
    )

    fun loadRamMap(psPackages: Set<String>): RamMap {
        val byPackage = mutableMapOf<String, Long>()
        loadPsRss().forEach { (pkg, kb) -> byPackage[pkg] = (byPackage[pkg] ?: 0) + kb }
        val meminfo = ShizukuShell.run("dumpsys meminfo", timeoutSec = 45).combined
        parseMeminfoDetails(meminfo).forEach { (pkg, kb) ->
            byPackage[pkg] = maxOf(byPackage[pkg] ?: 0, kb)
        }
        parseMeminfoSummary(meminfo).forEach { (pkg, kb) ->
            byPackage[pkg] = maxOf(byPackage[pkg] ?: 0, kb)
        }

        val active = psPackages.toMutableSet()
        active.addAll(byPackage.filter { (pkg, kb) -> kb > 0 && pkg in psPackages }.keys)

        val cachedOnly = byPackage.filter { (pkg, kb) ->
            kb > 0 && pkg !in psPackages
        }.keys

        return RamMap(byPackage, active, cachedOnly)
    }

    private fun parseMeminfoDetails(output: String): Map<String, Long> {
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
                    ?.takeIf { RunningAppsProbe.isLikelyPackageName(it) }
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

    private fun parseMeminfoSummary(output: String): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        var inSummary = false
        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Total PSS by process")) {
                inSummary = true
                continue
            }
            if (inSummary) {
                if (trimmed.isBlank() || trimmed.startsWith("Total PSS by OOM")) break
                val match = summaryPssRegex.find(trimmed) ?: continue
                val kb = match.groupValues[1].replace(",", "").toLongOrNull() ?: continue
                val pkg = RunningAppsProbe.normalizePackageName(match.groupValues[2])
                if (RunningAppsProbe.isLikelyPackageName(pkg)) {
                    result[pkg] = (result[pkg] ?: 0) + kb
                }
            }
        }
        return result
    }

    private fun loadPsRss(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        ShizukuShell.run("ps -A -o NAME,RSS").combined.lines().forEach { line ->
            val parts = line.trim().split(Regex("""\s+"""))
            if (parts.size < 2) return@forEach
            val pkg = RunningAppsProbe.normalizePackageName(parts[0])
            if (!RunningAppsProbe.isLikelyPackageName(pkg)) return@forEach
            val rss = parts.last().toLongOrNull() ?: return@forEach
            result[pkg] = (result[pkg] ?: 0) + rss
        }
        return result
    }

    /** RAM по всем именам процессов (включая нативные демоны). */
    fun loadExtendedProcessRam(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        ShizukuShell.run("ps -A -o NAME,RSS").combined.lines().forEach { line ->
            val parts = line.trim().split(Regex("""\s+"""))
            if (parts.size < 2) return@forEach
            val name = RunningAppsProbe.normalizePackageName(parts[0])
            if (name.isBlank()) return@forEach
            val rss = parts.last().toLongOrNull() ?: return@forEach
            result[name] = (result[name] ?: 0) + rss
        }
        val meminfo = ShizukuShell.run("dumpsys meminfo", timeoutSec = 45).combined
        parseMeminfoSummaryAll(meminfo).forEach { (name, kb) ->
            result[name] = maxOf(result[name] ?: 0, kb)
        }
        return result
    }

    private fun parseMeminfoSummaryAll(output: String): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        var inSummary = false
        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Total PSS by process")) {
                inSummary = true
                continue
            }
            if (inSummary) {
                if (trimmed.isBlank() || trimmed.startsWith("Total PSS by OOM")) break
                val match = summaryPssRegex.find(trimmed) ?: continue
                val kb = match.groupValues[1].replace(",", "").toLongOrNull() ?: continue
                val name = RunningAppsProbe.normalizePackageName(match.groupValues[2])
                if (name.isNotBlank()) {
                    result[name] = (result[name] ?: 0) + kb
                }
            }
        }
        return result
    }
}
