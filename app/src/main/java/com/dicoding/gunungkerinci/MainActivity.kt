package com.dicoding.gunungkerinci

import android.annotation.SuppressLint
import android.os.Bundle
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

        R.id.navigation_jejak to Pair(R.drawable.vr, R.drawable.vr_fill),

        R.id.navigation_laporan to Pair(R.drawable.laporan, R.drawable.laporan_fill),

        R.id.navigation_profile to Pair(R.drawable.profile, R.drawable.profile_fill)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView : BottomNavigationView = binding.bottomNavigationView
        val navController = findNavController(R.id.nav_host_fragment)

        navView.setupWithNavController(navController)

        val openFragment = intent.getStringExtra("open_fragment")
        if (openFragment == "profile") {
            navView.selectedItemId = R.id.navigation_profile
            navController.navigate(R.id.navigation_profile)
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
                R.id.navigation_profile -> {
                    navController.navigate(R.id.navigation_profile)
                    updateIcon(menuItem.itemId)
                    true
                } else -> false
            }
        }
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
    }