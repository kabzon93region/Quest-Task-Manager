package com.quest3.taskmanager

data class RunningSnapshot(
    val psPackages: Set<String>,
    val ramMap: MemoryProbe.RamMap
) {
    val activePackages: Set<String>
        get() = ramMap.activePackages

    val displayPackages: Set<String>
        get() = ramMap.activePackages + ramMap.cachedOnlyPackages

    fun processState(pkg: String): ProcessState = when {
        pkg in ramMap.activePackages -> ProcessState.ACTIVE
        pkg in ramMap.cachedOnlyPackages -> ProcessState.CACHED
        else -> ProcessState.NONE
    }
}

object RunningAppsProbe {
    fun collectRunningSnapshot(): RunningSnapshot {
        val psPackages = collectPsPackages()
        val ramMap = MemoryProbe.loadRamMap(psPackages)
        FileLogger.d("running: ps=${psPackages.size} active=${ramMap.activePackages.size} cached=${ramMap.cachedOnlyPackages.size}")
        return RunningSnapshot(psPackages, ramMap)
    }

    fun collectPsPackages(): Set<String> {
        val ps = ShizukuShell.run("ps -A -o NAME").combined
        if (ps.isBlank()) return emptySet()
        return parsePs(ps)
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

    private fun isLikelyPackageName(name: String): Boolean {
        if (!name.contains('.')) return false
        return name.split('.').all { s ->
            s.isNotEmpty() && s[0].isLowerCase() && s.all { c -> c.isLowerCase() || c.isDigit() || c == '_' }
        }
    }

    private fun parsePs(text: String): Set<String> =
        text.lineSequence().map { it.trim() }.filter { line ->
            line.startsWith("com.") || line.startsWith("org.") || line.startsWith("ru.") ||
                line.startsWith("su.") || line.startsWith("games.") || line.startsWith("proton.")
        }.map { normalizePackageName(it) }.filter { isLikelyPackageName(it) }.toSet()
}
