package com.dicoding.gunungkerinci

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.dicoding.gunungkerinci.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val iconMap = mapOf(
        R.id.navigation_beranda to Pair(R.drawable.beranda, R.drawable.beranda_fill),

        R.id.navigation_tiket to Pair(R.drawable.ticket, R.drawable.ticket_fill),

        //R.id.navigation_jejak to Pair(R.drawable.vr, R.drawable.vr_fill),

        //R.id.navigation_laporan to Pair(R.drawable.laporan, R.drawable.laporan_fill),

        R.id.navigation_profile to Pair(R.drawable.profile, R.drawable.profile_fill)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView : BottomNavigationView = binding.bottomNavigationView
        val navController = findNavController(R.id.nav_host_fragment)

        navView.setupWithNavController(navController)

        handleOAuthDeepLink(intent)

        // CEK JIKA KEMBALI DARI PROFILE DATA → BUKA PROFILE FRAGMENT
        // FIX: terima data dari biodata
        if (intent.getBooleanExtra("from_biodata", false)) {

            val bundle = Bundle().apply {
                putBoolean("from_biodata", true)
                putString("nama_user", intent.getStringExtra("nama_user") ?: "")
            }

            navView.selectedItemId = R.id.navigation_profile
            navController.navigate(R.id.navigation_profile, bundle)
        }

        when (intent.getStringExtra("open_fragment")) {
            "profile" -> {
                navView.selectedItemId = R.id.navigation_profile
                navController.navigate(R.id.navigation_profile)
            }
        }

        navView.setOnNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.navigation_beranda -> {
                    navController.navigate(R.id.navigation_beranda)
                    updateIcon(menuItem.itemId)
                    true
                }
                R.id.navigation_tiket -> {
                    navController.navigate(R.id.navigation_tiket)
                    updateIcon(menuItem.itemId)
                    true
                }
                /*
                R.id.navigation_jejak -> {
                    navController.navigate(R.id.navigation_jejak)
                    updateIcon(menuItem.itemId)
                    true
                }
                R.id.navigation_laporan -> {
                    navController.navigate(R.id.navigation_laporan)
                    updateIcon(menuItem.itemId)
                    true
                }
                 */
                R.id.navigation_profile -> {
                    navController.navigate(R.id.navigation_profile)
                    updateIcon(menuItem.itemId)
                    true
                } else -> false
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthDeepLink(intent)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateIcon(activeId: Int) {
        iconMap.forEach { (id, iconPair) ->
            val menuItem = binding.bottomNavigationView.menu.findItem(id)
            menuItem.icon = getDrawable(
                if (id == activeId) iconPair.second else iconPair.first
            )
        }
    }

    private fun handleOAuthDeepLink(intent: Intent) {
        val data = intent?.data ?: return

        Log.d("OAUTH_DEBUG", "Deep link diterima: $data")

        if (data.scheme == "gunungkerinci" && data.host == "oauth") {

            val token = data.getQueryParameter("token")
            val isNewUser = data.getQueryParameter("is_new_user")

            if (!token.isNullOrEmpty()) {
                val pref = getSharedPreferences("auth", MODE_PRIVATE)
                pref.edit().putString("token", token).apply()

                val navController = findNavController(R.id.nav_host_fragment)
                val navView = binding.bottomNavigationView

                if (isNewUser == "true") {
                    navView.selectedItemId = R.id.navigation_profile
                    navController.navigate(R.id.navigation_profile)
                } else {
                    navView.selectedItemId = R.id.navigation_beranda
                    navController.navigate(R.id.navigation_beranda)
                }
            }
        }
    }


}