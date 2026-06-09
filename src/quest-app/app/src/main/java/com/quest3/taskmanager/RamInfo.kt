package com.quest3.taskmanager

import android.app.ActivityManager
import android.content.Context
import java.util.Locale
import kotlin.math.roundToInt

object RamInfo {
    fun formatCompactMb(context: Context): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val freeMb = (info.availMem / 1_048_576.0).roundToInt()
        val totalMb = (info.totalMem / 1_048_576.0).roundToInt()
        return if (totalMb >= 1024) {
            String.format(Locale.US, "%.1f/%.1f GB", freeMb / 1024.0, totalMb / 1024.0)
        } else {
            "$freeMb/$totalMb MB"
        }
    }
}
