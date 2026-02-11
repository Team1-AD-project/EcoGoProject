package com.ecogo.ui.fragments

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoucherDetailFragmentTest {

    private val args = bundleOf("goodsId" to "test-goods")

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.text_name))
            assertNotNull(view.findViewById<View>(R.id.text_description))
            assertNotNull(view.findViewById<View>(R.id.text_cost))
            assertNotNull(view.findViewById<View>(R.id.card_voucher))
        }
    }

    @Test
    fun `redeem button is present`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<MaterialButton>(R.id.btn_redeem))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
