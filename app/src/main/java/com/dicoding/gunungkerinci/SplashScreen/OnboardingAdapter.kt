package com.dicoding.gunungkerinci.SplashScreen

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingAdapter (
    fa: FragmentActivity,
    private val items: List<OnboardingItem>
) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment {
        val  item = items[position]
        return OnboardingFragment.newInstance(item.title, item.description, item.imageResId)
    }
}