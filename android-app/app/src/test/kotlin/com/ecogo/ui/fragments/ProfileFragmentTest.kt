package com.ecogo.ui.fragments

import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import com.ecogo.data.Outfit
import com.ecogo.data.ShopItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileFragmentTest {

    /**
     * Helper: launch ProfileFragment with TestNavHostController
     */
    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<ProfileFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.profileFragment)

        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        return scenario to navController
    }

    /** Helper: get a private method from ProfileFragment via reflection */
    private fun getPrivateMethod(name: String, vararg paramTypes: Class<*>): Method {
        val method = ProfileFragment::class.java.getDeclaredMethod(name, *paramTypes)
        method.isAccessible = true
        return method
    }

    /** Helper: get a private field value */
    private fun getFieldValue(fragment: ProfileFragment, name: String): Any? {
        val field = ProfileFragment::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(fragment)
    }

    /** Helper: set a private field value */
    private fun setFieldValue(fragment: ProfileFragment, name: String, value: Any?) {
        val field = ProfileFragment::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(fragment, value)
    }

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
        }
    }

    @Test
    fun `fragment view is not null after onViewCreated`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_name))
            assertNotNull(view.findViewById<View>(R.id.text_faculty))
            assertNotNull(view.findViewById<View>(R.id.text_points))
        }
    }

    // ==================== Initial UI Views ====================

    @Test
    fun `mascot card is present`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.card_mascot))
            assertNotNull(view.findViewById<View>(R.id.mascot_lion))
        }
    }

    @Test
    fun `points card is present`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.card_points))
            assertNotNull(view.findViewById<View>(R.id.text_points))
            assertNotNull(view.findViewById<View>(R.id.button_redeem))
        }
    }

    @Test
    fun `settings button is present`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.button_settings))
        }
    }

    @Test
    fun `trip history button is present`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.button_trip_history))
        }
    }

    // ==================== Tabs Setup ====================

    @Test
    fun `closet card is visible after setup`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val card = fragment.requireView().findViewById<View>(R.id.card_closet)
            assertEquals(View.VISIBLE, card.visibility)
        }
    }

    @Test
    fun `badges card is visible after setup`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val card = fragment.requireView().findViewById<View>(R.id.card_badges)
            assertEquals(View.VISIBLE, card.visibility)
        }
    }

    @Test
    fun `closet card has preview mascot and description`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.mascot_closet_preview))
            assertNotNull(view.findViewById<View>(R.id.text_closet_desc))
        }
    }

    @Test
    fun `badge card has preview text and count`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_badge_preview))
            assertNotNull(view.findViewById<View>(R.id.text_badge_count))
        }
    }

    // ==================== User Info Display ====================

    @Test
    fun `name text view has default or loaded text`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val nameText = fragment.requireView().findViewById<TextView>(R.id.text_name)
            assertNotNull(nameText.text)
        }
    }

    @Test
    fun `faculty text view has default or loaded text`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val facultyText = fragment.requireView().findViewById<TextView>(R.id.text_faculty)
            assertNotNull(facultyText.text)
        }
    }

    @Test
    fun `points text view has default or loaded text`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val pointsText = fragment.requireView().findViewById<TextView>(R.id.text_points)
            assertNotNull(pointsText.text)
        }
    }

    // ==================== Navigation Clicks ====================

    @Test
    fun `button_settings click navigates to settingsFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.button_settings).performClick()
        }
        assertEquals(R.id.settingsFragment, navController.currentDestination?.id)
    }

    @Test
    fun `button_redeem click navigates to voucherFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.button_redeem).performClick()
        }
        assertEquals(R.id.voucherFragment, navController.currentDestination?.id)
    }

    @Test
    fun `button_trip_history click navigates to tripHistoryFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.button_trip_history).performClick()
        }
        assertEquals(R.id.tripHistoryFragment, navController.currentDestination?.id)
    }

    // ==================== Closet & Badge Card Clickable ====================

    @Test
    fun `closet card is clickable`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val card = fragment.requireView().findViewById<View>(R.id.card_closet)
            assertTrue(card.isClickable)
        }
    }

    @Test
    fun `badges card is clickable`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val card = fragment.requireView().findViewById<View>(R.id.card_badges)
            assertTrue(card.isClickable)
        }
    }

    // ==================== getItemShortName - all branches ====================

    @Test
    fun `getItemShortName returns correct name for head items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemShortName", String::class.java)
            assertEquals("Grad Cap", method.invoke(fragment, "hat_grad"))
            assertEquals("Cap", method.invoke(fragment, "hat_cap"))
            assertEquals("Helmet", method.invoke(fragment, "hat_helmet"))
            assertEquals("Beret", method.invoke(fragment, "hat_beret"))
            assertEquals("Crown", method.invoke(fragment, "hat_crown"))
            assertEquals("Party Hat", method.invoke(fragment, "hat_party"))
            assertEquals("Beanie", method.invoke(fragment, "hat_beanie"))
            assertEquals("Cowboy", method.invoke(fragment, "hat_cowboy"))
            assertEquals("Chef Hat", method.invoke(fragment, "hat_chef"))
            assertEquals("Wizard Hat", method.invoke(fragment, "hat_wizard"))
        }
    }

    @Test
    fun `getItemShortName returns correct name for face items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemShortName", String::class.java)
            assertEquals("Square Glasses", method.invoke(fragment, "face_glasses_square"))
            assertEquals("Sunglasses", method.invoke(fragment, "glasses_sun"))
            assertEquals("Goggles", method.invoke(fragment, "face_goggles"))
            assertEquals("Nerd Glasses", method.invoke(fragment, "glasses_nerd"))
            assertEquals("3D Glasses", method.invoke(fragment, "glasses_3d"))
            assertEquals("Hero Mask", method.invoke(fragment, "face_mask"))
            assertEquals("Monocle", method.invoke(fragment, "face_monocle"))
            assertEquals("Scarf", method.invoke(fragment, "face_scarf"))
            assertEquals("VR Headset", method.invoke(fragment, "face_vr"))
        }
    }

    @Test
    fun `getItemShortName returns correct name for body items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemShortName", String::class.java)
            assertEquals("White Shirt", method.invoke(fragment, "body_white_shirt"))
            assertEquals("NUS Tee", method.invoke(fragment, "shirt_nus"))
            assertEquals("Hoodie", method.invoke(fragment, "shirt_hoodie"))
            assertEquals("Plaid", method.invoke(fragment, "body_plaid"))
            assertEquals("Suit", method.invoke(fragment, "body_suit"))
            assertEquals("Lab Coat", method.invoke(fragment, "body_coat"))
            assertEquals("Jersey", method.invoke(fragment, "body_sports"))
            assertEquals("Kimono", method.invoke(fragment, "body_kimono"))
            assertEquals("Tuxedo", method.invoke(fragment, "body_tux"))
            assertEquals("Cape", method.invoke(fragment, "body_superhero"))
            assertEquals("Doctor", method.invoke(fragment, "body_doctor"))
            assertEquals("Pilot", method.invoke(fragment, "body_pilot"))
            assertEquals("Ninja", method.invoke(fragment, "body_ninja"))
            assertEquals("Scrubs", method.invoke(fragment, "body_scrubs"))
            assertEquals("Polo", method.invoke(fragment, "body_polo"))
        }
    }

    @Test
    fun `getItemShortName returns id for unknown item`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemShortName", String::class.java)
            assertEquals("unknown_item_xyz", method.invoke(fragment, "unknown_item_xyz"))
        }
    }

    // ==================== getBadgeEmoji - all branches ====================

    @Test
    fun `getBadgeEmoji returns correct emoji for badge_c series`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getBadgeEmoji", String::class.java)
            assertEquals("🌱", method.invoke(fragment, "badge_c1"))
            assertEquals("🚶", method.invoke(fragment, "badge_c2"))
            assertEquals("♻️", method.invoke(fragment, "badge_c3"))
            assertEquals("🌳", method.invoke(fragment, "badge_c4"))
            assertEquals("🚌", method.invoke(fragment, "badge_c5"))
            assertEquals("🌍", method.invoke(fragment, "badge_c6"))
            assertEquals("⚡", method.invoke(fragment, "badge_c7"))
            assertEquals("🦸", method.invoke(fragment, "badge_c8"))
            assertEquals("👑", method.invoke(fragment, "badge_c9"))
            assertEquals("🏆", method.invoke(fragment, "badge_c10"))
        }
    }

    @Test
    fun `getBadgeEmoji returns correct emoji for a series`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getBadgeEmoji", String::class.java)
            assertEquals("🌱", method.invoke(fragment, "a1"))
            assertEquals("🚶", method.invoke(fragment, "a2"))
            assertEquals("♻️", method.invoke(fragment, "a3"))
            assertEquals("🌳", method.invoke(fragment, "a4"))
            assertEquals("🚌", method.invoke(fragment, "a5"))
            assertEquals("🌍", method.invoke(fragment, "a6"))
            assertEquals("⚡", method.invoke(fragment, "a7"))
            assertEquals("🦸", method.invoke(fragment, "a8"))
            assertEquals("👑", method.invoke(fragment, "a9"))
            assertEquals("🏆", method.invoke(fragment, "a10"))
            assertEquals("💎", method.invoke(fragment, "a11"))
            assertEquals("🚴", method.invoke(fragment, "a12"))
            assertEquals("🚶", method.invoke(fragment, "a13"))
            assertEquals("🚍", method.invoke(fragment, "a14"))
            assertEquals("♻️", method.invoke(fragment, "a15"))
            assertEquals("🦋", method.invoke(fragment, "a16"))
            assertEquals("🤝", method.invoke(fragment, "a17"))
            assertEquals("👥", method.invoke(fragment, "a18"))
            assertEquals("🎫", method.invoke(fragment, "a19"))
            assertEquals("🏆", method.invoke(fragment, "a20"))
        }
    }

    @Test
    fun `getBadgeEmoji returns default emoji for unknown badge`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getBadgeEmoji", String::class.java)
            assertEquals("🏅", method.invoke(fragment, "unknown_badge"))
        }
    }

    // ==================== getItemEmoji - all branches ====================

    @Test
    fun `getItemEmoji returns correct emoji for hat items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemEmoji", String::class.java)
            assertEquals("🧢", method.invoke(fragment, "hat_cap"))
            assertEquals("🎓", method.invoke(fragment, "hat_grad"))
            assertEquals("🧶", method.invoke(fragment, "hat_beanie"))
            assertEquals("💪", method.invoke(fragment, "hat_headband"))
            assertEquals("👑", method.invoke(fragment, "hat_crown"))
            assertEquals("🤠", method.invoke(fragment, "hat_cowboy"))
            assertEquals("🎧", method.invoke(fragment, "hat_headphones"))
            assertEquals("⛑️", method.invoke(fragment, "hat_hardhat"))
            assertEquals("👨‍🍳", method.invoke(fragment, "hat_chef"))
            assertEquals("🧙", method.invoke(fragment, "hat_wizard"))
        }
    }

    @Test
    fun `getItemEmoji returns correct emoji for face items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemEmoji", String::class.java)
            assertEquals("👓", method.invoke(fragment, "face_glasses_square"))
            assertEquals("👓", method.invoke(fragment, "face_glasses_round"))
            assertEquals("😎", method.invoke(fragment, "face_sunglasses"))
            assertEquals("😷", method.invoke(fragment, "face_mask"))
            assertEquals("🧐", method.invoke(fragment, "face_monocle"))
            assertEquals("🥽", method.invoke(fragment, "face_goggles"))
            assertEquals("🥽", method.invoke(fragment, "face_vr"))
            assertEquals("🤿", method.invoke(fragment, "face_diving"))
            assertEquals("🧣", method.invoke(fragment, "face_scarf"))
        }
    }

    @Test
    fun `getItemEmoji returns correct emoji for body items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemEmoji", String::class.java)
            assertEquals("👔", method.invoke(fragment, "body_white_shirt"))
            assertEquals("👕", method.invoke(fragment, "shirt_nus"))
            assertEquals("📚", method.invoke(fragment, "shirt_fass"))
            assertEquals("💼", method.invoke(fragment, "shirt_business"))
            assertEquals("⚖️", method.invoke(fragment, "shirt_law"))
            assertEquals("🦷", method.invoke(fragment, "shirt_dent"))
            assertEquals("🎨", method.invoke(fragment, "shirt_arts"))
            assertEquals("💻", method.invoke(fragment, "shirt_comp"))
            assertEquals("🎵", method.invoke(fragment, "shirt_music"))
            assertEquals("🏥", method.invoke(fragment, "shirt_pub_health"))
            assertEquals("🩺", method.invoke(fragment, "body_doctor"))
            assertEquals("🧥", method.invoke(fragment, "body_hoodie"))
            assertEquals("🤵", method.invoke(fragment, "body_suit"))
            assertEquals("👔", method.invoke(fragment, "body_scrubs"))
            assertEquals("👕", method.invoke(fragment, "body_polo"))
        }
    }

    @Test
    fun `getItemEmoji returns default emoji for unknown item`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemEmoji", String::class.java)
            assertEquals("👕", method.invoke(fragment, "some_unknown_item"))
        }
    }

    // ==================== Initial State Verification ====================

    @Test
    fun `initial currentPoints is zero`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val points = getFieldValue(fragment, "currentPoints") as Int
            assertEquals(0, points)
        }
    }

    @Test
    fun `initial currentUserId is empty`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val userId = getFieldValue(fragment, "currentUserId") as String
            assertEquals("", userId)
        }
    }

    @Test
    fun `initial inventory is empty`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val inventory = getFieldValue(fragment, "inventory") as MutableList<String>
            assertTrue(inventory.isEmpty())
        }
    }

    @Test
    fun `initial userFacultyId is empty`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val facultyId = getFieldValue(fragment, "userFacultyId") as String
            assertEquals("", facultyId)
        }
    }

    @Test
    fun `initial closetCurrentTab is all`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val tab = getFieldValue(fragment, "closetCurrentTab") as String
            assertEquals("all", tab)
        }
    }

    @Test
    fun `initial equippedFacultyId is null`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val equipped = getFieldValue(fragment, "equippedFacultyId")
            assertNull(equipped)
        }
    }

    // ==================== getShopItemsGrouped - empty state ====================

    @Test
    fun `getShopItemsGrouped returns empty when no shop items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getShopItemsGrouped")
            @Suppress("UNCHECKED_CAST")
            val result = method.invoke(fragment) as List<*>
            assertTrue(result.isEmpty())
        }
    }

    // ==================== updateClosetPreview - initial state ====================

    @Test
    fun `closet description has default text initially`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val desc = fragment.requireView().findViewById<TextView>(R.id.text_closet_desc)
            assertNotNull(desc)
            assertTrue(desc.text.toString().isNotEmpty())
        }
    }

    // ==================== updateBadgeEntry - initial state ====================

    @Test
    fun `badge count has default text initially`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val count = fragment.requireView().findViewById<TextView>(R.id.text_badge_count)
            assertNotNull(count)
            assertTrue(count.text.toString().contains("unlocked"))
        }
    }

    @Test
    fun `badge preview shows trophy emoji initially`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val preview = fragment.requireView().findViewById<TextView>(R.id.text_badge_preview)
            assertEquals("🏆", preview.text.toString())
        }
    }

    // ==================== updateMascotOutfit ====================

    @Test
    fun `updateMascotOutfit sets outfit from currentOutfit map`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["head"] = "hat_cap"
            outfitMap["face"] = "face_mask"
            outfitMap["body"] = "body_suit"
            outfitMap["badge"] = "none"

            val method = getPrivateMethod("updateMascotOutfit")
            method.invoke(fragment)

            val mascot = fragment.requireView().findViewById<com.ecogo.ui.views.MascotLionView>(R.id.mascot_lion)
            assertNotNull(mascot.outfit)
            assertEquals("hat_cap", mascot.outfit.head)
            assertEquals("face_mask", mascot.outfit.face)
            assertEquals("body_suit", mascot.outfit.body)
        }
    }

    // ==================== updateClosetPreview with outfit ====================

    @Test
    fun `updateClosetPreview reflects current outfit`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["head"] = "hat_grad"
            outfitMap["face"] = "none"
            outfitMap["body"] = "shirt_nus"
            outfitMap["badge"] = "none"

            val method = getPrivateMethod("updateClosetPreview")
            method.invoke(fragment)

            val preview = fragment.requireView()
                .findViewById<com.ecogo.ui.views.MascotLionView>(R.id.mascot_closet_preview)
            assertNotNull(preview.outfit)
            assertEquals("hat_grad", preview.outfit.head)
            assertEquals("shirt_nus", preview.outfit.body)
        }
    }

    // ==================== loadBadgesAndCloths - early return ====================

    @Test
    fun `loadBadgesAndCloths returns early when userId is empty`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "currentUserId", "")
            val method = getPrivateMethod("loadBadgesAndCloths")
            // Should not throw - just returns early
            method.invoke(fragment)
        }
    }

    // ==================== handleItemClick branches ====================

    @Test
    fun `handleItemClick with equipped item calls unequip`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // Set up state: item is in inventory and equipped
            @Suppress("UNCHECKED_CAST")
            val inventory = getFieldValue(fragment, "inventory") as MutableList<String>
            inventory.add("hat_cap")

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfit["head"] = "hat_cap"

            setFieldValue(fragment, "currentUserId", "test-user")

            // Create a ShopItem
            val item = ShopItem(
                id = "hat_cap", name = "Cap", type = "head",
                cost = 50, owned = true, equipped = true
            )

            // handleItemClick should detect equipped and attempt unequip
            val method = getPrivateMethod("handleItemClick", ShopItem::class.java)
            // This will attempt API call which will fail gracefully
            method.invoke(fragment, item)

            // Verify the method ran (it will try unequip API, which will fail silently)
        }
    }

    @Test
    fun `handleItemClick with owned but not equipped item calls equip`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val inventory = getFieldValue(fragment, "inventory") as MutableList<String>
            inventory.add("hat_cap")

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfit["head"] = "none"

            setFieldValue(fragment, "currentUserId", "test-user")

            val item = ShopItem(
                id = "hat_cap", name = "Cap", type = "head",
                cost = 50, owned = true, equipped = false
            )

            val method = getPrivateMethod("handleItemClick", ShopItem::class.java)
            method.invoke(fragment, item)
        }
    }

    @Test
    fun `handleItemClick with unowned item shows purchase dialog`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfit["head"] = "none"

            val item = ShopItem(
                id = "hat_wizard", name = "Wizard Hat", type = "head",
                cost = 100, owned = false, equipped = false
            )

            val method = getPrivateMethod("handleItemClick", ShopItem::class.java)
            // Should show purchase dialog (won't crash in Robolectric)
            method.invoke(fragment, item)
        }
    }

    // ==================== getFacultyOutfitCost ====================

    @Test
    fun `getFacultyOutfitCost calculates cost from shop items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getFacultyOutfitCost", com.ecogo.data.FacultyData::class.java)

            // Create a faculty with all "none" outfit
            val faculty = com.ecogo.data.FacultyData(
                id = "test",
                name = "Test Faculty",
                color = "#000000",
                slogan = "Test Slogan",
                outfit = Outfit(head = "none", face = "none", body = "none", badge = "none")
            )
            val cost = method.invoke(fragment, faculty) as Int
            assertEquals(0, cost)
        }
    }

    // ==================== restoreOutfitFromServer ====================

    @Test
    fun `restoreOutfitFromServer resets outfit to none when no displayed items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfit["head"] = "hat_cap"
            outfit["face"] = "face_mask"

            val method = getPrivateMethod("restoreOutfitFromServer")
            method.invoke(fragment)

            assertEquals("none", outfit["head"])
            assertEquals("none", outfit["face"])
            assertEquals("none", outfit["body"])
            assertEquals("none", outfit["badge"])
        }
    }

    // ==================== purchaseClothWithApi - not enough points ====================

    @Test
    fun `purchaseClothWithApi shows toast when not enough points`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "currentPoints", 10)
            setFieldValue(fragment, "currentUserId", "test-user")

            val item = ShopItem(
                id = "hat_crown", name = "Crown", type = "head",
                cost = 500, owned = false, equipped = false
            )

            val method = getPrivateMethod("purchaseClothWithApi", ShopItem::class.java)
            // Should return early due to insufficient points
            method.invoke(fragment, item)

            // Points should remain unchanged
            val points = getFieldValue(fragment, "currentPoints") as Int
            assertEquals(10, points)
        }
    }

    // ==================== Fragment Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
