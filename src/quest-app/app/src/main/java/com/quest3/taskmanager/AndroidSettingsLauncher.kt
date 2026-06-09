package com.quest3.taskmanager

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

object AndroidSettingsLauncher {
    const val PACKAGE = "com.android.settings"
    private const val MAIN_ACTIVITY = "$PACKAGE.Settings"

    fun isInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    /**
     * Открывает нативные Android Settings (не Meta Quest Settings / panelapp.settings).
     * @return true если Activity запущена
     */
    fun open(context: Context): Boolean {
        if (!isInstalled(context)) {
            FileLogger.w("android settings missing: $PACKAGE")
            return false
        }

        val attempts = listOf(
            Intent().apply {
                component = ComponentName(PACKAGE, MAIN_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_SETTINGS).apply {
                setPackage(PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            innerSettingsIntent("DevelopmentSettingsDashboardActivity"),
            innerSettingsIntent("AccessibilitySettingsActivity"),
        )

        for (intent in attempts) {
            try {
                context.startActivity(intent)
                val target = intent.component?.className ?: intent.action
                FileLogger.i("opened android settings: $target")
                return true
            } catch (e: ActivityNotFoundException) {
                FileLogger.d("settings launch miss: ${intent.component} ${e.message}")
            } catch (e: SecurityException) {
                FileLogger.w("settings launch denied: ${intent.component} ${e.message}")
            }
        }

        FileLogger.e("android settings: all launch attempts failed")
        return false
    }

    private fun innerSettingsIntent(innerClass: String): Intent =
        Intent().apply {
            component = ComponentName(PACKAGE, "$PACKAGE.Settings\$$innerClass")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
