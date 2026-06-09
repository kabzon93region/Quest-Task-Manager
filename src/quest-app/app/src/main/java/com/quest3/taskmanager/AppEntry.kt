package com.quest3.taskmanager

import android.graphics.drawable.Drawable

enum class ProcessState { ACTIVE, CACHED, NONE }

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val processState: ProcessState,
    val diskSizeKb: Long?,
    val ramUsageKb: Long?,
    val runInBackgroundAllowed: Boolean?,
    val backgroundDataAllowed: Boolean?,
    val icon: Drawable?
) {
    val isRunning: Boolean get() = processState != ProcessState.NONE
}

enum class AppFilter { ALL, USER, SYSTEM }

fun AppEntry.matchesFilter(filter: AppFilter): Boolean = when (filter) {
    AppFilter.ALL -> true
    AppFilter.USER -> !isSystem
    AppFilter.SYSTEM -> isSystem
}

object MemoryFormat {
    fun formatKb(kb: Long?): String {
        if (kb == null || kb <= 0) return "—"
        return if (kb >= 1024) String.format("%.0f MB", kb / 1024.0) else "$kb KB"
    }
}
