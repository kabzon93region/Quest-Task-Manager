package com.quest3.taskmanager

import android.content.Context
import android.widget.Toast
import com.quest3.taskmanager.ui.AllAppsFragment
import com.quest3.taskmanager.ui.RunningTasksFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ListTabsBootstrap {
    suspend fun bootstrap(
        context: Context,
        running: RunningTasksFragment?,
        allApps: AllAppsFragment?
    ) {
        if (running == null || allApps == null) return

        if (AppListCache.hasCache(context)) {
            loadFromCache(context, running, allApps)
            return
        }

        if (!ShizukuShell.isAvailable() || !ShizukuShell.hasPermission()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.error_shizuku, Toast.LENGTH_SHORT).show()
            }
            return
        }

        loadFreshParallel(context, running, allApps)
    }

    private suspend fun loadFromCache(
        context: Context,
        running: RunningTasksFragment,
        allApps: AllAppsFragment
    ) = coroutineScope {
        running.setLoading(true)
        allApps.setLoading(true)

        val runningDeferred = async(Dispatchers.IO) { AppListCache.loadRunning(context) }
        val allDeferred = async(Dispatchers.IO) { AppListCache.loadAllApps(context) }

        launch {
            try {
                val items = runningDeferred.await().orEmpty()
                withContext(Dispatchers.Main) {
                    running.displayEntries(items)
                }
                FileLogger.d("lists: cache running=${items.size}")
            } finally {
                withContext(Dispatchers.Main) {
                    running.setLoading(false)
                }
            }
        }

        launch {
            try {
                val items = allDeferred.await().orEmpty()
                withContext(Dispatchers.Main) {
                    allApps.displayEntries(items)
                }
                FileLogger.d("lists: cache all=${items.size}")
            } finally {
                withContext(Dispatchers.Main) {
                    allApps.setLoading(false)
                }
            }
        }
    }

    private suspend fun loadFreshParallel(
        context: Context,
        running: RunningTasksFragment,
        allApps: AllAppsFragment
    ) = coroutineScope {
        running.setLoading(true)
        allApps.setLoading(true)

        val repo = AppRepository(context)
        val runningDeferred = async(Dispatchers.IO) { repo.loadRunningEntries() }
        val allDeferred = async(Dispatchers.IO) { repo.loadAllEntries() }

        launch {
            try {
                val items = runningDeferred.await()
                withContext(Dispatchers.IO) {
                    AppListCache.saveRunning(context, items)
                }
                withContext(Dispatchers.Main) {
                    running.displayEntries(items)
                }
                FileLogger.i("lists: fresh running=${items.size}")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    running.setLoading(false)
                }
            }
        }

        launch {
            try {
                val items = allDeferred.await()
                withContext(Dispatchers.IO) {
                    AppListCache.saveAllApps(context, items)
                }
                withContext(Dispatchers.Main) {
                    allApps.displayEntries(items)
                }
                FileLogger.i("lists: fresh all=${items.size}")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    allApps.setLoading(false)
                }
            }
        }
    }
}
