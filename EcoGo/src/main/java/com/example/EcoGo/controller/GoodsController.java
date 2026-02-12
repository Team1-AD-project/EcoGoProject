package com.example.EcoGo.controller;

import com.example.EcoGo.dto.BatchStockUpdateRequest;
import com.example.EcoGo.dto.GoodsRequestDto;
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

    private static final String VOUCHER_TYPE = "voucher";
    private static final String ALL_ITEMS_FILTER = "all items";
    private static final List<String> VALID_CATEGORIES = List.of("food", "beverage", "merchandise", "service");

    @Autowired
    private GoodsService goodsService;

    // 1. 获取所有商品（支持筛选 + 分页）
    @GetMapping
    public ResponseMessage<Map<String, Object>> getAllGoods(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isForRedemption,
            @RequestParam(required = false, defaultValue = "false") Boolean isVipActive) {

        List<Goods> goodsList = goodsService.getAllGoods();
        
        // Apply filters
        goodsList = filterNonVouchers(goodsList);
        goodsList = filterByVipStatus(goodsList, isVipActive);
        goodsList = filterByCategory(goodsList, category);
        goodsList = filterByKeyword(goodsList, keyword);
        goodsList = filterByRedemptionStatus(goodsList, isForRedemption);

        // Build paginated response
        Map<String, Object> data = buildPaginatedResponse(goodsList, page, size);
        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    private List<Goods> filterNonVouchers(List<Goods> goods) {
        return goods.stream()
                .filter(g -> g.getType() == null || !g.getType().trim().equalsIgnoreCase(VOUCHER_TYPE))
                .collect(Collectors.toList());
    }

    private List<Goods> filterByVipStatus(List<Goods> goods, Boolean isVipActive) {
        if (Boolean.FALSE.equals(isVipActive)) {
            return goods.stream()
                    .filter(g -> g.getVipLevelRequired() == null || g.getVipLevelRequired() == 0)
                    .collect(Collectors.toList());
        }
        return goods;
    }

    private List<Goods> filterByCategory(List<Goods> goods, String category) {
        if (category == null || category.isEmpty()) {
            return goods;
        }
        
        String c = category.trim().toLowerCase();
        if ("all".equals(c) || ALL_ITEMS_FILTER.equals(c)) {
            return goods;
        }
        
        return goods.stream()
                .filter(g -> g.getCategory() != null && c.equals(g.getCategory().trim().toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<Goods> filterByKeyword(List<Goods> goods, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return goods;
        }
        
        String kw = keyword.toLowerCase();
        return goods.stream()
                .filter(good -> (good.getName() != null && good.getName().toLowerCase().contains(kw))
                        || (good.getDescription() != null && good.getDescription().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
    }

    private List<Goods> filterByRedemptionStatus(List<Goods> goods, Boolean isForRedemption) {
        if (isForRedemption == null) {
            return goods;
        }
        
        return goods.stream()
                .filter(good -> isForRedemption.equals(good.getIsForRedemption()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildPaginatedResponse(List<Goods> goodsList, int page, int size) {
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
                "totalPages", (int) Math.ceil((double) total / size)));
        
        return data;
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

    @PostMapping
    public ResponseMessage<Goods> createGoods(@RequestBody GoodsRequestDto dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "goods");
        }

        Goods goods = dto.toEntity();

        // ✅ 库存不能为负数
        if (goods.getStock() != null && goods.getStock() < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "stock cannot be negative");
        }

        // ✅ category 必须在允许范围内（如果你不想强制必填，把 isBlank 的分支删掉即可）
        if (goods.getCategory() == null || goods.getCategory().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "category cannot be empty");
        }
        String c = goods.getCategory().trim().toLowerCase();
        if (!VALID_CATEGORIES.contains(c)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid category: " + goods.getCategory());
        }
        goods.setCategory(c); // 统一存小写，避免大小写导致前端过滤不到

        try {
            Goods createdGoods = goodsService.createGoods(goods);
            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), createdGoods);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseMessage<Goods> updateGoods(@PathVariable String id, @RequestBody GoodsRequestDto dto) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "id");
        }
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "goods");
        }

        Goods goods = dto.toEntity();

        // ✅ 库存不能为负数
        if (goods.getStock() != null && goods.getStock() < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "stock cannot be negative");
        }

        // ✅ category（如果前端更新时不一定传 category，可把这一段改成 "传了才校验"）
        if (goods.getCategory() != null && !goods.getCategory().isBlank()) {
            String c = goods.getCategory().trim().toLowerCase();
            if (!VALID_CATEGORIES.contains(c)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid category: " + goods.getCategory());
            }
            goods.setCategory(c);
        }

        try {
            Goods updatedGoods = goodsService.updateGoods(id, goods);
            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), updatedGoods);
        } catch (BusinessException be) {
            throw be;
        } catch (RuntimeException re) {
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

    // 8. 批量更新商品库存
    @PutMapping("/batch-stock")
public ResponseMessage<Void> batchUpdateStock(
        @RequestBody(required = false) BatchStockUpdateRequest request) {

    if (request == null || request.getUpdates() == null || request.getUpdates().isEmpty()) {
        throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "request body/updates");
    }

    goodsService.batchUpdateStock(request);
    return ResponseMessage.success(null);
}

    // 9. Mobile端专用 - 获取可兑换商品
    @GetMapping("/mobile/redemption")
    public ResponseMessage<List<Map<String, Object>>> getRedemptionGoodsForMobile(
            @RequestParam(required = false) Integer vipLevel) {

        List<Goods> allGoods = goodsService.getAllGoods();

        List<Goods> redemptionGoods = allGoods.stream()
                .filter(goods -> Boolean.TRUE.equals(goods.getIsForRedemption()))
                .filter(goods -> goods.getStock() > 0)
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
                    return item;
                })
                .collect(Collectors.toList());

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), simplifiedGoods);
    }

    // 返回所有商品分类（给前端 Tabs 用）
    @GetMapping("/categories")
    public ResponseMessage<Map<String, Object>> getGoodsCategories() {

        Map<String, Object> data = new HashMap<>();
        data.put("categories", VALID_CATEGORIES);

        // ✅ 这个不是分类，是“不过滤”的特殊 key
        data.put("allItemsKey", ALL_ITEMS_FILTER);

        // 可选：默认 tab
        data.put("default", ALL_ITEMS_FILTER);

        return new ResponseMessage<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data);
    }

    @GetMapping("/coupons")
    public ResponseMessage<List<Goods>> getVoucherMarketplace(
            @RequestParam(required = false, defaultValue = "false") Boolean isVipActive) {
        try {
            List<Goods> goodsList = goodsService.getAllGoods();

            goodsList = goodsList.stream()
                    .filter(g -> g.getType() != null && g.getType().equalsIgnoreCase(VOUCHER_TYPE))
                    .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                    .filter(g -> Boolean.TRUE.equals(g.getIsForRedemption()))
                    .toList();

            // ✅ VIP 过滤：非 VIP 只能看到 vipLevelRequired=0（或字段缺失）
            if (Boolean.FALSE.equals(isVipActive)) {
                goodsList = goodsList.stream()
                        .filter(g -> g.getVipLevelRequired() == null || g.getVipLevelRequired() == 0)
                        .toList();
            }

            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), goodsList);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR, e.getMessage());
        }
    }

    @PostMapping("/admin/vouchers")
    public ResponseMessage<Goods> createVoucher(@RequestBody GoodsRequestDto dto) {
        Goods goods = dto.toEntity();
        goods.setType(VOUCHER_TYPE);
        goods.setIsForRedemption(true);

        if (goods.getRedemptionPoints() == null || goods.getRedemptionPoints() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "VOUCHER_REDEMPTION_POINTS_INVALID");
        }
        if (goods.getStock() == null)
            goods.setStock(0);

        Goods created = goodsService.createGoods(goods);
        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), created);
    }

    @PutMapping("/admin/vouchers/{id}")
    public ResponseMessage<Goods> updateVoucher(@PathVariable String id, @RequestBody GoodsRequestDto dto) {
        Goods existing = goodsService.getGoodsById(id);
        if (existing.getType() == null || !VOUCHER_TYPE.equalsIgnoreCase(existing.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "NOT_A_VOUCHER");
        }

        Goods goods = dto.toEntity();
        goods.setType(VOUCHER_TYPE);
        goods.setIsForRedemption(true);

        Goods updated = goodsService.updateGoods(id, goods);
        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), updated);
    }

    @DeleteMapping("/admin/vouchers/{id}")
    public ResponseMessage<Void> deleteVoucher(@PathVariable String id) {
        Goods existing = goodsService.getGoodsById(id);
        if (existing.getType() == null || !VOUCHER_TYPE.equalsIgnoreCase(existing.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "NOT_A_VOUCHER");
        }

        goodsService.deleteGoods(id);
        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    @GetMapping("/admin/vouchers")
    public ResponseMessage<List<Goods>> adminGetAllVouchers() {
        try {
            List<Goods> goodsList = goodsService.getAllGoods();

            goodsList = goodsList.stream()
                    .filter(g -> g.getType() != null && g.getType().equalsIgnoreCase(VOUCHER_TYPE))
                    .toList();

            return new ResponseMessage<>(
                    ErrorCode.SUCCESS.getCode(),
                    ErrorCode.SUCCESS.getMessage(),
                    goodsList);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR, e.getMessage());
        }
    }

}
