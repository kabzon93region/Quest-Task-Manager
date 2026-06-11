package com.quest3.taskmanager

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Кэш списков между запусками (без иконок — подгружаются из PM). */
object AppListCache {
    private const val RUNNING_FILE = "cache_running.json"
    private const val ALL_APPS_FILE = "cache_all_apps.json"

    fun hasCache(context: Context): Boolean =
        cacheFile(context, RUNNING_FILE).exists() && cacheFile(context, ALL_APPS_FILE).exists()

    fun saveRunning(context: Context, entries: List<AppEntry>) =
        write(context, RUNNING_FILE, entries)

    fun saveAllApps(context: Context, entries: List<AppEntry>) =
        write(context, ALL_APPS_FILE, entries)

    fun loadRunning(context: Context): List<AppEntry>? =
        read(context, RUNNING_FILE)

    fun loadAllApps(context: Context): List<AppEntry>? =
        read(context, ALL_APPS_FILE)

    private fun cacheFile(context: Context, name: String): File =
        File(context.filesDir, name)

    private fun write(context: Context, name: String, entries: List<AppEntry>) {
        val arr = JSONArray()
        for (e in entries) {
            arr.put(
                JSONObject()
                    .put("pkg", e.packageName)
                    .put("label", e.label)
                    .put("system", e.isSystem)
                    .put("daemon", e.isDaemon)
                    .put("state", e.processState.name)
                    .put("disk", e.diskSizeKb ?: JSONObject.NULL)
                    .put("ram", e.ramUsageKb ?: JSONObject.NULL)
                    .put("runBg", e.runInBackgroundAllowed ?: JSONObject.NULL)
                    .put("bgData", e.backgroundDataAllowed ?: JSONObject.NULL)
            )
        }
        cacheFile(context, name).writeText(arr.toString())
    }

    private fun read(context: Context, name: String): List<AppEntry>? {
        val file = cacheFile(context, name)
        if (!file.exists()) return null
        return try {
            val arr = JSONArray(file.readText())
            val pm = context.packageManager
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(o.toEntry(pm))
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun JSONObject.toEntry(pm: PackageManager): AppEntry {
        val pkg = getString("pkg")
        val icon = try {
            pm.getApplicationIcon(pkg)
        } catch (_: PackageManager.NameNotFoundException) {
            pm.getDefaultActivityIcon()
        }
        return AppEntry(
            packageName = pkg,
            label = getString("label"),
            isSystem = getBoolean("system"),
            isDaemon = optBoolean("daemon", false),
            processState = ProcessState.valueOf(getString("state")),
            diskSizeKb = optLongOrNull("disk"),
            ramUsageKb = optLongOrNull("ram"),
            runInBackgroundAllowed = optBooleanOrNull("runBg"),
            backgroundDataAllowed = optBooleanOrNull("bgData"),
            icon = icon
        )
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? =
        if (isNull(key)) null else optBoolean(key)
}
