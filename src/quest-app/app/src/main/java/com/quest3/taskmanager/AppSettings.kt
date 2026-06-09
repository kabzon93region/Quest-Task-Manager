package com.quest3.taskmanager

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    const val PREFS = "qtaskmgr_settings"
    const val KEY_LOGGING = "logging_enabled"
    const val KEY_LOG_PATH = "log_path"
    const val KEY_NOTIFICATION = "notification_enabled"
    const val DEFAULT_LOG_PATH = "/sdcard/Download/QTaskManager.log"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isNotificationEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATION, false)
}
