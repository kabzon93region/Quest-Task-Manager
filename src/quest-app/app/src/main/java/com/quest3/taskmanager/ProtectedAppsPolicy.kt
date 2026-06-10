package com.quest3.taskmanager

import android.content.Context
import org.json.JSONObject

class ProtectedAppsPolicy(context: Context, ownPackage: String) {
    private val killProtected = mutableSetOf<String>()
    private val protectedPrefixes = mutableListOf<String>()
    private val alwaysShowAsUser = mutableSetOf<String>()
    private val ownPackage: String = ownPackage

    init {
        val json = context.assets.open("protected_apps.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        root.optJSONArray("kill_protected")?.let { arr ->
            for (i in 0 until arr.length()) killProtected.add(arr.getString(i))
        }
        root.optJSONArray("protected_prefixes")?.let { arr ->
            for (i in 0 until arr.length()) protectedPrefixes.add(arr.getString(i))
        }
        root.optJSONArray("always_show_as_user")?.let { arr ->
            for (i in 0 until arr.length()) alwaysShowAsUser.add(arr.getString(i))
        }
        killProtected.add(ownPackage)
        alwaysShowAsUser.add(ownPackage)
    }

    fun isKillProtected(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == ownPackage) return true
        if (killProtected.contains(packageName)) return true
        for (prefix in protectedPrefixes) {
            if (prefix.endsWith(".") && packageName.startsWith(prefix)) return true
            if (!prefix.endsWith(".") && packageName == prefix) return true
        }
        return false
    }

    fun isSystemForFilter(packageName: String, appIsSystemFlag: Boolean): Boolean {
        if (alwaysShowAsUser.contains(packageName)) return false
        if (appIsSystemFlag) return true
        return matchesProtectedPrefix(packageName)
    }

    fun matchesProtectedPrefix(packageName: String): Boolean {
        for (prefix in protectedPrefixes) {
            if (prefix.endsWith(".") && packageName.startsWith(prefix)) return true
            if (!prefix.endsWith(".") && packageName == prefix) return true
        }
        return false
    }
}
