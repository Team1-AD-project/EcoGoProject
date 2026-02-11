package com.ecogo.ui.fragments

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoucherCouponsFragmentTest {

    private fun launchFragment(): androidx.fragment.app.testing.FragmentScenario<VoucherCouponsFragment> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.voucherFragment)

        val scenario = launchFragmentInContainer<VoucherCouponsFragment>(
            themeResId = R.style.Theme_EcoGo,
            initialState = Lifecycle.State.CREATED
        )
        scenario.onFragment { fragment ->
            val contentView = fragment.requireActivity().findViewById<View>(android.R.id.content)
            Navigation.setViewNavController(contentView, navController)
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        return scenario
    }

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragment()
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `recycler view is present`() {
        val scenario = launchFragment()
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<RecyclerView>(R.id.recycler_coupons))
        }
    }

    @Test
    fun `filter chips are present`() {
        val scenario = launchFragment()
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.chip_group_filters))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragment()
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }
}
