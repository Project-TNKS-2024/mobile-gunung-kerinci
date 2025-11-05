package com.dicoding.gunungkerinci.Profile

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ViewPagerAdapterProfile(supportFragmentManager: FragmentManager) : FragmentPagerAdapter
    (supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val FragmentList = ArrayList<Fragment>()
    private val FragmentTittleList = ArrayList<String>()

    override fun getCount(): Int {
        return FragmentList.size
    }

    override fun getItem(position: Int): Fragment {
        return FragmentList[position]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return FragmentTittleList[position]
    }

    fun addFragment(fragment: Fragment, tittle: String) {
        FragmentList.add(fragment)
        FragmentTittleList.add(tittle)
    }

}