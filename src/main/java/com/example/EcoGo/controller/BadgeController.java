package com.example.EcoGo.controller;

import com.example.EcoGo.dto.BadgeDto;
import com.example.EcoGo.dto.BadgePurchaseStatDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.BadgeService;
import com.example.EcoGo.model.Badge;
import com.example.EcoGo.model.UserBadge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BadgeController {

    @Autowired
    private BadgeService badgeService;

    // 购买
    @PostMapping("/mobile/badges/{badge_id}/purchase")
    public ResponseMessage<UserBadge> purchaseBadge(@PathVariable("badge_id") String badgeId, @RequestBody Map<String, String> payload) {
        try {
            return ResponseMessage.success(badgeService.purchaseBadge(payload.get("user_id"), badgeId));
        } catch (Exception e) {
            return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        }
    }

    // 佩戴/卸下 (含互斥)
    @PutMapping("/mobile/badges/{badge_id}/display")
    public ResponseMessage<UserBadge> toggleDisplay(@PathVariable("badge_id") String badgeId, @RequestBody Map<String, Object> payload) {
        try {
            return ResponseMessage.success(badgeService.toggleBadgeDisplay((String)payload.get("user_id"), badgeId, (Boolean)payload.get("is_display")));
        } catch (Exception e) {
            return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        }
    }

    // 商城列表
    @GetMapping("/mobile/badges/shop")
    public ResponseMessage<List<Badge>> getShopList() {
        return ResponseMessage.success(badgeService.getShopList());
    }

    // 我的背包
    @GetMapping("/mobile/badges/user/{user_id}")
    public ResponseMessage<List<UserBadge>> getMyBadges(@PathVariable("user_id") String userId) {
        return ResponseMessage.success(badgeService.getMyBadges(userId));
    }

    // 管理员获取所有徽章（支持按category过滤）
    @GetMapping("/web/badges")
    public ResponseMessage<List<Badge>> getAllBadges(@RequestParam(required = false) String category) {
        return ResponseMessage.success(badgeService.getAllBadges(category));
    }

    // 管理员创建 (用于初始化测试数据)
    @PostMapping("/web/badges")
    public ResponseMessage<Badge> createBadge(@RequestBody BadgeDto badgeDto) {
        return ResponseMessage.success(badgeService.createBadge(badgeDto.toEntity()));
    }

    // 管理员修改徽章
    @PutMapping("/web/badges/{badge_id}")
    public ResponseMessage<Badge> updateBadge(@PathVariable("badge_id") String badgeId, @RequestBody BadgeDto badgeDto) {
        try {
            return ResponseMessage.success(badgeService.updateBadge(badgeId, badgeDto.toEntity()));
        } catch (Exception e) {
            return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        }
    }

    // 管理员删除徽章
    @DeleteMapping("/web/badges/{badge_id}")
    public ResponseMessage<String> deleteBadge(@PathVariable("badge_id") String badgeId) {
        try {
            badgeService.deleteBadge(badgeId);
            return ResponseMessage.success("徽章删除成功");
        } catch (Exception e) {
            return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        }
    }

    // 按子分类查询徽章
    @GetMapping("/mobile/badges/sub-category/{sub_category}")
    public ResponseMessage<List<Badge>> getBadgesBySubCategory(@PathVariable("sub_category") String subCategory) {
        return ResponseMessage.success(badgeService.getBadgesBySubCategory(subCategory));
    }

    // 按获取方式查询徽章
    @GetMapping("/mobile/badges/acquisition-method/{method}")
    public ResponseMessage<List<Badge>> getBadgesByAcquisitionMethod(@PathVariable("method") String method) {
        return ResponseMessage.success(badgeService.getBadgesByAcquisitionMethod(method));
    }

    // 管理员统计 - 获取每个 badge 的购买次数
    @GetMapping("/web/badges/stats/purchases")
    public ResponseMessage<List<BadgePurchaseStatDto>> getBadgePurchaseStats() {
        return ResponseMessage.success(badgeService.getBadgePurchaseStats());
    }
}
