package com.example.EcoGo.controller;

import com.example.EcoGo.model.Goods;
import com.example.EcoGo.service.GoodsService;
import com.example.EcoGo.dto.BatchStockUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/goods")
@CrossOrigin(origins = "*")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    // 1. 获取所有商品
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllGoods(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isForRedemption) {
        
        try {
            List<Goods> goodsList = goodsService.getAllGoods();
            
            // 筛选逻辑
            if (category != null && !category.isEmpty()) {
                goodsList = goodsList.stream()
                    .filter(goods -> category.equals(goods.getCategory()))
                    .collect(Collectors.toList());
            }
            
            if (keyword != null && !keyword.isEmpty()) {
                goodsList = goodsList.stream()
                    .filter(goods -> 
                        goods.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                        (goods.getDescription() != null && goods.getDescription().toLowerCase().contains(keyword.toLowerCase()))
                    )
                    .collect(Collectors.toList());
            }
            
            if (isForRedemption != null) {
                goodsList = goodsList.stream()
                    .filter(goods -> isForRedemption.equals(goods.getIsForRedemption()))
                    .collect(Collectors.toList());
            }
            
            // 分页逻辑
            int total = goodsList.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            if (fromIndex < total) {
                goodsList = goodsList.subList(fromIndex, toIndex);
            } else {
                goodsList = List.of();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取商品列表成功");
            response.put("data", goodsList);
            response.put("pagination", Map.of(
                "page", page,
                "size", size,
                "total", total,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取商品列表失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 2. 获取单个商品详情
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getGoodsById(@PathVariable String id) {
        try {
            Goods goods = goodsService.getGoodsById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取商品成功");
            response.put("data", goods);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 404);
            errorResponse.put("message", "商品未找到");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取商品失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 3. 创建商品
    @PostMapping
    public ResponseEntity<Map<String, Object>> createGoods(@RequestBody Goods goods) {
        try {
            Goods createdGoods = goodsService.createGoods(goods);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 201);
            response.put("message", "商品创建成功");
            response.put("data", createdGoods);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", "创建商品失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // 4. 更新商品
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateGoods(@PathVariable String id, @RequestBody Goods goods) {
        try {
            Goods updatedGoods = goodsService.updateGoods(id, goods);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "商品更新成功");
            response.put("data", updatedGoods);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 404);
            errorResponse.put("message", "商品未找到");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", "更新商品失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // 5. 删除商品
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteGoods(@PathVariable String id) {
        try {
            goodsService.deleteGoods(id);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "商品删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "删除商品失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 6. 删除所有商品
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAllGoods() {
        try {
            goodsService.deleteAllGoods();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "所有商品已删除");
            response.put("data", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "删除所有商品失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 7. 插入测试数据
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> insertTestData() {
        try {
            goodsService.insertTestData();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "测试数据插入成功");
            response.put("data", goodsService.getAllGoods());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "插入测试数据失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 8. 批量更新商品库存
    @PutMapping("/batch-stock")
    public ResponseEntity<Map<String, Object>> batchUpdateStock(@RequestBody BatchStockUpdateRequest request) {
        try {
            goodsService.batchUpdateStock(request);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "批量更新库存成功");
            response.put("data", null);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", "批量更新库存失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "服务器内部错误");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 9. Mobile端专用 - 获取可兑换商品
    @GetMapping("/mobile/redemption")
    public ResponseEntity<Map<String, Object>> getRedemptionGoodsForMobile(
            @RequestParam(required = false) Integer vipLevel) {
        try {
            List<Goods> allGoods = goodsService.getAllGoods();
            
            // 筛选可兑换商品
            List<Goods> redemptionGoods = allGoods.stream()
                .filter(goods -> Boolean.TRUE.equals(goods.getIsForRedemption()))
                .filter(goods -> goods.getStock() > 0)
                .filter(goods -> {
                    if (vipLevel != null) {
                        return goods.getVipLevelRequired() <= vipLevel;
                    }
                    return true;
                })
                .collect(Collectors.toList());
            
            // 简化返回数据
            List<Map<String, Object>> simplifiedGoods = redemptionGoods.stream()
                .map(goods -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", goods.getId());
                    item.put("name", goods.getName());
                    item.put("description", goods.getDescription());
                    item.put("imageUrl", goods.getImageUrl());
                    item.put("redemptionPoints", goods.getRedemptionPoints());
                    item.put("stock", goods.getStock());
                    item.put("vipLevelRequired", goods.getVipLevelRequired());
                    return item;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取可兑换商品成功");
            response.put("data", simplifiedGoods);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取商品失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}