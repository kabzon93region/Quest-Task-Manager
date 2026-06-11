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
import com.quest3.taskmanager.AppRepository
import com.quest3.taskmanager.KillResult
import com.quest3.taskmanager.R
import com.quest3.taskmanager.ShizukuShell
import com.quest3.taskmanager.databinding.FragmentRunningTasksBinding
import com.quest3.taskmanager.matchesFilter
import kotlinx.coroutines.launch

class RunningTasksFragment : Fragment() {
    private var _binding: FragmentRunningTasksBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var adapter: AppListAdapter
    private var allItems = listOf<AppEntry>()
    private var filter = AppFilter.USER

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRunningTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = AppRepository(requireContext())
        adapter = AppListAdapter(
            mode = AppListMode.RUNNING,
            onItemClick = { entry -> openAppDetails(entry) },
            onSelectionChanged = { selected ->
                binding.statusText.text = getString(R.string.selected_count, selected.size)
            },
            onRunBgChanged = { _, _ -> },
            onBgDataChanged = { _, _ -> }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.chipAll.setOnClickListener { applyFilter(AppFilter.ALL) }
        binding.chipUser.setOnClickListener { applyFilter(AppFilter.USER) }
        binding.chipSystem.setOnClickListener { applyFilter(AppFilter.SYSTEM) }
        binding.btnRefresh.setOnClickListener { refresh() }
        binding.btnKillAll.setOnClickListener { killAll() }
        binding.btnKillSelected.setOnClickListener { killSelected() }
        binding.btnKillRules.setOnClickListener { killByRules() }

        filter = AppFilter.USER
        updateFilterChips(filter)
        refresh()
    }

    private fun updateFilterChips(f: AppFilter) {
        binding.chipAll.isChecked = f == AppFilter.ALL
        binding.chipUser.isChecked = f == AppFilter.USER
        binding.chipSystem.isChecked = f == AppFilter.SYSTEM
    }

    private fun killableItems(): List<AppEntry> =
        allItems.filter { !repository.isKillProtected(it.packageName) }

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
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                allItems = repository.loadRunningEntries()
                applyFilter(filter)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Error", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun openAppDetails(entry: AppEntry) {
        AndroidSettingsLauncher.openAppDetailsWithUi(requireContext(), entry.packageName)
    }

    private fun killPackages(pkgs: Collection<String>) {
        if (pkgs.isEmpty()) return
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                showKillResult(repository.killPackages(pkgs))
                adapter.clearSelection()
                refresh()
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun showKillResult(result: KillResult) {
        val msg = when {
            result.skippedProtected > 0 && result.failed > 0 ->
                getString(R.string.killed_mixed, result.killed, result.skippedProtected, result.failed)
            result.skippedProtected > 0 ->
                getString(R.string.killed_with_skipped, result.killed, result.skippedProtected)
            result.failed > 0 ->
                getString(R.string.killed_with_failed, result.killed, result.failed)
            else ->
                getString(R.string.killed_count, result.killed)
        }
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    private fun killAll() = killPackages(killableItems().map { it.packageName })
    private fun killSelected() = killPackages(adapter.getSelectedPackages())

    private fun killByRules() {
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val pkgs = killableItems().map { it.packageName }
                showKillResult(repository.killByRules(pkgs))
                refresh()
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
