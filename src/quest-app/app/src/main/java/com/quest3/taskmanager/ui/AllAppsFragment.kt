package com.quest3.taskmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.quest3.taskmanager.AndroidSettingsLauncher
import com.quest3.taskmanager.AppEntry
import com.quest3.taskmanager.AppFilter
import com.quest3.taskmanager.AppListCache
import com.quest3.taskmanager.AppRepository
import com.quest3.taskmanager.BackgroundPolicy
import com.quest3.taskmanager.PolicyContext
import com.quest3.taskmanager.R
import com.quest3.taskmanager.ShizukuShell
import com.quest3.taskmanager.databinding.FragmentAllAppsBinding
import com.quest3.taskmanager.matchesFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllAppsFragment : Fragment() {
    private var _binding: FragmentAllAppsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var adapter: AppListAdapter
    private var allItems = listOf<AppEntry>()
    private var filter = AppFilter.USER
    private var policyCtx: PolicyContext? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAllAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = AppRepository(requireContext())
        adapter = AppListAdapter(
            mode = AppListMode.ALL_APPS,
            onItemClick = { entry -> openAppDetails(entry) },
            onSelectionChanged = {},
            onRunBgChanged = { entry, allowed -> setRunBg(entry, allowed) },
            onBgDataChanged = { entry, allowed -> setBgData(entry, allowed) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.chipAll.setOnClickListener { applyFilter(AppFilter.ALL) }
        binding.chipUser.setOnClickListener { applyFilter(AppFilter.USER) }
        binding.chipSystem.setOnClickListener { applyFilter(AppFilter.SYSTEM) }
        binding.btnRefresh.setOnClickListener { refresh() }

        filter = AppFilter.USER
        updateFilterChips(filter)
    }

    fun setLoading(visible: Boolean) {
        if (_binding == null) return
        binding.progress.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun displayEntries(items: List<AppEntry>) {
        allItems = items
        applyFilter(filter)
    }

    private fun updateFilterChips(f: AppFilter) {
        binding.chipAll.isChecked = f == AppFilter.ALL
        binding.chipUser.isChecked = f == AppFilter.USER
        binding.chipSystem.isChecked = f == AppFilter.SYSTEM
    }

    private fun openAppDetails(entry: AppEntry) {
        AndroidSettingsLauncher.openAppDetailsWithUi(requireContext(), entry.packageName)
    }

    private fun applyFilter(f: AppFilter) {
        filter = f
        updateFilterChips(f)
        adapter.submitList(allItems.filter { it.matchesFilter(filter) })
    }

    fun refresh() {
        if (!ShizukuShell.isAvailable() || !ShizukuShell.hasPermission()) {
            Toast.makeText(requireContext(), R.string.error_shizuku, Toast.LENGTH_SHORT).show()
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    policyCtx = BackgroundPolicy.loadContext()
                    repository.loadAllEntries()
                }
                allItems = items
                applyFilter(filter)
                withContext(Dispatchers.IO) {
                    AppListCache.saveAllApps(requireContext(), items)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Error", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setRunBg(entry: AppEntry, allowed: Boolean) {
        val previous = entry.runInBackgroundAllowed
        updateLocal(entry.packageName, runAllowed = allowed, dataAllowed = null)
        lifecycleScope.launch {
            val ok = BackgroundPolicy.setRunInBackgroundAllowed(entry.packageName, allowed)
            if (!ok) {
                updateLocal(entry.packageName, runAllowed = previous, dataAllowed = null)
                Toast.makeText(requireContext(), R.string.error_shizuku, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setBgData(entry: AppEntry, allowed: Boolean) {
        val previous = entry.backgroundDataAllowed
        updateLocal(entry.packageName, runAllowed = null, dataAllowed = allowed)
        lifecycleScope.launch {
            val ctx = policyCtx ?: BackgroundPolicy.loadContext().also { policyCtx = it }
            val ok = BackgroundPolicy.setBackgroundDataAllowed(entry.packageName, allowed, ctx)
            if (!ok) {
                updateLocal(entry.packageName, runAllowed = null, dataAllowed = previous)
                Toast.makeText(requireContext(), R.string.error_shizuku, Toast.LENGTH_SHORT).show()
            } else {
                policyCtx = BackgroundPolicy.loadContext()
            }
        }
    }

    private fun updateLocal(packageName: String, runAllowed: Boolean?, dataAllowed: Boolean?) {
        allItems = allItems.map { item ->
            if (item.packageName != packageName) item
            else item.copy(
                runInBackgroundAllowed = runAllowed ?: item.runInBackgroundAllowed,
                backgroundDataAllowed = dataAllowed ?: item.backgroundDataAllowed
            )
        }
        adapter.updatePolicy(packageName, runAllowed, dataAllowed)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
