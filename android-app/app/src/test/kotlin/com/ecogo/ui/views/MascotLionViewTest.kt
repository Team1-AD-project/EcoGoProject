package com.ecogo.ui.views

import android.graphics.Canvas
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.ecogo.data.MascotEmotion
import com.ecogo.data.MascotSize
import com.ecogo.data.Outfit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MascotLionViewTest {

    private lateinit var view: MascotLionView

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        view = MascotLionView(context)
    }

    private fun forceDrawCycle() {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val canvas = Canvas()
        view.draw(canvas)
    }

    // ==================== Basic Properties ====================

    @Test
    fun `default outfit is none`() {
        assertEquals(Outfit(), view.outfit)
    }

    @Test
    fun `default emotion is NORMAL`() {
        assertEquals(MascotEmotion.NORMAL, view.currentEmotion)
    }

    @Test
    fun `default size is LARGE`() {
        assertEquals(MascotSize.LARGE, view.mascotSize)
    }

    @Test
    fun `default simplifiedMode is false`() {
        assertFalse(view.simplifiedMode)
    }

    @Test
    fun `setting outfit updates property`() {
        val newOutfit = Outfit(head = "hat_grad", face = "glasses_sun", body = "shirt_nus", badge = "badge_c1")
        view.outfit = newOutfit
        assertEquals(newOutfit, view.outfit)
    }

    @Test
    fun `setting mascotSize updates property`() {
        view.mascotSize = MascotSize.SMALL
        assertEquals(MascotSize.SMALL, view.mascotSize)
    }

    @Test
    fun `setting simplifiedMode updates property`() {
        view.simplifiedMode = true
        assertTrue(view.simplifiedMode)
    }

    @Test
    fun `setEmotion updates currentEmotion`() {
        view.setEmotion(MascotEmotion.HAPPY)
        assertEquals(MascotEmotion.HAPPY, view.currentEmotion)
    }

    // ==================== Draw with Default State ====================

    @Test
    fun `draw with default state does not crash`() {
        forceDrawCycle()
    }

    @Test
    fun `draw with zero size does not crash`() {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.draw(Canvas())
    }

    // ==================== Emotion Drawing ====================

    @Test
    fun `draw NORMAL emotion`() {
        view.setEmotion(MascotEmotion.NORMAL)
        forceDrawCycle()
    }

    @Test
    fun `draw HAPPY emotion`() {
        view.setEmotion(MascotEmotion.HAPPY)
        forceDrawCycle()
    }

    @Test
    fun `draw SAD emotion`() {
        view.setEmotion(MascotEmotion.SAD)
        forceDrawCycle()
    }

    @Test
    fun `draw THINKING emotion`() {
        view.setEmotion(MascotEmotion.THINKING)
        forceDrawCycle()
    }

    @Test
    fun `draw SLEEPING emotion`() {
        view.setEmotion(MascotEmotion.SLEEPING)
        forceDrawCycle()
    }

    @Test
    fun `draw CONFUSED emotion`() {
        view.setEmotion(MascotEmotion.CONFUSED)
        forceDrawCycle()
    }

    @Test
    fun `draw CELEBRATING emotion`() {
        view.setEmotion(MascotEmotion.CELEBRATING)
        forceDrawCycle()
    }

    @Test
    fun `draw WAVING emotion`() {
        view.setEmotion(MascotEmotion.WAVING)
        forceDrawCycle()
    }

    // ==================== Emotion with Simplified Mode ====================

    @Test
    fun `draw THINKING simplified hides bubbles`() {
        view.simplifiedMode = true
        view.setEmotion(MascotEmotion.THINKING)
        forceDrawCycle()
    }

    @Test
    fun `draw SLEEPING simplified hides ZZZ`() {
        view.simplifiedMode = true
        view.setEmotion(MascotEmotion.SLEEPING)
        forceDrawCycle()
    }

    @Test
    fun `draw CONFUSED simplified hides question mark`() {
        view.simplifiedMode = true
        view.setEmotion(MascotEmotion.CONFUSED)
        forceDrawCycle()
    }

    @Test
    fun `draw CELEBRATING simplified hides sparkles`() {
        view.simplifiedMode = true
        view.setEmotion(MascotEmotion.CELEBRATING)
        forceDrawCycle()
    }

    // ==================== Size Variants ====================

    @Test
    fun `draw with SMALL size`() {
        view.mascotSize = MascotSize.SMALL
        forceDrawCycle()
    }

    @Test
    fun `draw with MEDIUM size`() {
        view.mascotSize = MascotSize.MEDIUM
        forceDrawCycle()
    }

    @Test
    fun `draw with LARGE size`() {
        view.mascotSize = MascotSize.LARGE
        forceDrawCycle()
    }

    @Test
    fun `draw with XLARGE size`() {
        view.mascotSize = MascotSize.XLARGE
        forceDrawCycle()
    }

    // ==================== onMeasure Modes ====================

    @Test
    fun `onMeasure EXACTLY sets exact size`() {
        val spec = View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY)
        view.measure(spec, spec)
        assertEquals(300, view.measuredWidth)
        assertEquals(300, view.measuredHeight)
    }

    @Test
    fun `onMeasure AT_MOST constrains to available`() {
        val spec = View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.AT_MOST)
        view.measure(spec, spec)
        assertTrue(view.measuredWidth <= 50)
    }

    @Test
    fun `onMeasure UNSPECIFIED uses desired size`() {
        view.mascotSize = MascotSize.LARGE
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(spec, spec)
        assertTrue(view.measuredWidth > 0)
    }

    @Test
    fun `SMALL size enables simplifiedMode in onMeasure`() {
        view.mascotSize = MascotSize.SMALL
        val spec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        view.measure(spec, spec)
        assertTrue(view.simplifiedMode)
    }

    @Test
    fun `MEDIUM size enables simplifiedMode in onMeasure`() {
        view.mascotSize = MascotSize.MEDIUM
        val spec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        view.measure(spec, spec)
        assertTrue(view.simplifiedMode)
    }

    @Test
    fun `LARGE size disables simplifiedMode in onMeasure`() {
        view.mascotSize = MascotSize.LARGE
        val spec = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        view.measure(spec, spec)
        assertFalse(view.simplifiedMode)
    }

    // ==================== Body Outfits ====================

    @Test
    fun `draw body white shirt`() {
        view.outfit = Outfit(body = "body_white_shirt")
        forceDrawCycle()
    }

    @Test
    fun `draw body NUS tee`() {
        view.outfit = Outfit(body = "shirt_nus")
        forceDrawCycle()
    }

    @Test
    fun `draw body hoodie`() {
        view.outfit = Outfit(body = "shirt_hoodie")
        forceDrawCycle()
    }

    @Test
    fun `draw body plaid shirt`() {
        view.outfit = Outfit(body = "body_plaid")
        forceDrawCycle()
    }

    @Test
    fun `draw body suit`() {
        view.outfit = Outfit(body = "body_suit")
        forceDrawCycle()
    }

    @Test
    fun `draw body lab coat`() {
        view.outfit = Outfit(body = "body_coat")
        forceDrawCycle()
    }

    @Test
    fun `draw body sports jersey`() {
        view.outfit = Outfit(body = "body_sports")
        forceDrawCycle()
    }

    @Test
    fun `draw body kimono`() {
        view.outfit = Outfit(body = "body_kimono")
        forceDrawCycle()
    }

    @Test
    fun `draw body tuxedo`() {
        view.outfit = Outfit(body = "body_tux")
        forceDrawCycle()
    }

    @Test
    fun `draw body superhero cape`() {
        view.outfit = Outfit(body = "body_superhero")
        forceDrawCycle()
    }

    @Test
    fun `draw body doctor coat`() {
        view.outfit = Outfit(body = "body_doctor")
        forceDrawCycle()
    }

    @Test
    fun `draw body pilot uniform`() {
        view.outfit = Outfit(body = "body_pilot")
        forceDrawCycle()
    }

    @Test
    fun `draw body ninja outfit`() {
        view.outfit = Outfit(body = "body_ninja")
        forceDrawCycle()
    }

    @Test
    fun `draw body medical scrubs`() {
        view.outfit = Outfit(body = "body_scrubs")
        forceDrawCycle()
    }

    @Test
    fun `draw body nurse polo`() {
        view.outfit = Outfit(body = "body_polo")
        forceDrawCycle()
    }

    @Test
    fun `draw body none`() {
        view.outfit = Outfit(body = "none")
        forceDrawCycle()
    }

    // ==================== Head Outfits ====================

    @Test
    fun `draw head grad cap`() {
        view.outfit = Outfit(head = "hat_grad")
        forceDrawCycle()
    }

    @Test
    fun `draw head orange cap`() {
        view.outfit = Outfit(head = "hat_cap")
        forceDrawCycle()
    }

    @Test
    fun `draw head safety helmet`() {
        view.outfit = Outfit(head = "hat_helmet")
        forceDrawCycle()
    }

    @Test
    fun `draw head beret`() {
        view.outfit = Outfit(head = "hat_beret")
        forceDrawCycle()
    }

    @Test
    fun `draw head crown`() {
        view.outfit = Outfit(head = "hat_crown")
        forceDrawCycle()
    }

    @Test
    fun `draw head party hat`() {
        view.outfit = Outfit(head = "hat_party")
        forceDrawCycle()
    }

    @Test
    fun `draw head beanie`() {
        view.outfit = Outfit(head = "hat_beanie")
        forceDrawCycle()
    }

    @Test
    fun `draw head cowboy hat`() {
        view.outfit = Outfit(head = "hat_cowboy")
        forceDrawCycle()
    }

    @Test
    fun `draw head chef hat`() {
        view.outfit = Outfit(head = "hat_chef")
        forceDrawCycle()
    }

    @Test
    fun `draw head wizard hat`() {
        view.outfit = Outfit(head = "hat_wizard")
        forceDrawCycle()
    }

    @Test
    fun `draw head none`() {
        view.outfit = Outfit(head = "none")
        forceDrawCycle()
    }

    // ==================== Face Outfits ====================

    @Test
    fun `draw face square glasses`() {
        view.outfit = Outfit(face = "face_glasses_square")
        forceDrawCycle()
    }

    @Test
    fun `draw face sunglasses`() {
        view.outfit = Outfit(face = "glasses_sun")
        forceDrawCycle()
    }

    @Test
    fun `draw face safety goggles`() {
        view.outfit = Outfit(face = "face_goggles")
        forceDrawCycle()
    }

    @Test
    fun `draw face nerd glasses`() {
        view.outfit = Outfit(face = "glasses_nerd")
        forceDrawCycle()
    }

    @Test
    fun `draw face 3D glasses`() {
        view.outfit = Outfit(face = "glasses_3d")
        forceDrawCycle()
    }

    @Test
    fun `draw face superhero mask`() {
        view.outfit = Outfit(face = "face_mask")
        forceDrawCycle()
    }

    @Test
    fun `draw face monocle`() {
        view.outfit = Outfit(face = "face_monocle")
        forceDrawCycle()
    }

    @Test
    fun `draw face scarf`() {
        view.outfit = Outfit(face = "face_scarf")
        forceDrawCycle()
    }

    @Test
    fun `draw face VR headset`() {
        view.outfit = Outfit(face = "face_vr")
        forceDrawCycle()
    }

    @Test
    fun `draw face none`() {
        view.outfit = Outfit(face = "none")
        forceDrawCycle()
    }

    // ==================== Badge ====================

    @Test
    fun `draw badge none does not crash`() {
        view.outfit = Outfit(badge = "none")
        forceDrawCycle()
    }

    @Test
    fun `draw badge empty does not crash`() {
        view.outfit = Outfit(badge = "")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c1`() {
        view.outfit = Outfit(badge = "badge_c1")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c2`() {
        view.outfit = Outfit(badge = "badge_c2")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c3`() {
        view.outfit = Outfit(badge = "badge_c3")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c4`() {
        view.outfit = Outfit(badge = "badge_c4")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c5`() {
        view.outfit = Outfit(badge = "badge_c5")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c6`() {
        view.outfit = Outfit(badge = "badge_c6")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c7`() {
        view.outfit = Outfit(badge = "badge_c7")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c8`() {
        view.outfit = Outfit(badge = "badge_c8")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c9`() {
        view.outfit = Outfit(badge = "badge_c9")
        forceDrawCycle()
    }

    @Test
    fun `draw badge_c10`() {
        view.outfit = Outfit(badge = "badge_c10")
        forceDrawCycle()
    }

    @Test
    fun `draw legacy badge a1`() {
        view.outfit = Outfit(badge = "a1")
        forceDrawCycle()
    }

    @Test
    fun `draw legacy badge number 5`() {
        view.outfit = Outfit(badge = "5")
        forceDrawCycle()
    }

    @Test
    fun `draw unknown badge falls back`() {
        view.outfit = Outfit(badge = "unknown_badge")
        forceDrawCycle()
    }

    // ==================== Full Outfit Combinations ====================

    @Test
    fun `draw full outfit with all slots`() {
        view.outfit = Outfit(
            head = "hat_grad", face = "glasses_sun",
            body = "shirt_nus", badge = "badge_c1"
        )
        forceDrawCycle()
    }

    @Test
    fun `draw full outfit science`() {
        view.outfit = Outfit(
            head = "hat_helmet", face = "face_goggles",
            body = "body_coat", badge = "badge_c3"
        )
        forceDrawCycle()
    }

    @Test
    fun `draw full outfit business`() {
        view.outfit = Outfit(
            head = "hat_beret", face = "face_monocle",
            body = "body_suit", badge = "badge_c10"
        )
        forceDrawCycle()
    }

    @Test
    fun `draw full outfit ninja`() {
        view.outfit = Outfit(
            head = "hat_wizard", face = "face_mask",
            body = "body_ninja", badge = "badge_c7"
        )
        forceDrawCycle()
    }

    @Test
    fun `draw full outfit medical`() {
        view.outfit = Outfit(
            head = "hat_chef", face = "face_vr",
            body = "body_doctor", badge = "badge_c9"
        )
        forceDrawCycle()
    }

    // ==================== Emotion + Outfit Combinations ====================

    @Test
    fun `draw SAD emotion with full outfit`() {
        view.setEmotion(MascotEmotion.SAD)
        view.outfit = Outfit(head = "hat_crown", face = "face_scarf", body = "body_kimono", badge = "a5")
        forceDrawCycle()
    }

    @Test
    fun `draw CELEBRATING emotion with outfit`() {
        view.setEmotion(MascotEmotion.CELEBRATING)
        view.outfit = Outfit(head = "hat_party", body = "body_superhero", badge = "badge_c8")
        forceDrawCycle()
    }

    @Test
    fun `draw THINKING with outfit simplified`() {
        view.simplifiedMode = true
        view.setEmotion(MascotEmotion.THINKING)
        view.outfit = Outfit(head = "hat_cowboy", face = "glasses_3d", body = "body_pilot")
        forceDrawCycle()
    }

    @Test
    fun `draw SLEEPING with outfit simplified`() {
        view.simplifiedMode = true
        view.setEmotion(MascotEmotion.SLEEPING)
        view.outfit = Outfit(head = "hat_beanie", face = "glasses_nerd", body = "body_sports")
        forceDrawCycle()
    }

    @Test
    fun `draw CONFUSED with full outfit not simplified`() {
        view.simplifiedMode = false
        view.setEmotion(MascotEmotion.CONFUSED)
        view.outfit = Outfit(head = "hat_cap", face = "face_glasses_square", body = "body_tux", badge = "10")
        forceDrawCycle()
    }

    // ==================== Animations ====================

    @Test
    fun `celebrateAnimation sets CELEBRATING emotion`() {
        view.celebrateAnimation()
        assertEquals(MascotEmotion.CELEBRATING, view.currentEmotion)
    }

    @Test
    fun `waveAnimation sets WAVING emotion`() {
        view.waveAnimation()
        assertEquals(MascotEmotion.WAVING, view.currentEmotion)
    }

    @Test
    fun `draw after celebrateAnimation does not crash`() {
        view.celebrateAnimation()
        forceDrawCycle()
    }

    @Test
    fun `draw after waveAnimation does not crash`() {
        view.waveAnimation()
        forceDrawCycle()
    }

    // ==================== Edge Cases ====================

    @Test
    fun `draw with unknown body outfit does not crash`() {
        view.outfit = Outfit(body = "unknown_body")
        forceDrawCycle()
    }

    @Test
    fun `draw with unknown head outfit does not crash`() {
        view.outfit = Outfit(head = "unknown_head")
        forceDrawCycle()
    }

    @Test
    fun `draw with unknown face outfit does not crash`() {
        view.outfit = Outfit(face = "unknown_face")
        forceDrawCycle()
    }

    @Test
    fun `click triggers happy animation`() {
        forceDrawCycle()
        view.performClick()
        forceDrawCycle()
    }

    @Test
    fun `all badge legacy IDs`() {
        listOf("a2", "a3", "a4", "a6", "a7", "a8", "a9", "a10",
               "1", "2", "3", "4", "6", "7", "8", "9").forEach { badgeId ->
            view.outfit = Outfit(badge = badgeId)
            forceDrawCycle()
        }
    }

    @Test
    fun `draw with AT_MOST measure spec`() {
        view.mascotSize = MascotSize.XLARGE
        val widthSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.draw(Canvas())
    }

    @Test
    fun `rapid outfit changes do not crash`() {
        val outfits = listOf(
            Outfit(body = "shirt_nus"),
            Outfit(body = "body_suit", head = "hat_grad"),
            Outfit(face = "glasses_sun", badge = "badge_c5"),
            Outfit()
        )
        outfits.forEach { outfit ->
            view.outfit = outfit
            forceDrawCycle()
        }
    }

    @Test
    fun `rapid emotion changes do not crash`() {
        MascotEmotion.values().forEach { emotion ->
            view.setEmotion(emotion)
            forceDrawCycle()
        }
    }
}
