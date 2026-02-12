package com.example.EcoGo.controller;

import com.example.EcoGo.dto.BadgeDto;
import com.example.EcoGo.dto.BadgePurchaseStatDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.BadgeService;
import com.example.EcoGo.model.Badge;
import com.example.EcoGo.model.UserBadge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BadgeControllerTest {

    private BadgeService badgeService;
    private BadgeController controller;

    @BeforeEach
    void setUp() throws Exception {
        badgeService = mock(BadgeService.class);
        controller = new BadgeController();

        Field f = BadgeController.class.getDeclaredField("badgeService");
        f.setAccessible(true);
        f.set(controller, badgeService);
    }

    // ---------- helpers ----------

    private static Badge buildBadge(String badgeId, String enName, String category, Integer cost) {
        Badge b = new Badge();
        b.setBadgeId(badgeId);
        b.setName(Map.of("en", enName));
        b.setCategory(category);
        b.setPurchaseCost(cost);
        b.setIsActive(true);
        b.setCreatedAt(new Date());
        return b;
    }

    private static UserBadge buildUserBadge(String userId, String badgeId, boolean isDisplay) {
        UserBadge ub = new UserBadge();
        ub.setUserId(userId);
        ub.setBadgeId(badgeId);
        ub.setDisplay(isDisplay);
        ub.setUnlockedAt(new Date());
        ub.setCreatedAt(new Date());
        return ub;
    }

    // ========== purchaseBadge ==========

    @Test
    void purchaseBadge_success() {
        UserBadge ub = buildUserBadge("user1", "badge1", false);
        when(badgeService.purchaseBadge("user1", "badge1")).thenReturn(ub);

        Map<String, String> payload = Map.of("user_id", "user1");
        ResponseMessage<?> resp = controller.purchaseBadge("badge1", payload);

        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
        verify(badgeService).purchaseBadge("user1", "badge1");
    }

    @Test
    void purchaseBadge_alreadyOwned() {
        when(badgeService.purchaseBadge("user1", "badge1"))
                .thenThrow(new RuntimeException("您已拥有该徽章"));

        Map<String, String> payload = Map.of("user_id", "user1");
        ResponseMessage<?> resp = controller.purchaseBadge("badge1", payload);

        assertEquals(400, resp.getCode());
        assertEquals("您已拥有该徽章", resp.getMessage());
    }

    @Test
    void purchaseBadge_badgeNotFound() {
        when(badgeService.purchaseBadge("user1", "nonexistent"))
                .thenThrow(new RuntimeException("商品不存在"));

        Map<String, String> payload = Map.of("user_id", "user1");
        ResponseMessage<?> resp = controller.purchaseBadge("nonexistent", payload);

        assertEquals(400, resp.getCode());
        assertEquals("商品不存在", resp.getMessage());
    }

    @Test
    void purchaseBadge_notPurchasable() {
        when(badgeService.purchaseBadge("user1", "badge_achievement"))
                .thenThrow(new RuntimeException("该徽章不可通过购买获得"));

        Map<String, String> payload = Map.of("user_id", "user1");
        ResponseMessage<?> resp = controller.purchaseBadge("badge_achievement", payload);

        assertEquals(400, resp.getCode());
        assertEquals("该徽章不可通过购买获得", resp.getMessage());
    }

    // ========== toggleDisplay ==========

    @Test
    void toggleDisplay_equipSuccess() {
        UserBadge ub = buildUserBadge("user1", "badge1", true);
        when(badgeService.toggleBadgeDisplay("user1", "badge1", true)).thenReturn(ub);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", "user1");
        payload.put("is_display", true);
        ResponseMessage<?> resp = controller.toggleDisplay("badge1", payload);

        assertEquals(200, resp.getCode());
        verify(badgeService).toggleBadgeDisplay("user1", "badge1", true);
    }

    @Test
    void toggleDisplay_unequipSuccess() {
        UserBadge ub = buildUserBadge("user1", "badge1", false);
        when(badgeService.toggleBadgeDisplay("user1", "badge1", false)).thenReturn(ub);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", "user1");
        payload.put("is_display", false);
        ResponseMessage<?> resp = controller.toggleDisplay("badge1", payload);

        assertEquals(200, resp.getCode());
        verify(badgeService).toggleBadgeDisplay("user1", "badge1", false);
    }

    @Test
    void toggleDisplay_badgeNotOwned() {
        when(badgeService.toggleBadgeDisplay("user1", "badge1", true))
                .thenThrow(new RuntimeException("您还没有获得这个徽章"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", "user1");
        payload.put("is_display", true);
        ResponseMessage<?> resp = controller.toggleDisplay("badge1", payload);

        assertEquals(400, resp.getCode());
        assertEquals("您还没有获得这个徽章", resp.getMessage());
    }

    // ========== getShopList ==========

    @Test
    void getShopList_success() {
        Badge b1 = buildBadge("b1", "Green Walker", "badge", 100);
        Badge b2 = buildBadge("b2", "Eco Rider", "badge", 200);
        when(badgeService.getShopList()).thenReturn(List.of(b1, b2));

        ResponseMessage<?> resp = controller.getShopList();

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<Badge> data = (List<Badge>) resp.getData();
        assertEquals(2, data.size());
    }

    @Test
    void getShopList_empty() {
        when(badgeService.getShopList()).thenReturn(List.of());

        ResponseMessage<?> resp = controller.getShopList();

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<Badge> data = (List<Badge>) resp.getData();
        assertTrue(data.isEmpty());
    }

    // ========== getMyBadges ==========

    @Test
    void getMyBadges_success() {
        UserBadge ub1 = buildUserBadge("user1", "b1", true);
        UserBadge ub2 = buildUserBadge("user1", "b2", false);
        when(badgeService.getMyBadges("user1")).thenReturn(List.of(ub1, ub2));

        ResponseMessage<?> resp = controller.getMyBadges("user1");

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<UserBadge> data = (List<UserBadge>) resp.getData();
        assertEquals(2, data.size());
    }

    @Test
    void getMyBadges_empty() {
        when(badgeService.getMyBadges("user1")).thenReturn(List.of());

        ResponseMessage<?> resp = controller.getMyBadges("user1");

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<UserBadge> data = (List<UserBadge>) resp.getData();
        assertTrue(data.isEmpty());
    }

    // ========== getAllBadges (admin) ==========

    @Test
    void getAllBadges_noFilter() {
        Badge b1 = buildBadge("b1", "Badge1", "badge", 100);
        Badge b2 = buildBadge("b2", "Cloth1", "cloth", 50);
        when(badgeService.getAllBadges(null)).thenReturn(List.of(b1, b2));

        ResponseMessage<?> resp = controller.getAllBadges(null);

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<Badge> data = (List<Badge>) resp.getData();
        assertEquals(2, data.size());
    }

    @Test
    void getAllBadges_filterByCategory() {
        Badge b1 = buildBadge("b1", "Badge1", "badge", 100);
        when(badgeService.getAllBadges("badge")).thenReturn(List.of(b1));

        ResponseMessage<?> resp = controller.getAllBadges("badge");

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<Badge> data = (List<Badge>) resp.getData();
        assertEquals(1, data.size());
    }

    // ========== createBadge (admin) ==========

    @Test
    void createBadge_success() {
        Badge returned = buildBadge("b_new", "New Badge", "badge", 150);
        when(badgeService.createBadge(any(Badge.class))).thenReturn(returned);

        BadgeDto input = new BadgeDto();
        input.setBadgeId("b_new");
        input.setName(Map.of("en", "New Badge"));
        input.setCategory("badge");
        input.setPurchaseCost(150);
        input.setIsActive(true);
        ResponseMessage<Badge> resp = controller.createBadge(input);

        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
        verify(badgeService).createBadge(any(Badge.class));
    }

    // ========== updateBadge (admin) ==========

    @Test
    void updateBadge_success() {
        Badge updated = buildBadge("b1", "Updated Badge", "badge", 200);
        when(badgeService.updateBadge(eq("b1"), any(Badge.class))).thenReturn(updated);

        BadgeDto input = new BadgeDto();
        input.setName(Map.of("en", "Updated Badge"));
        ResponseMessage<Badge> resp = controller.updateBadge("b1", input);

        assertEquals(200, resp.getCode());
        verify(badgeService).updateBadge(eq("b1"), any(Badge.class));
    }

    @Test
    void updateBadge_notFound() {
        when(badgeService.updateBadge(eq("nonexistent"), any(Badge.class)))
                .thenThrow(new RuntimeException("徽章不存在"));

        BadgeDto input = new BadgeDto();
        input.setName(Map.of("en", "Updated"));
        ResponseMessage<Badge> resp = controller.updateBadge("nonexistent", input);

        assertEquals(400, resp.getCode());
        assertEquals("徽章不存在", resp.getMessage());
    }

    // ========== deleteBadge (admin) ==========

    @Test
    void deleteBadge_success() {
        doNothing().when(badgeService).deleteBadge("b1");

        ResponseMessage<?> resp = controller.deleteBadge("b1");

        assertEquals(200, resp.getCode());
        assertEquals("徽章删除成功", resp.getData());
        verify(badgeService).deleteBadge("b1");
    }

    @Test
    void deleteBadge_notFound() {
        doThrow(new RuntimeException("徽章不存在")).when(badgeService).deleteBadge("nonexistent");

        ResponseMessage<?> resp = controller.deleteBadge("nonexistent");

        assertEquals(400, resp.getCode());
        assertEquals("徽章不存在", resp.getMessage());
    }

    // ========== getBadgesBySubCategory ==========

    @Test
    void getBadgesBySubCategory_success() {
        Badge b = buildBadge("b1", "Rank Badge", "badge", 100);
        b.setSubCategory("RANK");
        when(badgeService.getBadgesBySubCategory("RANK")).thenReturn(List.of(b));

        ResponseMessage<?> resp = controller.getBadgesBySubCategory("RANK");

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<Badge> data = (List<Badge>) resp.getData();
        assertEquals(1, data.size());
    }

    @Test
    void getBadgesBySubCategory_empty() {
        when(badgeService.getBadgesBySubCategory("NONEXISTENT")).thenReturn(List.of());

        ResponseMessage<?> resp = controller.getBadgesBySubCategory("NONEXISTENT");

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<Badge> data = (List<Badge>) resp.getData();
        assertTrue(data.isEmpty());
    }

    // ========== getBadgesByAcquisitionMethod ==========

    @Test
    void getBadgesByAcquisitionMethod_success() {
        Badge b = buildBadge("b1", "Achievement Badge", "badge", 0);
        b.setAcquisitionMethod("achievement");
        when(badgeService.getBadgesByAcquisitionMethod("achievement")).thenReturn(List.of(b));

        ResponseMessage<?> resp = controller.getBadgesByAcquisitionMethod("achievement");

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<Badge> data = (List<Badge>) resp.getData();
        assertEquals(1, data.size());
    }

    @Test
    void getBadgesByAcquisitionMethod_empty() {
        when(badgeService.getBadgesByAcquisitionMethod("reward")).thenReturn(List.of());

        ResponseMessage<?> resp = controller.getBadgesByAcquisitionMethod("reward");

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<Badge> data = (List<Badge>) resp.getData();
        assertTrue(data.isEmpty());
    }

    // ========== getBadgePurchaseStats (admin) ==========

    @Test
    void getBadgePurchaseStats_success() {
        BadgePurchaseStatDto stat1 = new BadgePurchaseStatDto();
        stat1.setBadgeId("b1");
        stat1.setPurchaseCount(10);
        BadgePurchaseStatDto stat2 = new BadgePurchaseStatDto();
        stat2.setBadgeId("b2");
        stat2.setPurchaseCount(5);
        when(badgeService.getBadgePurchaseStats()).thenReturn(List.of(stat1, stat2));

        ResponseMessage<?> resp = controller.getBadgePurchaseStats();

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<BadgePurchaseStatDto> data = (List<BadgePurchaseStatDto>) resp.getData();
        assertEquals(2, data.size());
        assertEquals(10, data.get(0).getPurchaseCount());
    }

    @Test
    void getBadgePurchaseStats_empty() {
        when(badgeService.getBadgePurchaseStats()).thenReturn(List.of());

        ResponseMessage<?> resp = controller.getBadgePurchaseStats();

        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        List<BadgePurchaseStatDto> data = (List<BadgePurchaseStatDto>) resp.getData();
        assertTrue(data.isEmpty());
    }
}
