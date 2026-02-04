package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.model.Badge;
import com.example.EcoGo.service.BadgeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BadgeController {

    @Autowired
    private BadgeServiceImpl badgeService;

    // 购买
    @PostMapping("/mobile/badges/{badge_id}/purchase")
    public ResponseMessage<?> purchaseBadge(@PathVariable("badge_id") String badgeId, @RequestBody Map<String, String> payload) {
        try {
            return ResponseMessage.success(badgeService.purchaseBadge(payload.get("user_id"), badgeId));
        } catch (Exception e) {
            return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        }
    }

    // 佩戴/卸下 (含互斥)
    @PutMapping("/mobile/badges/{badge_id}/display")
    public ResponseMessage<?> toggleDisplay(@PathVariable("badge_id") String badgeId, @RequestBody Map<String, Object> payload) {
        try {
            return ResponseMessage.success(badgeService.toggleBadgeDisplay((String)payload.get("user_id"), badgeId, (Boolean)payload.get("is_display")));
        } catch (Exception e) {
            return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        }
    }

    // 商城列表
    @GetMapping("/mobile/badges/shop")
    public ResponseMessage<?> getShopList() {
        return ResponseMessage.success(badgeService.getShopList());
    }

    // 我的背包
    @GetMapping("/mobile/badges/user/{user_id}")
    public ResponseMessage<?> getMyBadges(@PathVariable("user_id") String userId) {
        return ResponseMessage.success(badgeService.getMyBadges(userId));
    }

    // 管理员创建 (用于初始化测试数据)
    @PostMapping("/web/badges")
    public ResponseMessage<?> createBadge(@RequestBody Badge badge) {
        return ResponseMessage.success(badgeService.createBadge(badge));
    }

    // 管理员修改徽章
    @PutMapping("/web/badges/{badge_id}")
    public ResponseMessage<?> updateBadge(@PathVariable("badge_id") String badgeId, @RequestBody Badge badge) {
        try {
            return ResponseMessage.success(badgeService.updateBadge(badgeId, badge));
        } catch (Exception e) {
            return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        }
    }

    // 管理员删除徽章
    @DeleteMapping("/web/badges/{badge_id}")
    public ResponseMessage<?> deleteBadge(@PathVariable("badge_id") String badgeId) {
        try {
            badgeService.deleteBadge(badgeId);
            return ResponseMessage.success("徽章删除成功");
        } catch (Exception e) {
            return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        }
    }

    // 管理员统计 - 获取每个 badge 的购买次数
    @GetMapping("/web/badges/stats/purchases")
    public ResponseMessage<?> getBadgePurchaseStats() {
        return ResponseMessage.success(badgeService.getBadgePurchaseStats());
    }
}