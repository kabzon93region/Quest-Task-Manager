package com.quest3.taskmanager.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.quest3.taskmanager.AndroidSettingsLauncher
import com.quest3.taskmanager.AppSettings
import com.quest3.taskmanager.CleanupForegroundService
import com.quest3.taskmanager.FileLogger
import com.quest3.taskmanager.R
import com.quest3.taskmanager.ShizukuShell
import com.quest3.taskmanager.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = AppSettings.prefs(requireContext())

        binding.switchNotification.isChecked = AppSettings.isNotificationEnabled(requireContext())
        binding.switchLogging.isChecked = prefs.getBoolean(AppSettings.KEY_LOGGING, true)
        binding.editLogPath.setText(
            prefs.getString(AppSettings.KEY_LOG_PATH, AppSettings.DEFAULT_LOG_PATH)
        )

        binding.switchNotification.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATIONS
                    )
                }
                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION, true).apply()
            } else {
                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION, false).apply()
            }
            AppSettings.syncNotificationService(requireContext())
        }

        binding.switchLogging.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AppSettings.KEY_LOGGING, checked).apply()
            FileLogger.setEnabled(checked)
        }

        binding.editLogPath.doAfterTextChanged { text ->
            val path = text?.toString()?.trim().orEmpty()
            if (path.isNotEmpty()) {
                prefs.edit().putString(AppSettings.KEY_LOG_PATH, path).apply()
                FileLogger.setLogPath(path)
            }
        }

        binding.btnOpenShizuku.setOnClickListener {
            val intent = requireContext().packageManager
                .getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) startActivity(intent)
        }

        binding.btnOpenAndroidSettings.setOnClickListener { openAndroidSettings() }

        updateShizukuStatus()
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
        binding.switchNotification.isChecked = AppSettings.isNotificationEnabled(requireContext())
    }

    private fun updateShizukuStatus() {
        binding.shizukuStatus.text = when {
            !ShizukuShell.isAvailable() -> getString(R.string.shizuku_down)
            !ShizukuShell.hasPermission() -> getString(R.string.shizuku_no_perm)
            else -> getString(R.string.shizuku_ok)
        }
    }

    private fun openAndroidSettings() {
        AndroidSettingsLauncher.openMainWithUi(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 200
    }
}
