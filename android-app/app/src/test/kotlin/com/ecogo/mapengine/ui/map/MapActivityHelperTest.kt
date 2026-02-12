package com.ecogo.mapengine.ui.map

import com.ecogo.mapengine.data.model.TransportMode
import com.ecogo.mapengine.ml.TransportModeLabel
import org.junit.Assert.*
import org.junit.Test

class MapActivityHelperTest {

    // ==================== mlLabelToDictMode ====================

    @Test
    fun `mlLabelToDictMode WALKING returns walk`() {
        assertEquals("walk", MapActivityHelper.mlLabelToDictMode(TransportModeLabel.WALKING))
    }

    @Test
    fun `mlLabelToDictMode CYCLING returns bike`() {
        assertEquals("bike", MapActivityHelper.mlLabelToDictMode(TransportModeLabel.CYCLING))
    }

    @Test
    fun `mlLabelToDictMode BUS returns bus`() {
        assertEquals("bus", MapActivityHelper.mlLabelToDictMode(TransportModeLabel.BUS))
    }

    @Test
    fun `mlLabelToDictMode SUBWAY returns subway`() {
        assertEquals("subway", MapActivityHelper.mlLabelToDictMode(TransportModeLabel.SUBWAY))
    }

    @Test
    fun `mlLabelToDictMode DRIVING returns car`() {
        assertEquals("car", MapActivityHelper.mlLabelToDictMode(TransportModeLabel.DRIVING))
    }

    @Test
    fun `mlLabelToDictMode UNKNOWN returns walk`() {
        assertEquals("walk", MapActivityHelper.mlLabelToDictMode(TransportModeLabel.UNKNOWN))
    }

    // ==================== isGreenMode ====================

    @Test
    fun `isGreenMode walk returns true`() {
        assertTrue(MapActivityHelper.isGreenMode("walk"))
    }

    @Test
    fun `isGreenMode bike returns true`() {
        assertTrue(MapActivityHelper.isGreenMode("bike"))
    }

    @Test
    fun `isGreenMode bus returns true`() {
        assertTrue(MapActivityHelper.isGreenMode("bus"))
    }

    @Test
    fun `isGreenMode subway returns true`() {
        assertTrue(MapActivityHelper.isGreenMode("subway"))
    }

    @Test
    fun `isGreenMode car returns false`() {
        assertFalse(MapActivityHelper.isGreenMode("car"))
    }

    // ==================== getDominantMode ====================

    @Test
    fun `getDominantMode empty list returns WALKING`() {
        assertEquals(TransportModeLabel.WALKING, MapActivityHelper.getDominantMode(emptyList()))
    }

    @Test
    fun `getDominantMode single segment returns its mode`() {
        val segments = listOf(
            MapActivityHelper.ModeSegment(TransportModeLabel.BUS, 1000L, 5000L)
        )
        assertEquals(TransportModeLabel.BUS, MapActivityHelper.getDominantMode(segments))
    }

    @Test
    fun `getDominantMode returns longest duration mode`() {
        val segments = listOf(
            MapActivityHelper.ModeSegment(TransportModeLabel.WALKING, 0L, 1000L),   // 1s
            MapActivityHelper.ModeSegment(TransportModeLabel.BUS, 1000L, 6000L),     // 5s
            MapActivityHelper.ModeSegment(TransportModeLabel.WALKING, 6000L, 8000L)  // 2s
        )
        assertEquals(TransportModeLabel.BUS, MapActivityHelper.getDominantMode(segments))
    }

    @Test
    fun `getDominantMode groups same mode segments`() {
        val segments = listOf(
            MapActivityHelper.ModeSegment(TransportModeLabel.WALKING, 0L, 2000L),     // 2s
            MapActivityHelper.ModeSegment(TransportModeLabel.BUS, 2000L, 4000L),       // 2s
            MapActivityHelper.ModeSegment(TransportModeLabel.WALKING, 4000L, 7000L)    // 3s = total walking 5s
        )
        assertEquals(TransportModeLabel.WALKING, MapActivityHelper.getDominantMode(segments))
    }

