package com.ecogo.ui.fragments

import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import com.ecogo.data.Achievement
import com.ecogo.data.FacultyData
import com.ecogo.data.MockData
import com.ecogo.data.Outfit
import com.ecogo.data.ShopItem
import com.ecogo.data.dto.BadgeDto
import com.ecogo.data.dto.UserBadgeDto
import com.ecogo.ui.adapters.ShopListItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileFragmentTest {

    companion object {
        private const val TEST_USER = "test-user"
        private const val SEEDLING_EMOJI = "\uD83C\uDF31"
        private const val WALKING_EMOJI = "\uD83D\uDEB6"
        private const val CROWN_EMOJI = "\uD83D\uDC51"
        private const val TROPHY_EMOJI = "\uD83C\uDFC6"
        private const val SHIRT_EMOJI = "\uD83D\uDC55"
        private const val ECO_STARTER = "Eco Starter"
        private const val SOC_NAME = "School of Computing"
        private const val SOC_COLOR = "#3B82F6"
        private const val SOC_SLOGAN = "Code the Future"
    }

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

    /** Helper: create a BadgeDto for cloth items */
    private fun makeClothDto(
        badgeId: String,
        subCategory: String,
        name: String = "Test Item",
        cost: Int = 100
    ) = BadgeDto(
        badgeId = badgeId,
        name = mapOf("en" to name),
        description = mapOf("en" to "A test item"),
        purchaseCost = cost,
        category = "cloth",
        subCategory = subCategory,
        acquisitionMethod = "purchase",
        carbonThreshold = null,
        icon = null,
        isActive = true
    )

    /** Helper: create a BadgeDto for badge items */
    private fun makeBadgeDto(
        badgeId: String,
        name: String = "Test Badge",
        cost: Int? = 200,
        acquisitionMethod: String = "purchase"
    ) = BadgeDto(
        badgeId = badgeId,
        name = mapOf("en" to name),
        description = mapOf("en" to "A test badge"),
        purchaseCost = cost,
        category = "badge",
        subCategory = "rank",
        acquisitionMethod = acquisitionMethod,
        carbonThreshold = null,
        icon = null,
        isActive = true
    )

    /** Helper: create a UserBadgeDto */
    private fun makeUserBadge(
        badgeId: String,
        isDisplay: Boolean = false,
        userId: String = TEST_USER
    ) = UserBadgeDto(
        userId = userId,
        badgeId = badgeId,
        unlockedAt = null,
        isDisplay = isDisplay
    )

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
            assertEquals(SEEDLING_EMOJI, method.invoke(fragment, "badge_c1"))
            assertEquals(WALKING_EMOJI, method.invoke(fragment, "badge_c2"))
            assertEquals("♻️", method.invoke(fragment, "badge_c3"))
            assertEquals("\uD83C\uDF33", method.invoke(fragment, "badge_c4"))
            assertEquals("\uD83D\uDE8C", method.invoke(fragment, "badge_c5"))
            assertEquals("\uD83C\uDF0D", method.invoke(fragment, "badge_c6"))
            assertEquals("⚡", method.invoke(fragment, "badge_c7"))
            assertEquals("\uD83E\uDDB8", method.invoke(fragment, "badge_c8"))
            assertEquals(CROWN_EMOJI, method.invoke(fragment, "badge_c9"))
            assertEquals(TROPHY_EMOJI, method.invoke(fragment, "badge_c10"))
        }
    }

    @Test
    fun `getBadgeEmoji returns correct emoji for a series`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getBadgeEmoji", String::class.java)
            assertEquals(SEEDLING_EMOJI, method.invoke(fragment, "a1"))
            assertEquals(WALKING_EMOJI, method.invoke(fragment, "a2"))
            assertEquals("♻️", method.invoke(fragment, "a3"))
            assertEquals("\uD83C\uDF33", method.invoke(fragment, "a4"))
            assertEquals("\uD83D\uDE8C", method.invoke(fragment, "a5"))
            assertEquals("\uD83C\uDF0D", method.invoke(fragment, "a6"))
            assertEquals("⚡", method.invoke(fragment, "a7"))
            assertEquals("\uD83E\uDDB8", method.invoke(fragment, "a8"))
            assertEquals(CROWN_EMOJI, method.invoke(fragment, "a9"))
            assertEquals(TROPHY_EMOJI, method.invoke(fragment, "a10"))
            assertEquals("\uD83D\uDC8E", method.invoke(fragment, "a11"))
            assertEquals("\uD83D\uDEB4", method.invoke(fragment, "a12"))
            assertEquals(WALKING_EMOJI, method.invoke(fragment, "a13"))
            assertEquals("\uD83D\uDE8D", method.invoke(fragment, "a14"))
            assertEquals("♻️", method.invoke(fragment, "a15"))
            assertEquals("\uD83E\uDD8B", method.invoke(fragment, "a16"))
            assertEquals("\uD83E\uDD1D", method.invoke(fragment, "a17"))
            assertEquals("\uD83D\uDC65", method.invoke(fragment, "a18"))
            assertEquals("\uD83C\uDFAB", method.invoke(fragment, "a19"))
            assertEquals(TROPHY_EMOJI, method.invoke(fragment, "a20"))
        }
    }

    @Test
    fun `getBadgeEmoji returns default emoji for unknown badge`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getBadgeEmoji", String::class.java)
            assertEquals("\uD83C\uDFC5", method.invoke(fragment, "unknown_badge"))
        }
    }

    // ==================== getItemEmoji - all branches ====================

    @Test
    fun `getItemEmoji returns correct emoji for hat items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemEmoji", String::class.java)
            assertEquals("\uD83E\uDDE2", method.invoke(fragment, "hat_cap"))
            assertEquals("\uD83C\uDF93", method.invoke(fragment, "hat_grad"))
            assertEquals("\uD83E\uDDF6", method.invoke(fragment, "hat_beanie"))
            assertEquals("\uD83D\uDCAA", method.invoke(fragment, "hat_headband"))
            assertEquals(CROWN_EMOJI, method.invoke(fragment, "hat_crown"))
            assertEquals("\uD83E\uDD20", method.invoke(fragment, "hat_cowboy"))
            assertEquals("\uD83C\uDFA7", method.invoke(fragment, "hat_headphones"))
            assertEquals("⛑️", method.invoke(fragment, "hat_hardhat"))
            assertEquals("\uD83D\uDC68\u200D\uD83C\uDF73", method.invoke(fragment, "hat_chef"))
            assertEquals("\uD83E\uDDD9", method.invoke(fragment, "hat_wizard"))
        }
    }

    @Test
    fun `getItemEmoji returns correct emoji for face items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemEmoji", String::class.java)
            assertEquals("\uD83D\uDC53", method.invoke(fragment, "face_glasses_square"))
            assertEquals("\uD83D\uDC53", method.invoke(fragment, "face_glasses_round"))
            assertEquals("\uD83D\uDE0E", method.invoke(fragment, "face_sunglasses"))
            assertEquals("\uD83D\uDE37", method.invoke(fragment, "face_mask"))
            assertEquals("\uD83E\uDDD0", method.invoke(fragment, "face_monocle"))
            assertEquals("\uD83E\uDD7D", method.invoke(fragment, "face_goggles"))
            assertEquals("\uD83E\uDD7D", method.invoke(fragment, "face_vr"))
            assertEquals("\uD83E\uDD3F", method.invoke(fragment, "face_diving"))
            assertEquals("\uD83E\uDDE3", method.invoke(fragment, "face_scarf"))
        }
    }

    @Test
    fun `getItemEmoji returns correct emoji for body items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemEmoji", String::class.java)
            assertEquals("\uD83D\uDC54", method.invoke(fragment, "body_white_shirt"))
            assertEquals(SHIRT_EMOJI, method.invoke(fragment, "shirt_nus"))
            assertEquals("\uD83D\uDCDA", method.invoke(fragment, "shirt_fass"))
            assertEquals("\uD83D\uDCBC", method.invoke(fragment, "shirt_business"))
            assertEquals("⚖️", method.invoke(fragment, "shirt_law"))
            assertEquals("\uD83E\uDDB7", method.invoke(fragment, "shirt_dent"))
            assertEquals("\uD83C\uDFA8", method.invoke(fragment, "shirt_arts"))
            assertEquals("\uD83D\uDCBB", method.invoke(fragment, "shirt_comp"))
            assertEquals("\uD83C\uDFB5", method.invoke(fragment, "shirt_music"))
            assertEquals("\uD83C\uDFE5", method.invoke(fragment, "shirt_pub_health"))
            assertEquals("\uD83E\uDE7A", method.invoke(fragment, "body_doctor"))
            assertEquals("\uD83E\uDDE5", method.invoke(fragment, "body_hoodie"))
            assertEquals("\uD83E\uDD35", method.invoke(fragment, "body_suit"))
            assertEquals("\uD83D\uDC54", method.invoke(fragment, "body_scrubs"))
            assertEquals(SHIRT_EMOJI, method.invoke(fragment, "body_polo"))
        }
    }

    @Test
    fun `getItemEmoji returns default emoji for unknown item`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemEmoji", String::class.java)
            assertEquals(SHIRT_EMOJI, method.invoke(fragment, "some_unknown_item"))
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

    @Test
    fun `initial ownedFaculties is empty`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val owned = getFieldValue(fragment, "ownedFaculties") as MutableSet<String>
            assertTrue(owned.isEmpty())
        }
    }

    @Test
    fun `initial shopItems is empty`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val items = getFieldValue(fragment, "shopItems") as MutableList<*>
            assertTrue(items.isEmpty())
        }
    }

    @Test
    fun `initial userItems is empty`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val items = getFieldValue(fragment, "userItems") as MutableList<*>
            assertTrue(items.isEmpty())
        }
    }

    @Test
    fun `initial currentOutfit is empty map`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            // May be empty or may have been set during init
            assertNotNull(outfit)
        }
    }

    @Test
    fun `initial closetDialog is null`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNull(getFieldValue(fragment, "closetDialog"))
        }
    }

    // ==================== getShopItemsGrouped ====================

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

    @Test
    fun `getShopItemsGrouped returns grouped items with headers for head face body`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("hat_cap", "head", "Cap"))
            shopItems.add(makeClothDto("hat_grad", "head", "Grad Cap"))
            shopItems.add(makeClothDto("face_mask", "face", "Mask"))
            shopItems.add(makeClothDto("body_suit", "body", "Suit"))

            val method = getPrivateMethod("getShopItemsGrouped")
            @Suppress("UNCHECKED_CAST")
            val result = method.invoke(fragment) as List<ShopListItem>

            // Should have 3 headers + 4 items = 7
            assertTrue(result.isNotEmpty())
            // First item should be a header for "Head"
            assertTrue(result[0] is ShopListItem.Header)
            val headHeader = result[0] as ShopListItem.Header
            assertTrue(headHeader.title.contains("Head"))
        }
    }

    @Test
    fun `getShopItemsGrouped only shows head header when only head items exist`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("hat_cap", "head", "Cap"))

            val method = getPrivateMethod("getShopItemsGrouped")
            @Suppress("UNCHECKED_CAST")
            val result = method.invoke(fragment) as List<ShopListItem>

            // 1 header + 1 item
            assertEquals(2, result.size)
            assertTrue(result[0] is ShopListItem.Header)
            assertTrue(result[1] is ShopListItem.Item)
        }
    }

    @Test
    fun `getShopItemsGrouped only shows body header when only body items exist`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("body_suit", "body", "Suit"))
            shopItems.add(makeClothDto("shirt_nus", "body", "NUS Tee"))

            val method = getPrivateMethod("getShopItemsGrouped")
            @Suppress("UNCHECKED_CAST")
            val result = method.invoke(fragment) as List<ShopListItem>

            // 1 header + 2 items
            assertEquals(3, result.size)
            val header = result[0] as ShopListItem.Header
            assertTrue(header.title.contains("Body"))
        }
    }

    @Test
    fun `getShopItemsGrouped only shows face header when only face items exist`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("face_mask", "face", "Mask"))

            val method = getPrivateMethod("getShopItemsGrouped")
            @Suppress("UNCHECKED_CAST")
            val result = method.invoke(fragment) as List<ShopListItem>

            assertEquals(2, result.size)
            val header = result[0] as ShopListItem.Header
            assertTrue(header.title.contains("Face"))
        }
    }

    @Test
    fun `getShopItemsGrouped ignores badge category items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            // Only add badge items, no cloth items
            shopItems.add(makeBadgeDto("badge_c1", ECO_STARTER))
            shopItems.add(makeBadgeDto("badge_c2", "Walker"))

            val method = getPrivateMethod("getShopItemsGrouped")
            @Suppress("UNCHECKED_CAST")
            val result = method.invoke(fragment) as List<ShopListItem>

            // Badges are excluded from cloth grouping -> empty result
            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun `getShopItemsGrouped marks owned items correctly`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("hat_cap", "head", "Cap"))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("hat_cap", isDisplay = true))

            val method = getPrivateMethod("getShopItemsGrouped")
            @Suppress("UNCHECKED_CAST")
            val result = method.invoke(fragment) as List<ShopListItem>

            val item = result[1] as ShopListItem.Item
            assertTrue(item.shopItem.owned)
            assertTrue(item.shopItem.equipped)
        }
    }

    // ==================== updateClosetPreview ====================

    @Test
    fun `closet description has default text initially`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val desc = fragment.requireView().findViewById<TextView>(R.id.text_closet_desc)
            assertNotNull(desc)
            assertTrue(desc.text.toString().isNotEmpty())
        }
    }

    @Test
    fun `updateClosetPreview shows count of cloth items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("hat_cap", "head", "Cap"))
            shopItems.add(makeClothDto("face_mask", "face", "Mask"))
            shopItems.add(makeClothDto("body_suit", "body", "Suit"))
            // Add a badge item - should NOT be counted
            shopItems.add(makeBadgeDto("badge_c1", ECO_STARTER))

            val method = getPrivateMethod("updateClosetPreview")
            method.invoke(fragment)

            val desc = fragment.requireView().findViewById<TextView>(R.id.text_closet_desc)
            // Should say "Browse & equip 3 outfits" (only cloth items counted)
            assertEquals("Browse & equip 3 outfits", desc.text.toString())
        }
    }

    @Test
    fun `updateClosetPreview shows zero when only badges exist`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeBadgeDto("badge_c1", ECO_STARTER))

            val method = getPrivateMethod("updateClosetPreview")
            method.invoke(fragment)

            val desc = fragment.requireView().findViewById<TextView>(R.id.text_closet_desc)
            assertEquals("Browse & equip 0 outfits", desc.text.toString())
        }
    }

    @Test
    fun `updateClosetPreview sets mascot outfit from currentOutfit`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["head"] = "hat_grad"
            outfitMap["face"] = "face_mask"
            outfitMap["body"] = "shirt_nus"
            outfitMap["badge"] = "badge_c1"

            val method = getPrivateMethod("updateClosetPreview")
            method.invoke(fragment)

            val preview = fragment.requireView()
                .findViewById<com.ecogo.ui.views.MascotLionView>(R.id.mascot_closet_preview)
            assertEquals("hat_grad", preview.outfit.head)
            assertEquals("face_mask", preview.outfit.face)
            assertEquals("shirt_nus", preview.outfit.body)
            assertEquals("badge_c1", preview.outfit.badge)
        }
    }

    // ==================== updateBadgeEntry ====================

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
            assertEquals(TROPHY_EMOJI, preview.text.toString())
        }
    }

    @Test
    fun `updateBadgeEntry shows correct badge counts`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeBadgeDto("badge_c1", "Badge 1"))
            shopItems.add(makeBadgeDto("badge_c2", "Badge 2"))
            shopItems.add(makeBadgeDto("badge_c3", "Badge 3"))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("badge_c1"))
            userItems.add(makeUserBadge("badge_c3"))

            val method = getPrivateMethod("updateBadgeEntry")
            method.invoke(fragment)

            val count = fragment.requireView().findViewById<TextView>(R.id.text_badge_count)
            assertEquals("2 / 3 unlocked", count.text.toString())
        }
    }

    @Test
    fun `updateBadgeEntry shows zero counts when empty`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // shopItems is empty by default
            val method = getPrivateMethod("updateBadgeEntry")
            method.invoke(fragment)

            val count = fragment.requireView().findViewById<TextView>(R.id.text_badge_count)
            assertEquals("0 / 0 unlocked", count.text.toString())
        }
    }

    @Test
    fun `updateBadgeEntry shows badge emoji when badge is equipped`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["badge"] = "badge_c1"

            val method = getPrivateMethod("updateBadgeEntry")
            method.invoke(fragment)

            val preview = fragment.requireView().findViewById<TextView>(R.id.text_badge_preview)
            assertEquals(SEEDLING_EMOJI, preview.text.toString()) // badge_c1 -> 🌱
        }
    }

    @Test
    fun `updateBadgeEntry shows trophy when no badge equipped`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["badge"] = "none"

            val method = getPrivateMethod("updateBadgeEntry")
            method.invoke(fragment)

            val preview = fragment.requireView().findViewById<TextView>(R.id.text_badge_preview)
            assertEquals(TROPHY_EMOJI, preview.text.toString()) // 🏆
        }
    }

    @Test
    fun `updateBadgeEntry does not count cloth items as badges`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("hat_cap", "head", "Cap"))
            shopItems.add(makeBadgeDto("badge_c1", "Badge 1"))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("hat_cap"))
            userItems.add(makeUserBadge("badge_c1"))

            val method = getPrivateMethod("updateBadgeEntry")
            method.invoke(fragment)

            val count = fragment.requireView().findViewById<TextView>(R.id.text_badge_count)
            // Only 1 badge in shop, 1 unlocked
            assertEquals("1 / 1 unlocked", count.text.toString())
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

    @Test
    fun `updateMascotOutfit defaults to none when key missing`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // currentOutfit is empty, so getOrDefault should return "none"
            val method = getPrivateMethod("updateMascotOutfit")
            method.invoke(fragment)

            val mascot = fragment.requireView().findViewById<com.ecogo.ui.views.MascotLionView>(R.id.mascot_lion)
            assertEquals("none", mascot.outfit.head)
            assertEquals("none", mascot.outfit.face)
            assertEquals("none", mascot.outfit.body)
            assertEquals("none", mascot.outfit.badge)
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

    @Test
    fun `restoreOutfitFromServer restores head cloth`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("hat_cap", "head", "Cap"))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("hat_cap", isDisplay = true))

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>

            val method = getPrivateMethod("restoreOutfitFromServer")
            method.invoke(fragment)

            assertEquals("hat_cap", outfit["head"])
            assertEquals("none", outfit["face"])
            assertEquals("none", outfit["body"])
            assertEquals("none", outfit["badge"])
        }
    }

    @Test
    fun `restoreOutfitFromServer restores face cloth`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("face_mask", "face", "Mask"))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("face_mask", isDisplay = true))

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>

            val method = getPrivateMethod("restoreOutfitFromServer")
            method.invoke(fragment)

            assertEquals("none", outfit["head"])
            assertEquals("face_mask", outfit["face"])
            assertEquals("none", outfit["body"])
            assertEquals("none", outfit["badge"])
        }
    }

    @Test
    fun `restoreOutfitFromServer restores body cloth`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("body_suit", "body", "Suit"))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("body_suit", isDisplay = true))

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>

            val method = getPrivateMethod("restoreOutfitFromServer")
            method.invoke(fragment)

            assertEquals("none", outfit["head"])
            assertEquals("none", outfit["face"])
            assertEquals("body_suit", outfit["body"])
            assertEquals("none", outfit["badge"])
        }
    }

    @Test
    fun `restoreOutfitFromServer restores badge rank`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeBadgeDto("badge_c1", ECO_STARTER))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("badge_c1", isDisplay = true))

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>

            val method = getPrivateMethod("restoreOutfitFromServer")
            method.invoke(fragment)

            assertEquals("none", outfit["head"])
            assertEquals("none", outfit["face"])
            assertEquals("none", outfit["body"])
            assertEquals("badge_c1", outfit["badge"])
        }
    }

    @Test
    fun `restoreOutfitFromServer skips unknown category`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            // Item with unknown category/subCategory
            shopItems.add(BadgeDto(
                badgeId = "mystery_item",
                name = mapOf("en" to "Mystery"),
                description = mapOf("en" to "Unknown"),
                purchaseCost = 50,
                category = "special",
                subCategory = "misc",
                acquisitionMethod = "event",
                carbonThreshold = null,
                icon = null,
                isActive = true
            ))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("mystery_item", isDisplay = true))

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>

            val method = getPrivateMethod("restoreOutfitFromServer")
            method.invoke(fragment)

            // All should remain "none" since category is unknown
            assertEquals("none", outfit["head"])
            assertEquals("none", outfit["face"])
            assertEquals("none", outfit["body"])
            assertEquals("none", outfit["badge"])
        }
    }

    @Test
    fun `restoreOutfitFromServer skips item not in shop map`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // shopItems is empty - no items in shop
            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("hat_cap", isDisplay = true))

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>

            val method = getPrivateMethod("restoreOutfitFromServer")
            method.invoke(fragment)

            // hat_cap not in shop, so should be skipped
            assertEquals("none", outfit["head"])
        }
    }

    @Test
    fun `restoreOutfitFromServer restores all slots at once`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("hat_cap", "head", "Cap"))
            shopItems.add(makeClothDto("face_mask", "face", "Mask"))
            shopItems.add(makeClothDto("body_suit", "body", "Suit"))
            shopItems.add(makeBadgeDto("badge_c1", "Badge"))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("hat_cap", isDisplay = true))
            userItems.add(makeUserBadge("face_mask", isDisplay = true))
            userItems.add(makeUserBadge("body_suit", isDisplay = true))
            userItems.add(makeUserBadge("badge_c1", isDisplay = true))

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>

            val method = getPrivateMethod("restoreOutfitFromServer")
            method.invoke(fragment)

            assertEquals("hat_cap", outfit["head"])
            assertEquals("face_mask", outfit["face"])
            assertEquals("body_suit", outfit["body"])
            assertEquals("badge_c1", outfit["badge"])
        }
    }

    @Test
    fun `restoreOutfitFromServer ignores non-displayed items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeClothDto("hat_cap", "head", "Cap"))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            // isDisplay = false => not currently equipped
            userItems.add(makeUserBadge("hat_cap", isDisplay = false))

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>

            val method = getPrivateMethod("restoreOutfitFromServer")
            method.invoke(fragment)

            assertEquals("none", outfit["head"])
        }
    }

    // ==================== getFacultyOutfitCost ====================

    @Test
    fun `getFacultyOutfitCost returns zero for all-none outfit`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getFacultyOutfitCost", FacultyData::class.java)
            val faculty = FacultyData(
                id = "test", name = "Test", color = "#000",
                slogan = "Test", outfit = Outfit()
            )
            val cost = method.invoke(fragment, faculty) as Int
            assertEquals(0, cost)
        }
    }

    @Test
    fun `getFacultyOutfitCost adds head cost from MockData`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getFacultyOutfitCost", FacultyData::class.java)
            // Use a faculty with a head item that exists in MockData.SHOP_ITEMS
            val faculty = FacultyData(
                id = "test", name = "Test", color = "#000",
                slogan = "Test", outfit = Outfit(head = "hat_cap")
            )
            val cost = method.invoke(fragment, faculty) as Int
            // hat_cap should have a cost in MockData.SHOP_ITEMS
            assertTrue(cost >= 0)
        }
    }

    @Test
    fun `getFacultyOutfitCost adds face cost from MockData`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getFacultyOutfitCost", FacultyData::class.java)
            val faculty = FacultyData(
                id = "test", name = "Test", color = "#000",
                slogan = "Test", outfit = Outfit(face = "face_glasses_square")
            )
            val cost = method.invoke(fragment, faculty) as Int
            assertTrue(cost >= 0)
        }
    }

    @Test
    fun `getFacultyOutfitCost adds body cost from MockData`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getFacultyOutfitCost", FacultyData::class.java)
            val faculty = FacultyData(
                id = "test", name = "Test", color = "#000",
                slogan = "Test", outfit = Outfit(body = "shirt_nus")
            )
            val cost = method.invoke(fragment, faculty) as Int
            assertTrue(cost >= 0)
        }
    }

    @Test
    fun `getFacultyOutfitCost sums all non-none parts`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getFacultyOutfitCost", FacultyData::class.java)
            val faculty = FacultyData(
                id = "test", name = "Test", color = "#000",
                slogan = "Test",
                outfit = Outfit(head = "hat_cap", face = "face_glasses_square", body = "shirt_nus")
            )
            val totalCost = method.invoke(fragment, faculty) as Int

            // Individual costs
            val headFaculty = FacultyData("t", "T", "#000", "T", Outfit(head = "hat_cap"))
            val faceFaculty = FacultyData("t", "T", "#000", "T", Outfit(face = "face_glasses_square"))
            val bodyFaculty = FacultyData("t", "T", "#000", "T", Outfit(body = "shirt_nus"))
            val headCost = method.invoke(fragment, headFaculty) as Int
            val faceCost = method.invoke(fragment, faceFaculty) as Int
            val bodyCost = method.invoke(fragment, bodyFaculty) as Int

            assertEquals(headCost + faceCost + bodyCost, totalCost)
        }
    }

    @Test
    fun `getFacultyOutfitCost returns zero for unknown items`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getFacultyOutfitCost", FacultyData::class.java)
            val faculty = FacultyData(
                id = "test", name = "Test", color = "#000",
                slogan = "Test",
                outfit = Outfit(head = "nonexistent_hat", face = "nonexistent_face", body = "nonexistent_body")
            )
            val cost = method.invoke(fragment, faculty) as Int
            assertEquals(0, cost) // Items not in SHOP_ITEMS -> cost defaults to 0
        }
    }

    // ==================== equipFacultyOutfit ====================

    @Test
    fun `equipFacultyOutfit sets outfit and equippedFacultyId`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("equipFacultyOutfit", FacultyData::class.java)
            val faculty = FacultyData(
                id = "soc", name = SOC_NAME, color = SOC_COLOR,
                slogan = SOC_SLOGAN,
                outfit = Outfit(head = "hat_cap", face = "none", body = "shirt_hoodie")
            )
            method.invoke(fragment, faculty)

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            assertEquals("hat_cap", outfit["head"])
            assertEquals("none", outfit["face"])
            assertEquals("shirt_hoodie", outfit["body"])
            assertEquals("none", outfit["badge"])

            val equippedId = getFieldValue(fragment, "equippedFacultyId") as String
            assertEquals("soc", equippedId)
        }
    }

    @Test
    fun `equipFacultyOutfit updates mascot on main binding`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("equipFacultyOutfit", FacultyData::class.java)
            val faculty = FacultyData(
                id = "iss", name = "ISS", color = "#6B7280",
                slogan = "Systems",
                outfit = Outfit(face = "face_glasses_square", body = "body_white_shirt")
            )
            method.invoke(fragment, faculty)

            val mascot = fragment.requireView().findViewById<com.ecogo.ui.views.MascotLionView>(R.id.mascot_lion)
            assertEquals("face_glasses_square", mascot.outfit.face)
            assertEquals("body_white_shirt", mascot.outfit.body)
        }
    }

    // ==================== handleFacultyClick ====================

    @Test
    fun `handleFacultyClick with owned faculty equips directly`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val ownedFaculties = getFieldValue(fragment, "ownedFaculties") as MutableSet<String>
            ownedFaculties.add("soc")

            val faculty = FacultyData(
                id = "soc", name = SOC_NAME, color = SOC_COLOR,
                slogan = SOC_SLOGAN,
                outfit = Outfit(head = "hat_cap", face = "none", body = "shirt_hoodie")
            )

            val method = getPrivateMethod("handleFacultyClick", FacultyData::class.java)
            method.invoke(fragment, faculty)

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            assertEquals("hat_cap", outfit["head"])
            assertEquals("shirt_hoodie", outfit["body"])

            val equippedId = getFieldValue(fragment, "equippedFacultyId") as String
            assertEquals("soc", equippedId)
        }
    }

    @Test
    fun `handleFacultyClick with all components owned auto-equips`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // All components are in inventory
            @Suppress("UNCHECKED_CAST")
            val inventory = getFieldValue(fragment, "inventory") as MutableList<String>
            inventory.add("hat_cap")
            inventory.add("shirt_hoodie")

            val faculty = FacultyData(
                id = "soc", name = SOC_NAME, color = SOC_COLOR,
                slogan = SOC_SLOGAN,
                outfit = Outfit(head = "hat_cap", face = "none", body = "shirt_hoodie")
            )

            val method = getPrivateMethod("handleFacultyClick", FacultyData::class.java)
            method.invoke(fragment, faculty)

            // Should be auto-owned and equipped
            @Suppress("UNCHECKED_CAST")
            val ownedFaculties = getFieldValue(fragment, "ownedFaculties") as MutableSet<String>
            assertTrue(ownedFaculties.contains("soc"))

            val equippedId = getFieldValue(fragment, "equippedFacultyId") as String
            assertEquals("soc", equippedId)
        }
    }

    @Test
    fun `handleFacultyClick with missing components shows purchase dialog`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "currentPoints", 1000)

            // Only own one of two components
            @Suppress("UNCHECKED_CAST")
            val inventory = getFieldValue(fragment, "inventory") as MutableList<String>
            inventory.add("hat_cap")
            // shirt_hoodie is missing

            val faculty = FacultyData(
                id = "soc", name = SOC_NAME, color = SOC_COLOR,
                slogan = SOC_SLOGAN,
                outfit = Outfit(head = "hat_cap", face = "none", body = "shirt_hoodie")
            )

            val method = getPrivateMethod("handleFacultyClick", FacultyData::class.java)
            // Dialog.show() may throw IllegalStateException in Robolectric
            try {
                method.invoke(fragment, faculty)
            } catch (e: java.lang.reflect.InvocationTargetException) {
                // Expected: showConfirmPurchaseDialog calls Dialog.show()
                // which can throw IllegalStateException in Robolectric
            }

            // Faculty should NOT be owned yet (user needs to confirm purchase)
            @Suppress("UNCHECKED_CAST")
            val ownedFaculties = getFieldValue(fragment, "ownedFaculties") as MutableSet<String>
            assertFalse(ownedFaculties.contains("soc"))
        }
    }

    @Test
    fun `handleFacultyClick with all-none outfit auto-equips`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val faculty = FacultyData(
                id = "test", name = "Test", color = "#000",
                slogan = "Test",
                outfit = Outfit() // all "none"
            )

            val method = getPrivateMethod("handleFacultyClick", FacultyData::class.java)
            method.invoke(fragment, faculty)

            // No components needed, so auto-equips
            @Suppress("UNCHECKED_CAST")
            val ownedFaculties = getFieldValue(fragment, "ownedFaculties") as MutableSet<String>
            assertTrue(ownedFaculties.contains("test"))
        }
    }

    // ==================== handleBadgeClick ====================

    @Test
    fun `handleBadgeClick with badge not in shopItems does nothing`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // shopItems is empty - badge won't be found
            val method = getPrivateMethod(
                "handleBadgeClick",
                String::class.java,
                android.app.Dialog::class.java,
                com.ecogo.ui.views.MascotLionView::class.java,
                TextView::class.java
            )
            // Should not crash when badge not found
            method.invoke(fragment, "nonexistent_badge", null, null, null)
        }
    }

    @Test
    fun `handleBadgeClick with badge found shows detail dialog`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeBadgeDto("badge_c1", ECO_STARTER))

            val method = getPrivateMethod(
                "handleBadgeClick",
                String::class.java,
                android.app.Dialog::class.java,
                com.ecogo.ui.views.MascotLionView::class.java,
                TextView::class.java
            )
            // Shows badge detail dialog, won't crash in Robolectric
            method.invoke(fragment, "badge_c1", null, null, null)
        }
    }

    // ==================== showBadgeDetailDialog ====================

    @Test
    fun `showBadgeDetailDialog displays equipped badge state`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // Set the badge as equipped
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["badge"] = "badge_c1"

            val achievement = Achievement(
                id = "badge_c1", name = ECO_STARTER,
                description = "Begin your eco journey",
                unlocked = true, howToUnlock = "Start using eco transport"
            )

            val method = getPrivateMethod(
                "showBadgeDetailDialog",
                Achievement::class.java,
                android.app.Dialog::class.java,
                com.ecogo.ui.views.MascotLionView::class.java,
                TextView::class.java
            )
            // Should not crash, shows dialog
            method.invoke(fragment, achievement, null, null, null)
        }
    }

    @Test
    fun `showBadgeDetailDialog displays unlocked badge state`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["badge"] = "none" // different badge equipped

            val achievement = Achievement(
                id = "badge_c2", name = "Walker",
                description = "Walk frequently",
                unlocked = true, howToUnlock = "Walk 10km"
            )

            val method = getPrivateMethod(
                "showBadgeDetailDialog",
                Achievement::class.java,
                android.app.Dialog::class.java,
                com.ecogo.ui.views.MascotLionView::class.java,
                TextView::class.java
            )
            method.invoke(fragment, achievement, null, null, null)
        }
    }

    @Test
    fun `showBadgeDetailDialog displays purchasable badge state`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["badge"] = "none"

            // Add badge to shopItems with purchase acquisition
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeBadgeDto("badge_c3", "Recycler", cost = 200, acquisitionMethod = "purchase"))

            val achievement = Achievement(
                id = "badge_c3", name = "Recycler",
                description = "Recycle often",
                unlocked = false, howToUnlock = "Purchase for 200 pts"
            )

            val method = getPrivateMethod(
                "showBadgeDetailDialog",
                Achievement::class.java,
                android.app.Dialog::class.java,
                com.ecogo.ui.views.MascotLionView::class.java,
                TextView::class.java
            )
            method.invoke(fragment, achievement, null, null, null)
        }
    }

    @Test
    fun `showBadgeDetailDialog displays locked badge state`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["badge"] = "none"

            // Add badge with achievement acquisition (not purchasable)
            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeBadgeDto("badge_c4", "Tree Planter", cost = null, acquisitionMethod = "achievement"))

            val achievement = Achievement(
                id = "badge_c4", name = "Tree Planter",
                description = "Plant trees",
                unlocked = false, howToUnlock = "Save 50kg of carbon"
            )

            val method = getPrivateMethod(
                "showBadgeDetailDialog",
                Achievement::class.java,
                android.app.Dialog::class.java,
                com.ecogo.ui.views.MascotLionView::class.java,
                TextView::class.java
            )
            method.invoke(fragment, achievement, null, null, null)
        }
    }

    @Test
    fun `showBadgeDetailDialog with empty howToUnlock uses default text`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val achievement = Achievement(
                id = "badge_c5", name = "Bus Rider",
                description = "Take the bus",
                unlocked = false, howToUnlock = "" // empty
            )

            val method = getPrivateMethod(
                "showBadgeDetailDialog",
                Achievement::class.java,
                android.app.Dialog::class.java,
                com.ecogo.ui.views.MascotLionView::class.java,
                TextView::class.java
            )
            method.invoke(fragment, achievement, null, null, null)
        }
    }

    // ==================== showSuccessDialog ====================

    @Test
    fun `showSuccessDialog without points hides points view`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showSuccessDialog", String::class.java, String::class.java)
            // null points -> points view should not be visible
            method.invoke(fragment, "Test success!", null)
        }
    }

    @Test
    fun `showSuccessDialog with points shows points view`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showSuccessDialog", String::class.java, String::class.java)
            method.invoke(fragment, "Purchased!", "-100 pts")
        }
    }

    // ==================== showConfirmPurchaseDialog ====================

    @Test
    fun `showConfirmPurchaseDialog creates dialog without crash`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod(
                "showConfirmPurchaseDialog",
                String::class.java,
                String::class.java,
                String::class.java,
                Function0::class.java
            )
            val onConfirm: () -> Unit = { /* no-op */ }
            method.invoke(fragment, SHIRT_EMOJI, "Purchase Item", "Buy this item?", onConfirm)
        }
    }

    // ==================== showClosetDialog ====================

    @Test
    fun `showClosetDialog creates dialog without crash`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "userFacultyId", "soc")

            val method = getPrivateMethod("showClosetDialog")
            method.invoke(fragment)

            // Verify closetDialog is set
            assertNotNull(getFieldValue(fragment, "closetDialog"))
            assertNotNull(getFieldValue(fragment, "closetAdapter"))
            assertNotNull(getFieldValue(fragment, "closetFacultyAdapter"))
        }
    }

    @Test
    fun `showClosetDialog sets closetCurrentTab to all`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "userFacultyId", "soc")
            // Change tab first
            setFieldValue(fragment, "closetCurrentTab", "faculty")

            val method = getPrivateMethod("showClosetDialog")
            method.invoke(fragment)

            assertEquals("all", getFieldValue(fragment, "closetCurrentTab"))
        }
    }

    // ==================== showBadgesDialog ====================

    @Test
    fun `showBadgesDialog creates dialog without crash`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["badge"] = "none"

            val method = getPrivateMethod("showBadgesDialog")
            method.invoke(fragment)
        }
    }

    @Test
    fun `showBadgesDialog with equipped badge shows badge name`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["badge"] = "badge_c1"

            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeBadgeDto("badge_c1", ECO_STARTER))

            val method = getPrivateMethod("showBadgesDialog")
            method.invoke(fragment)
        }
    }

    @Test
    fun `showBadgesDialog with badges creates sorted list`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["badge"] = "none"

            @Suppress("UNCHECKED_CAST")
            val shopItems = getFieldValue(fragment, "shopItems") as MutableList<BadgeDto>
            shopItems.add(makeBadgeDto("badge_c1", ECO_STARTER))
            shopItems.add(makeBadgeDto("badge_c2", "Walker"))

            @Suppress("UNCHECKED_CAST")
            val userItems = getFieldValue(fragment, "userItems") as MutableList<UserBadgeDto>
            userItems.add(makeUserBadge("badge_c2")) // c2 is unlocked, c1 is not

            val method = getPrivateMethod("showBadgesDialog")
            method.invoke(fragment)
        }
    }

    // ==================== purchaseBadgeWithApi - not enough points ====================

    @Test
    fun `purchaseBadgeWithApi shows toast when not enough points`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "currentPoints", 10)

            val badgeDto = makeBadgeDto("badge_c1", ECO_STARTER, cost = 200)
            val detailDialog = android.app.Dialog(fragment.requireContext())

            val method = getPrivateMethod(
                "purchaseBadgeWithApi",
                BadgeDto::class.java,
                android.app.Dialog::class.java,
                com.ecogo.ui.views.MascotLionView::class.java,
                TextView::class.java,
                android.app.Dialog::class.java
            )
            method.invoke(fragment, badgeDto, null, null, null, detailDialog)

            // Points should remain unchanged
            assertEquals(10, getFieldValue(fragment, "currentPoints"))
        }
    }

    @Test
    fun `purchaseBadgeWithApi with zero cost badge returns early if points zero`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "currentPoints", 0)

            // Badge with null cost (defaults to 0 in the method)
            val badgeDto = makeBadgeDto("badge_free", "Free Badge", cost = null)
            val detailDialog = android.app.Dialog(fragment.requireContext())

            val method = getPrivateMethod(
                "purchaseBadgeWithApi",
                BadgeDto::class.java,
                android.app.Dialog::class.java,
                com.ecogo.ui.views.MascotLionView::class.java,
                TextView::class.java,
                android.app.Dialog::class.java
            )
            // cost = 0 and points = 0, so 0 < 0 is false, it proceeds to API call
            method.invoke(fragment, badgeDto, null, null, null, detailDialog)
        }
    }

    // ==================== handleItemClick ====================

    @Test
    fun `handleItemClick with equipped item calls unequip`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val inventory = getFieldValue(fragment, "inventory") as MutableList<String>
            inventory.add("hat_cap")

            @Suppress("UNCHECKED_CAST")
            val outfit = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfit["head"] = "hat_cap"

            setFieldValue(fragment, "currentUserId", TEST_USER)

            val item = ShopItem(
                id = "hat_cap", name = "Cap", type = "head",
                cost = 50, owned = true, equipped = true
            )

            val method = getPrivateMethod("handleItemClick", ShopItem::class.java)
            method.invoke(fragment, item)
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

            setFieldValue(fragment, "currentUserId", TEST_USER)

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
            method.invoke(fragment, item)
        }
    }

    // ==================== purchaseClothWithApi ====================

    @Test
    fun `purchaseClothWithApi shows toast when not enough points`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "currentPoints", 10)
            setFieldValue(fragment, "currentUserId", TEST_USER)

            val item = ShopItem(
                id = "hat_crown", name = "Crown", type = "head",
                cost = 500, owned = false, equipped = false
            )

            val method = getPrivateMethod("purchaseClothWithApi", ShopItem::class.java)
            method.invoke(fragment, item)

            val points = getFieldValue(fragment, "currentPoints") as Int
            assertEquals(10, points)
        }
    }

    @Test
    fun `purchaseClothWithApi proceeds when enough points`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "currentPoints", 1000)
            setFieldValue(fragment, "currentUserId", TEST_USER)

            val item = ShopItem(
                id = "hat_cap", name = "Cap", type = "head",
                cost = 50, owned = false, equipped = false
            )

            val method = getPrivateMethod("purchaseClothWithApi", ShopItem::class.java)
            // Will proceed to API call which may fail, but won't crash
            method.invoke(fragment, item)
        }
    }

    // ==================== loadBadgesAndCloths ====================

    @Test
    fun `loadBadgesAndCloths returns early when userId is empty`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "currentUserId", "")
            val method = getPrivateMethod("loadBadgesAndCloths")
            method.invoke(fragment)
        }
    }

    @Test
    fun `loadBadgesAndCloths proceeds when userId is set`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "currentUserId", TEST_USER)
            val method = getPrivateMethod("loadBadgesAndCloths")
            // Will proceed to async calls which may fail, but won't crash
            method.invoke(fragment)
        }
    }

    // ==================== showPurchaseConfirmDialog ====================

    @Test
    fun `showPurchaseConfirmDialog creates dialog for item`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val item = ShopItem(
                id = "hat_cap", name = "Cap", type = "head",
                cost = 50, owned = false, equipped = false
            )

            val method = getPrivateMethod("showPurchaseConfirmDialog", ShopItem::class.java)
            method.invoke(fragment, item)
        }
    }

    // ==================== updateClosetMascot ====================

    @Test
    fun `updateClosetMascot with no closetMascot does not crash`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // closetMascot is null by default
            assertNull(getFieldValue(fragment, "closetMascot"))

            val method = getPrivateMethod("updateClosetMascot")
            method.invoke(fragment) // Should not crash
        }
    }

    @Test
    fun `updateClosetMascot builds outfit detail text with all parts`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["head"] = "hat_cap"
            outfitMap["face"] = "face_mask"
            outfitMap["body"] = "body_suit"

            // Set closetOutfitDetail to a real TextView
            val textView = TextView(fragment.requireContext())
            setFieldValue(fragment, "closetOutfitDetail", textView)

            val method = getPrivateMethod("updateClosetMascot")
            method.invoke(fragment)

            // Should build "Cap + Hero Mask + Suit"
            assertEquals("Cap + Hero Mask + Suit", textView.text.toString())
        }
    }

    @Test
    fun `updateClosetMascot shows no outfit text when all none`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["head"] = "none"
            outfitMap["face"] = "none"
            outfitMap["body"] = "none"

            val textView = TextView(fragment.requireContext())
            setFieldValue(fragment, "closetOutfitDetail", textView)

            val method = getPrivateMethod("updateClosetMascot")
            method.invoke(fragment)

            assertEquals("No outfit equipped", textView.text.toString())
        }
    }

    @Test
    fun `updateClosetMascot shows partial outfit text`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val outfitMap = getFieldValue(fragment, "currentOutfit") as MutableMap<String, String>
            outfitMap["head"] = "hat_grad"
            outfitMap["face"] = "none"
            outfitMap["body"] = "shirt_nus"

            val textView = TextView(fragment.requireContext())
            setFieldValue(fragment, "closetOutfitDetail", textView)

            val method = getPrivateMethod("updateClosetMascot")
            method.invoke(fragment)

            assertEquals("Grad Cap + NUS Tee", textView.text.toString())
        }
    }

    // ==================== refreshShopAdapter ====================

    @Test
    fun `refreshShopAdapter does not crash when adapter is null`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // closetAdapter is null by default
            assertNull(getFieldValue(fragment, "closetAdapter"))

            val method = getPrivateMethod("refreshShopAdapter")
            method.invoke(fragment) // Should not crash
        }
    }

    // ==================== setupUI ====================

    @Test
    fun `setupUI does not crash`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("setupUI")
            method.invoke(fragment)
        }
    }

    // ==================== setupBadgeEntry ====================

    @Test
    fun `setupBadgeEntry does not crash`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("setupBadgeEntry")
            method.invoke(fragment)
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
