package com.example.EcoGo.controller;

import com.example.EcoGo.dto.BatchStockUpdateRequest;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.Goods;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
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

    // 1. 获取所有商品（支持筛选 + 分页）
    @GetMapping
    public ResponseMessage<Map<String, Object>> getAllGoods(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isForRedemption
    ) {

        List<Goods> goodsList = goodsService.getAllGoods();

        // 筛选逻辑
        if (category != null && !category.isEmpty()) {
            goodsList = goodsList.stream()
                    .filter(goods -> category.equals(goods.getCategory()))
                    .collect(Collectors.toList());
        }

        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            goodsList = goodsList.stream()
                    .filter(goods ->
                            goods.getName() != null && goods.getName().toLowerCase().contains(kw)
                                    || (goods.getDescription() != null && goods.getDescription().toLowerCase().contains(kw))
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
        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(fromIndex + size, total);

        List<Goods> pageItems = (fromIndex < total) ? goodsList.subList(fromIndex, toIndex) : List.of();

        Map<String, Object> data = new HashMap<>();
        data.put("items", pageItems);
        data.put("pagination", Map.of(
                "page", page,
                "size", size,
                "total", total,
                "totalPages", (int) Math.ceil((double) total / size)
        ));

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    // 2. 获取单个商品详情
    @GetMapping("/{id}")
    public ResponseMessage<Goods> getGoodsById(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "id");
        }

        try {
            Goods goods = goodsService.getGoodsById(id);
            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), goods);
        } catch (BusinessException be) {
            throw be; // 已经是标准错误码，直接抛给全局处理器
        } catch (RuntimeException re) {
            // 你原逻辑：RuntimeException => 404
            throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST, id);
        }
    }

    // 3. 创建商品
    @PostMapping
    public ResponseMessage<Goods> createGoods(@RequestBody Goods goods) {
        if (goods == null) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "goods");
        }

        try {
            Goods createdGoods = goodsService.createGoods(goods);
            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), createdGoods);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR);
        }
    }

    // 4. 更新商品
    @PutMapping("/{id}")
    public ResponseMessage<Goods> updateGoods(@PathVariable String id, @RequestBody Goods goods) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "id");
        }
        if (goods == null) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "goods");
        }

        try {
            Goods updatedGoods = goodsService.updateGoods(id, goods);
            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), updatedGoods);
        } catch (BusinessException be) {
            throw be;
        } catch (RuntimeException re) {
            // 你原逻辑：RuntimeException => 404
            throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST, id);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR);
        }
    }

    // 5. 删除商品
    @DeleteMapping("/{id}")
    public ResponseMessage<Void> deleteGoods(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "id");
        }

        try {
            goodsService.deleteGoods(id);
            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
        } catch (BusinessException be) {
            throw be;
        } catch (RuntimeException re) {
            // 你可以按你们业务选择：不存在也算失败 or 直接成功
            throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST, id);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR);
        }
    }

    // 6. 删除所有商品
    @DeleteMapping
    public ResponseMessage<Void> deleteAllGoods() {
        try {
            goodsService.deleteAllGoods();
            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR);
        }
    }

    // 7. 插入测试数据
    @PostMapping("/test")
    public ResponseMessage<List<Goods>> insertTestData() {
        try {
            goodsService.insertTestData();
            List<Goods> all = goodsService.getAllGoods();
            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), all);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR);
        }
    }

    // 8. 批量更新商品库存
    @PutMapping("/batch-stock")
    public ResponseMessage<Void> batchUpdateStock(@RequestBody BatchStockUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "request");
        }

        try {
            goodsService.batchUpdateStock(request);
            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
        } catch (BusinessException be) {
            throw be;
        } catch (RuntimeException re) {
            // 你原逻辑：RuntimeException => 400
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, re.getMessage());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR);
        }
    }

    // 9. Mobile端专用 - 获取可兑换商品
    @GetMapping("/mobile/redemption")
    public ResponseMessage<List<Map<String, Object>>> getRedemptionGoodsForMobile(
            @RequestParam(required = false) Integer vipLevel) {

        List<Goods> allGoods = goodsService.getAllGoods();

        List<Goods> redemptionGoods = allGoods.stream()
                .filter(goods -> Boolean.TRUE.equals(goods.getIsForRedemption()))
                .filter(goods -> goods.getStock() > 0)
                .filter(goods -> vipLevel == null || goods.getVipLevelRequired() <= vipLevel)
                .collect(Collectors.toList());

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

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), simplifiedGoods);
    }
}