    @Test
    fun `getDominantMode with zero duration segments`() {
        val segments = listOf(
            MapActivityHelper.ModeSegment(TransportModeLabel.CYCLING, 1000L, 1000L), // 0s
            MapActivityHelper.ModeSegment(TransportModeLabel.BUS, 1000L, 2000L)      // 1s
        )
        assertEquals(TransportModeLabel.BUS, MapActivityHelper.getDominantMode(segments))
    }

    // ==================== buildTransportModeSegments ====================

    @Test
    fun `buildTransportModeSegments empty list returns empty`() {
        val result = MapActivityHelper.buildTransportModeSegments(emptyList(), 1000.0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `buildTransportModeSegments single segment covers full distance`() {
        val segments = listOf(
            MapActivityHelper.ModeSegment(TransportModeLabel.WALKING, 0L, 5000L)
        )
        val result = MapActivityHelper.buildTransportModeSegments(segments, 1000.0) // 1000m = 1km
        assertEquals(1, result.size)
        assertEquals("walk", result[0].mode)
        assertEquals(1.0, result[0].subDistance, 0.01) // 1km
        assertEquals(5, result[0].subDuration) // 5 seconds
    }

    @Test
    fun `buildTransportModeSegments two segments split proportionally`() {
        val segments = listOf(
            MapActivityHelper.ModeSegment(TransportModeLabel.WALKING, 0L, 3000L), // 3s
            MapActivityHelper.ModeSegment(TransportModeLabel.BUS, 3000L, 6000L)   // 3s
        )
        val result = MapActivityHelper.buildTransportModeSegments(segments, 2000.0) // 2000m = 2km
        assertEquals(2, result.size)
        assertEquals("walk", result[0].mode)
        assertEquals(1.0, result[0].subDistance, 0.01) // 50% of 2km
        assertEquals("bus", result[1].mode)
        assertEquals(1.0, result[1].subDistance, 0.01) // 50% of 2km
    }

    @Test
    fun `buildTransportModeSegments maps modes correctly`() {
        val segments = listOf(
            MapActivityHelper.ModeSegment(TransportModeLabel.DRIVING, 0L, 1000L)
        )
        val result = MapActivityHelper.buildTransportModeSegments(segments, 5000.0)
        assertEquals("car", result[0].mode)
        assertEquals(5.0, result[0].subDistance, 0.01) // 5km
    }

    // ==================== calculateRealTimeCarbonSaved ====================

    @Test
    fun `calculateRealTimeCarbonSaved walking saves max carbon`() {
        // Walking: 0 emission, driving: 0.15 * 1km = 0.15 kg = 150g
        val result = MapActivityHelper.calculateRealTimeCarbonSaved(1000f, TransportMode.WALKING)
        assertEquals(150.0, result, 0.1)
    }

    @Test
    fun `calculateRealTimeCarbonSaved cycling saves max carbon`() {
        val result = MapActivityHelper.calculateRealTimeCarbonSaved(1000f, TransportMode.CYCLING)
        assertEquals(150.0, result, 0.1)
    }

    @Test
    fun `calculateRealTimeCarbonSaved bus saves some carbon`() {
        // Bus: 0.05 * 1km = 0.05kg, driving: 0.15 * 1km = 0.15kg, saved = (0.15-0.05)*1000 = 100g
        val result = MapActivityHelper.calculateRealTimeCarbonSaved(1000f, TransportMode.BUS)
        assertEquals(100.0, result, 0.1)
    }

    @Test
    fun `calculateRealTimeCarbonSaved subway saves some carbon`() {
        val result = MapActivityHelper.calculateRealTimeCarbonSaved(1000f, TransportMode.SUBWAY)
        assertEquals(100.0, result, 0.1)
    }

    @Test
    fun `calculateRealTimeCarbonSaved driving saves zero`() {
        val result = MapActivityHelper.calculateRealTimeCarbonSaved(1000f, TransportMode.DRIVING)
        assertEquals(0.0, result, 0.1)
    }

    @Test
    fun `calculateRealTimeCarbonSaved null mode uses driving factor`() {
        val result = MapActivityHelper.calculateRealTimeCarbonSaved(1000f, null)
        assertEquals(0.0, result, 0.1)
    }

    @Test
    fun `calculateRealTimeCarbonSaved zero distance returns zero`() {
        val result = MapActivityHelper.calculateRealTimeCarbonSaved(0f, TransportMode.WALKING)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `calculateRealTimeCarbonSaved large distance`() {
        // 10km walking: (0.15 * 10 - 0) * 1000 = 1500g
        val result = MapActivityHelper.calculateRealTimeCarbonSaved(10000f, TransportMode.WALKING)
        assertEquals(1500.0, result, 0.1)
    }

    // ==================== calculateEcoRating ====================

    @Test
    fun `calculateEcoRating zero carbon returns 5 stars`() {
        assertEquals("⭐⭐⭐⭐⭐", MapActivityHelper.calculateEcoRating(0.0, 5.0))
    }

    @Test
    fun `calculateEcoRating very low carbon returns 4 stars`() {
        // 0.02 carbon / 1km = 0.02 per km < 0.03
        assertEquals("⭐⭐⭐⭐", MapActivityHelper.calculateEcoRating(0.02, 1.0))
    }

    @Test
    fun `calculateEcoRating low carbon returns 3 stars`() {
        // 0.04 carbon / 1km = 0.04 per km, >= 0.03, < 0.06
        assertEquals("⭐⭐⭐", MapActivityHelper.calculateEcoRating(0.04, 1.0))
    }

    @Test
    fun `calculateEcoRating medium carbon returns 2 stars`() {
        // 0.08 carbon / 1km = 0.08 per km, >= 0.06, < 0.10
        assertEquals("⭐⭐", MapActivityHelper.calculateEcoRating(0.08, 1.0))
    }

    @Test
    fun `calculateEcoRating high carbon returns 1 star`() {
        // 0.15 carbon / 1km = 0.15 per km, >= 0.10
        assertEquals("⭐", MapActivityHelper.calculateEcoRating(0.15, 1.0))
    }

    @Test
    fun `calculateEcoRating zero distance uses totalCarbon as perKm`() {
        // distance = 0, carbonPerKm = totalCarbon = 0.5, >= 0.10 → 1 star
        assertEquals("⭐", MapActivityHelper.calculateEcoRating(0.5, 0.0))
    }

    @Test
    fun `calculateEcoRating zero distance zero carbon returns 5 stars`() {
        assertEquals("⭐⭐⭐⭐⭐", MapActivityHelper.calculateEcoRating(0.0, 0.0))
    }

    @Test
    fun `calculateEcoRating boundary exactly 0_03 returns 3 stars`() {
        // 0.03 / 1 = 0.03, not < 0.03, so 3 stars check: >= 0.03, < 0.06
        assertEquals("⭐⭐⭐", MapActivityHelper.calculateEcoRating(0.03, 1.0))
    }

    @Test
    fun `calculateEcoRating boundary exactly 0_06 returns 2 stars`() {
        assertEquals("⭐⭐", MapActivityHelper.calculateEcoRating(0.06, 1.0))
    }

    @Test
    fun `calculateEcoRating boundary exactly 0_10 returns 1 star`() {
        assertEquals("⭐", MapActivityHelper.calculateEcoRating(0.10, 1.0))
    }

    // ==================== generateEncouragementMessage ====================

    @Test
    fun `generateEncouragementMessage walking with carbon saved`() {
        // 1km walking → 150g saved
        val msg = MapActivityHelper.generateEncouragementMessage(1000f, TransportMode.WALKING)
        assertTrue(msg.contains("已减碳"))
        assertTrue(msg.contains("150"))
        assertTrue(msg.contains("继续加油"))
    }

    @Test
    fun `generateEncouragementMessage walking very short distance`() {
        // 1m walking → 0.15g saved < 1, so generic message
        val msg = MapActivityHelper.generateEncouragementMessage(1f, TransportMode.WALKING)
        assertEquals("绿色出行 | 继续加油 💪", msg)
    }

    @Test
    fun `generateEncouragementMessage cycling with carbon saved`() {
        val msg = MapActivityHelper.generateEncouragementMessage(2000f, TransportMode.CYCLING)
        assertTrue(msg.contains("已减碳"))
        assertTrue(msg.contains("继续加油"))
    }

    @Test
    fun `generateEncouragementMessage bus with carbon saved`() {
        val msg = MapActivityHelper.generateEncouragementMessage(1000f, TransportMode.BUS)
        assertTrue(msg.contains("绿色出行进行中"))
        assertTrue(msg.contains("已减碳"))
    }

    @Test
    fun `generateEncouragementMessage subway with carbon saved`() {
        val msg = MapActivityHelper.generateEncouragementMessage(1000f, TransportMode.SUBWAY)
        assertTrue(msg.contains("绿色出行进行中"))
    }

    @Test
    fun `generateEncouragementMessage bus very short distance`() {
        val msg = MapActivityHelper.generateEncouragementMessage(1f, TransportMode.BUS)
        assertEquals("绿色出行进行中 🚌", msg)
    }

    @Test
    fun `generateEncouragementMessage driving shows distance`() {
        val msg = MapActivityHelper.generateEncouragementMessage(5000f, TransportMode.DRIVING)
        assertTrue(msg.contains("已行进"))
        assertTrue(msg.contains("5.00"))
    }

    @Test
    fun `generateEncouragementMessage null mode shows distance`() {
        val msg = MapActivityHelper.generateEncouragementMessage(3000f, null)
        assertTrue(msg.contains("已行进"))
        assertTrue(msg.contains("3.00"))
    }

    // ==================== generateMilestoneMessage ====================

    @Test
    fun `generateMilestoneMessage walking`() {
        val msg = MapActivityHelper.generateMilestoneMessage(500f, TransportMode.WALKING)
        assertTrue(msg.contains("步行"))
        assertTrue(msg.contains("500"))
        assertTrue(msg.contains("减碳"))
    }

    @Test
    fun `generateMilestoneMessage cycling`() {
        val msg = MapActivityHelper.generateMilestoneMessage(1000f, TransportMode.CYCLING)
        assertTrue(msg.contains("骑行"))
        assertTrue(msg.contains("1000"))
    }

    @Test
    fun `generateMilestoneMessage bus`() {
        val msg = MapActivityHelper.generateMilestoneMessage(2000f, TransportMode.BUS)
        assertTrue(msg.contains("出行"))
        assertTrue(msg.contains("2000"))
        assertTrue(msg.contains("减碳"))
    }

    @Test
    fun `generateMilestoneMessage subway`() {
        val msg = MapActivityHelper.generateMilestoneMessage(3000f, TransportMode.SUBWAY)
        assertTrue(msg.contains("出行"))
        assertTrue(msg.contains("减碳"))
    }

    @Test
    fun `generateMilestoneMessage driving shows only distance`() {
        val msg = MapActivityHelper.generateMilestoneMessage(5000f, TransportMode.DRIVING)
        assertTrue(msg.contains("出行"))
        assertTrue(msg.contains("5000"))
        assertFalse(msg.contains("减碳"))
    }

    @Test
    fun `generateMilestoneMessage null mode shows only distance`() {
        val msg = MapActivityHelper.generateMilestoneMessage(1000f, null)
        assertTrue(msg.contains("出行"))
        assertFalse(msg.contains("减碳"))
    }

    // ==================== formatElapsedTime ====================

    @Test
    fun `formatElapsedTime zero returns 00 00`() {
        assertEquals("00:00", MapActivityHelper.formatElapsedTime(0L))
    }

    @Test
    fun `formatElapsedTime 30 seconds`() {
        assertEquals("00:30", MapActivityHelper.formatElapsedTime(30_000L))
    }

    @Test
    fun `formatElapsedTime 1 minute`() {
        assertEquals("01:00", MapActivityHelper.formatElapsedTime(60_000L))
    }

    @Test
    fun `formatElapsedTime 5 min 30 sec`() {
        assertEquals("05:30", MapActivityHelper.formatElapsedTime(330_000L))
    }

    @Test
    fun `formatElapsedTime 59 min 59 sec`() {
        assertEquals("59:59", MapActivityHelper.formatElapsedTime(3_599_000L))
    }

    @Test
    fun `formatElapsedTime 1 hour shows hours format`() {
        assertEquals("1:00:00", MapActivityHelper.formatElapsedTime(3_600_000L))
    }

    @Test
    fun `formatElapsedTime 2 hours 15 min 30 sec`() {
        val ms = (2 * 3600 + 15 * 60 + 30) * 1000L
        assertEquals("2:15:30", MapActivityHelper.formatElapsedTime(ms))
    }

    @Test
    fun `formatElapsedTime 10 hours`() {
        assertEquals("10:00:00", MapActivityHelper.formatElapsedTime(36_000_000L))
    }

    // ==================== checkMilestone ====================

    @Test
    fun `checkMilestone no milestones returns null`() {
        assertNull(MapActivityHelper.checkMilestone(500f, emptyList(), emptySet()))
    }

    @Test
    fun `checkMilestone distance below first milestone returns null`() {
        val milestones = listOf(500f, 1000f, 2000f)
        assertNull(MapActivityHelper.checkMilestone(400f, milestones, emptySet()))
    }

    @Test
    fun `checkMilestone reaches first milestone`() {
        val milestones = listOf(500f, 1000f, 2000f)
        assertEquals(500f, MapActivityHelper.checkMilestone(500f, milestones, emptySet()))
    }

    @Test
    fun `checkMilestone already reached milestone returns next`() {
        val milestones = listOf(500f, 1000f, 2000f)
        val reached = setOf(500f)
        assertEquals(1000f, MapActivityHelper.checkMilestone(1500f, milestones, reached))
    }

    @Test
    fun `checkMilestone all milestones reached returns null`() {
        val milestones = listOf(500f, 1000f)
        val reached = setOf(500f, 1000f)
        assertNull(MapActivityHelper.checkMilestone(2000f, milestones, reached))
    }

    @Test
    fun `checkMilestone returns first unreached milestone`() {
        val milestones = listOf(500f, 1000f, 2000f)
        val reached = setOf(500f)
        // distance 2500 exceeds both 1000 and 2000, but returns first unreached (1000)
        assertEquals(1000f, MapActivityHelper.checkMilestone(2500f, milestones, reached))
    }

    // ==================== getModeIcon ====================

    @Test
    fun `getModeIcon WALKING returns pedestrian emoji`() {
        assertEquals("🚶", MapActivityHelper.getModeIcon(TransportModeLabel.WALKING))
    }

    @Test
    fun `getModeIcon CYCLING returns bike emoji`() {
        assertEquals("🚴", MapActivityHelper.getModeIcon(TransportModeLabel.CYCLING))
    }

    @Test
    fun `getModeIcon BUS returns bus emoji`() {
        assertEquals("🚌", MapActivityHelper.getModeIcon(TransportModeLabel.BUS))
    }

    @Test
    fun `getModeIcon SUBWAY returns subway emoji`() {
        assertEquals("🚇", MapActivityHelper.getModeIcon(TransportModeLabel.SUBWAY))
    }

    @Test
    fun `getModeIcon DRIVING returns car emoji`() {
        assertEquals("🚗", MapActivityHelper.getModeIcon(TransportModeLabel.DRIVING))
    }

    @Test
    fun `getModeIcon UNKNOWN returns question mark emoji`() {
        assertEquals("❓", MapActivityHelper.getModeIcon(TransportModeLabel.UNKNOWN))
    }

    // ==================== getModeText ====================

    @Test
    fun `getModeText WALKING returns Chinese text`() {
        assertEquals("步行", MapActivityHelper.getModeText(TransportModeLabel.WALKING))
    }

    @Test
    fun `getModeText CYCLING returns Chinese text`() {
        assertEquals("骑行", MapActivityHelper.getModeText(TransportModeLabel.CYCLING))
    }

    @Test
    fun `getModeText BUS returns Chinese text`() {
        assertEquals("公交", MapActivityHelper.getModeText(TransportModeLabel.BUS))
    }

    @Test
    fun `getModeText SUBWAY returns Chinese text`() {
        assertEquals("地铁", MapActivityHelper.getModeText(TransportModeLabel.SUBWAY))
    }

    @Test
    fun `getModeText DRIVING returns Chinese text`() {
        assertEquals("驾车", MapActivityHelper.getModeText(TransportModeLabel.DRIVING))
    }

    @Test
    fun `getModeText UNKNOWN returns unknown`() {
        assertEquals("未知", MapActivityHelper.getModeText(TransportModeLabel.UNKNOWN))
    }

    // ==================== getRouteTypeText ====================

    @Test
    fun `getRouteTypeText low_carbon returns correct text`() {
        assertEquals("低碳路线", MapActivityHelper.getRouteTypeText("low_carbon"))
    }

    @Test
    fun `getRouteTypeText balanced returns correct text`() {
        assertEquals("平衡路线", MapActivityHelper.getRouteTypeText("balanced"))
    }

    @Test
    fun `getRouteTypeText null returns default text`() {
        assertEquals("推荐路线", MapActivityHelper.getRouteTypeText(null))
    }

    @Test
    fun `getRouteTypeText unknown string returns default text`() {
        assertEquals("推荐路线", MapActivityHelper.getRouteTypeText("fastest"))
    }

    // ==================== getCarbonColorHex ====================

    @Test
    fun `getCarbonColorHex zero returns green`() {
        assertEquals("#4CAF50", MapActivityHelper.getCarbonColorHex(0.0))
    }

    @Test
    fun `getCarbonColorHex low carbon returns light green`() {
        assertEquals("#8BC34A", MapActivityHelper.getCarbonColorHex(0.3))
    }

    @Test
    fun `getCarbonColorHex medium carbon returns yellow`() {
        assertEquals("#FFC107", MapActivityHelper.getCarbonColorHex(1.0))
    }

    @Test
    fun `getCarbonColorHex high carbon returns red`() {
        assertEquals("#FF5722", MapActivityHelper.getCarbonColorHex(2.0))
    }

    @Test
    fun `getCarbonColorHex boundary 0_5 returns yellow`() {
        assertEquals("#FFC107", MapActivityHelper.getCarbonColorHex(0.5))
    }

    @Test
    fun `getCarbonColorHex boundary 1_5 returns red`() {
        assertEquals("#FF5722", MapActivityHelper.getCarbonColorHex(1.5))
    }

    @Test
    fun `getCarbonColorHex just below 0_5 returns light green`() {
        assertEquals("#8BC34A", MapActivityHelper.getCarbonColorHex(0.499))
    }

    // ==================== formatCarbonSavedText ====================

    @Test
    fun `formatCarbonSavedText positive saved shows saving message`() {
        val text = MapActivityHelper.formatCarbonSavedText(1.25, 0.5)
        assertTrue(text.contains("比驾车减少"))
        assertTrue(text.contains("1.25"))
    }

    @Test
    fun `formatCarbonSavedText zero saved shows emission`() {
        val text = MapActivityHelper.formatCarbonSavedText(0.0, 2.5)
        assertTrue(text.contains("碳排放"))
        assertTrue(text.contains("2.50"))
    }

    @Test
    fun `formatCarbonSavedText negative saved shows emission`() {
        val text = MapActivityHelper.formatCarbonSavedText(-0.5, 3.0)
        assertTrue(text.contains("碳排放"))
        assertTrue(text.contains("3.00"))
    }

    // ==================== ModeSegment ====================

    @Test
    fun `ModeSegment default endTime equals startTime`() {
        val segment = MapActivityHelper.ModeSegment(TransportModeLabel.WALKING, 1000L)
        assertEquals(1000L, segment.startTime)
        assertEquals(1000L, segment.endTime)
    }

    @Test
    fun `ModeSegment endTime is mutable`() {
        val segment = MapActivityHelper.ModeSegment(TransportModeLabel.BUS, 1000L)
        segment.endTime = 5000L
        assertEquals(5000L, segment.endTime)
    }

    @Test
    fun `ModeSegment equality check`() {
        val seg1 = MapActivityHelper.ModeSegment(TransportModeLabel.WALKING, 0L, 1000L)
        val seg2 = MapActivityHelper.ModeSegment(TransportModeLabel.WALKING, 0L, 1000L)
        assertEquals(seg1, seg2)
    }
}
