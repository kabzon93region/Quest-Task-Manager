package com.quest3.taskmanager

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.quest3.taskmanager.databinding.ActivityMainBinding
import com.quest3.taskmanager.ui.AllAppsFragment
import com.quest3.taskmanager.ui.RunningTasksFragment
import com.quest3.taskmanager.ui.SettingsFragment
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            FileLogger.i("Shizuku permission granted")
        } else {
            FileLogger.w("Shizuku permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FileLogger.onAppLaunch(this)

        setSupportActionBar(binding.toolbar)

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> RunningTasksFragment()
                1 -> AllAppsFragment()
                2 -> SettingsFragment()
                else -> SettingsFragment()
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_running)
                1 -> getString(R.string.tab_apps)
                else -> getString(R.string.tab_settings)
            }
        }.attach()

        Shizuku.addRequestPermissionResultListener(permissionListener)
        requestShizukuIfNeeded()
    }

    private fun requestShizukuIfNeeded() {
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(0)
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }
}
