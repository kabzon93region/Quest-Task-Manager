package com.quest3.taskmanager

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Только [PACKAGE] — без generic Settings intents (на Quest их перехватывает VrShell / Meta Settings).
 * Shell-команды оборачивают component в одинарные кавычки — иначе `$` в inner-классах ломается в sh.
 */
object AndroidSettingsLauncher {
    const val PACKAGE = "com.android.settings"

    fun isInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    fun open(context: Context): Boolean {
        if (!isInstalled(context)) {
            FileLogger.w("android settings missing: $PACKAGE")
            return false
        }

        val components = listOf(
            ".Settings\$SystemDashboardActivity",
            ".Settings\$ManageApplicationsActivity",
            ".Settings\$AccessibilitySettingsActivity",
        )

        for (suffix in components) {
            if (launchComponentShell(suffix, label = "main")) return true
        }

        for (suffix in components) {
            if (launchComponentIntent(context, suffix, label = "main")) return true
        }

        FileLogger.e("android settings: all launch attempts failed")
        return false
    }

    fun openAppDetails(context: Context, packageName: String): Boolean {
        if (!isInstalled(context)) {
            FileLogger.w("android settings missing for app details: $packageName")
            return false
        }

        val uri = "package:$packageName"
        val shellComponents = listOf(
            ".applications.InstalledAppDetailsTop",
            ".applications.InstalledAppDetails",
            ".Settings\$AppDetailsSettingsActivity",
        )
        for (suffix in shellComponents) {
            if (launchComponentShell(suffix, dataUri = uri, label = "app-details:$packageName")) return true
        }

        val intentComponents = listOf(
            "$PACKAGE.applications.InstalledAppDetailsTop",
            "$PACKAGE.applications.InstalledAppDetails",
            "$PACKAGE.Settings\$AppDetailsSettingsActivity",
            "$PACKAGE.Settings\$ManageApplicationsActivity",
        )
        for (className in intentComponents) {
            val intent = component(className).apply {
                data = Uri.fromParts("package", packageName, null)
                putExtra("pkg", packageName)
                putExtra(":settings:fragment_args_key", packageName)
                putExtra("package", packageName)
            }
            if (launchIntent(context, intent, "app-details:$packageName")) return true
        }

        FileLogger.e("app details launch failed: $packageName")
        return false
    }

    private fun launchComponentIntent(context: Context, classSuffix: String, label: String): Boolean {
        val className = if (classSuffix.startsWith(".")) PACKAGE + classSuffix else classSuffix
        return launchIntent(context, component(className), label)
    }

    private fun launchComponentShell(
        classSuffix: String,
        dataUri: String? = null,
        label: String
    ): Boolean {
        val normalized = if (classSuffix.startsWith(".")) classSuffix else ".$classSuffix"
        val component = "$PACKAGE$normalized"
        val dataPart = dataUri?.let { " -d '${it.replace("'", "")}'" }.orEmpty()
        val cmd = "am start -n '$component'$dataPart"
        if (!launchViaShell(cmd)) return false
        FileLogger.i("opened android settings [shell/$label]: $component")
        return true
    }

    private fun component(className: String): Intent =
        Intent().apply {
            val relative = when {
                className.startsWith(".") -> className
                className.startsWith("$PACKAGE.") -> "." + className.removePrefix("$PACKAGE.")
                else -> className
            }
            component = ComponentName(PACKAGE, relative)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun launchIntent(context: Context, intent: Intent, label: String): Boolean {
        return try {
            context.startActivity(intent)
            FileLogger.i("opened android settings [intent/$label]: ${intent.component?.className}")
            true
        } catch (e: ActivityNotFoundException) {
            FileLogger.d("settings miss [$label]: ${intent.component?.className} ${e.message}")
            false
        } catch (e: SecurityException) {
            FileLogger.w("settings denied [$label]: ${intent.component?.className} ${e.message}")
            false
        } catch (e: Exception) {
            FileLogger.w("settings error [$label]: ${intent.component?.className} ${e.message}")
            false
        }
    }

    private fun launchViaShell(command: String): Boolean {
        if (!ShizukuShell.isAvailable()) return false
        val result = ShizukuShell.run(command, timeoutSec = 10)
        if (result.exitCode == 0) return true
        FileLogger.d("settings shell fail exit=${result.exitCode}: $command")
        return false
    }
}
