package com.dicoding.gunungkerinci

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.findNavController
import com.dicoding.gunungkerinci.databinding.ActivityMainBinding
import com.dicoding.gunungkerinci.ui.common.CustomBottomBar
import com.dicoding.gunungkerinci.ui.common.NavTab

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedTab by mutableStateOf(NavTab.BERANDA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment)

        // Bottom bar custom (Compose) — desain disamakan dengan repo vibe-coding.
        binding.composeBottomNav.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CustomBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        when (tab) {
                            NavTab.BERANDA -> navController.navigate(R.id.navigation_beranda)
                            NavTab.TIKET -> navController.navigate(R.id.navigation_tiket)
                            NavTab.CHECK_POINT -> navController.navigate(R.id.navigation_jejak)
                            NavTab.AKUN -> navController.navigate(R.id.navigation_profile)
                            // Tab SOS belum bernavigasi (fitur SOS belum dikerjakan).
                            NavTab.SOS -> Unit
                        }
                    }
                )
            }
        }

        // Jaga tab terpilih tetap sinkron saat fragment berubah (mis. dari deep link).
        navController.addOnDestinationChangedListener { _, destination, _ ->
            selectedTab = when (destination.id) {
                R.id.navigation_beranda -> NavTab.BERANDA
                R.id.navigation_tiket -> NavTab.TIKET
                R.id.navigation_jejak -> NavTab.CHECK_POINT
                R.id.navigation_profile -> NavTab.AKUN
                else -> selectedTab
            }
        }

        handleOAuthDeepLink(intent)

        // CEK JIKA KEMBALI DARI PROFILE DATA → BUKA PROFILE FRAGMENT
        if (intent.getBooleanExtra("from_biodata", false)) {
            val bundle = Bundle().apply {
                putBoolean("from_biodata", true)
                putString("nama_user", intent.getStringExtra("nama_user") ?: "")
            }
            selectedTab = NavTab.AKUN
            navController.navigate(R.id.navigation_profile, bundle)
        }

        when (intent.getStringExtra("open_fragment")) {
            "profile" -> {
                selectedTab = NavTab.AKUN
                navController.navigate(R.id.navigation_profile)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthDeepLink(intent)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun handleOAuthDeepLink(intent: Intent) {
        val data = intent.data ?: return

        Log.d("OAUTH_DEBUG", "Deep link diterima: $data")

        if (data.scheme == "gunungkerinci" && data.host == "oauth") {

            val token = data.getQueryParameter("token")
            val isNewUser = data.getQueryParameter("is_new_user")

            if (!token.isNullOrEmpty()) {
                val pref = getSharedPreferences("auth", MODE_PRIVATE)
                pref.edit().putString("token", token).apply()

                val navController = findNavController(R.id.nav_host_fragment)

                if (isNewUser == "true") {
                    selectedTab = NavTab.AKUN
                    navController.navigate(R.id.navigation_profile)
                } else {
                    selectedTab = NavTab.BERANDA
                    navController.navigate(R.id.navigation_beranda)
                }
            }
        }
    }
}
