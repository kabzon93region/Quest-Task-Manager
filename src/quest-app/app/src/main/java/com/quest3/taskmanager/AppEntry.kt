package com.quest3.taskmanager

import android.graphics.drawable.Drawable

enum class ProcessState { ACTIVE, CACHED, NONE }

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isDaemon: Boolean = false,
    val processState: ProcessState,
    val diskSizeKb: Long?,
    val ramUsageKb: Long?,
    val runInBackgroundAllowed: Boolean?,
    val backgroundDataAllowed: Boolean?,
    val icon: Drawable?
) {
    val isRunning: Boolean get() = processState != ProcessState.NONE
}

enum class AppFilter { ALL, USER, SYSTEM, DAEMON }

fun AppEntry.matchesFilter(filter: AppFilter): Boolean = when (filter) {
    AppFilter.ALL -> !isDaemon
    AppFilter.USER -> !isSystem && !isDaemon
    AppFilter.SYSTEM -> isSystem && !isDaemon
    AppFilter.DAEMON -> isDaemon
}

fun AppEntry.matchesSearch(query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return label.lowercase().contains(q) || packageName.lowercase().contains(q)
}

fun List<AppEntry>.filtered(filter: AppFilter, search: String): List<AppEntry> =
    filter { it.matchesFilter(filter) && it.matchesSearch(search) }

object MemoryFormat {
    fun formatKb(kb: Long?): String {
        if (kb == null || kb <= 0) return "—"
        return if (kb >= 1024) String.format("%.0f MB", kb / 1024.0) else "$kb KB"
    }
}
