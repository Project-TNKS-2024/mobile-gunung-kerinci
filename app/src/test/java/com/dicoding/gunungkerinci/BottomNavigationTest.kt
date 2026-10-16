package com.dicoding.gunungkerinci

import android.os.Bundle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BottomNavigationTest {
    private lateinit var nav: NavController
    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        // Exercise the real NavController and the app's tab handler without loading
        // screen fragments, GPS, or network services.
        activity = Robolectric.buildActivity(MainActivity::class.java).get()
        nav = NavController(ApplicationProvider.getApplicationContext())
        val screens = ScreenNavigator()
        nav.navigatorProvider.addNavigator(screens)
        nav.graph = NavGraph(NavGraphNavigator(nav.navigatorProvider)).apply {
            id = R.id.mobile_navigation
            listOf(R.id.navigation_beranda, R.id.navigation_jejak, R.id.navigation_tiket,
                R.id.navigation_profile).forEach { screenId ->
                addDestination(screens.createDestination().apply { id = screenId })
            }
            setStartDestination(R.id.navigation_beranda)
        }
    }

    private fun select(destination: Int) {
        MainActivity::class.java.getDeclaredMethod(
            "navigateToTab", NavController::class.java, Int::class.javaPrimitiveType, Bundle::class.java
        ).apply { isAccessible = true }.invoke(activity, nav, destination, null)
    }

    @Test
    fun `home after checkpoint returns to home repeatedly`() {
        repeat(3) {
            select(R.id.navigation_jejak)
            assertEquals(R.id.navigation_jejak, nav.currentDestination?.id)
            select(R.id.navigation_beranda)
            assertEquals(R.id.navigation_beranda, nav.currentDestination?.id)
            assertNull(nav.previousBackStackEntry)
        }
    }

    @Test
    fun `home after switching other tabs does not restore checkpoint`() {
        select(R.id.navigation_jejak)
        select(R.id.navigation_tiket)
        select(R.id.navigation_profile)
        select(R.id.navigation_beranda)
        assertEquals(R.id.navigation_beranda, nav.currentDestination?.id)
        select(R.id.navigation_jejak)
        assertEquals(R.id.navigation_jejak, nav.currentDestination?.id)
    }

    @Test
    fun `home after checkpoint opened from home card returns to home`() {
        nav.navigate(R.id.navigation_jejak)
        select(R.id.navigation_beranda)
        assertEquals(R.id.navigation_beranda, nav.currentDestination?.id)
        select(R.id.navigation_beranda)
        assertNull(nav.previousBackStackEntry)
    }

    @Navigator.Name("test_screen")
    private class ScreenNavigator : Navigator<NavDestination>() {
        override fun createDestination() = NavDestination(this)

        override fun navigate(entries: List<NavBackStackEntry>, navOptions: NavOptions?, navigatorExtras: Extras?) {
            entries.forEach { state.push(it) }
        }

        override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
            state.pop(popUpTo, savedState)
        }
    }
}
