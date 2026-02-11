package com.example.EcoGo.controller;

import com.example.EcoGo.dto.BatchStockUpdateRequest;
import com.example.EcoGo.dto.GoodsRequestDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.Goods;
import com.example.EcoGo.service.GoodsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoodsControllerTest {

    private GoodsService goodsService;
    private GoodsController controller;

    @BeforeEach
    void setUp() throws Exception {
        goodsService = mock(GoodsService.class);
        controller = new GoodsController();

        // 字段注入：把 mock 塞进 @Autowired 的 private 字段
        Field f = GoodsController.class.getDeclaredField("goodsService");
        f.setAccessible(true);
        f.set(controller, goodsService);
    }

    // ---------- helper ----------
    private static Goods goods(String id, String name, String desc, String category, String type,
                              Integer vipLevelRequired, Boolean isForRedemption, Integer stock,
                              Boolean isActive) {
        Goods g = new Goods();
        g.setId(id);
        g.setName(name);
        g.setDescription(desc);
        g.setCategory(category);
        g.setType(type);
        g.setVipLevelRequired(vipLevelRequired);
        g.setIsForRedemption(isForRedemption);
        g.setStock(stock);
        g.setIsActive(isActive);
        return g;
    }

    // ---------- getAllGoods ----------
    @Test
    void getAllGoods_shouldFilterOutVoucherType_andApplyVipCategoryKeywordRedemption_andPaginate() {
        // 准备数据：
        // 1) voucher 类型：应该被过滤掉
        // 2) vipLevelRequired=1：当 isVipActive=false 时应该被过滤掉
        // 3) 正常商品：保留
        Goods voucher = goods("v1", "Voucher X", "coupon", "service", "voucher", 0, true, 10, true);
        Goods vipOnly = goods("g1", "VIP Burger", "tasty", "food", "normal", 1, true, 10, true);
        Goods normal = goods("g2", "Coffee", "Starbucks", "beverage", "normal", 0, true, 5, true);
        Goods normal2 = goods("g3", "Tea", "Green tea", "beverage", null, 0, false, 5, true);

        when(goodsService.getAllGoods()).thenReturn(List.of(voucher, vipOnly, normal, normal2));

        // page=1,size=1 + category=beverage + keyword=star + isForRedemption=true + isVipActive=false
        ResponseMessage<Map<String, Object>> resp = controller.getAllGoods(
                1, 1, "beverage", "star", true, false
        );

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());

        Object itemsObj = resp.getData().get("items");
        assertTrue(itemsObj instanceof List<?>);
        List<?> items = (List<?>) itemsObj;

        // 过滤后只剩 "Coffee" 一条，再分页 size=1 仍是 1
        assertEquals(1, items.size());
        Goods item = (Goods) items.get(0);
        assertEquals("g2", item.getId());

        // pagination 检查
        Object paginationObj = resp.getData().get("pagination");
        assertTrue(paginationObj instanceof Map<?, ?>);
        Map<?, ?> pagination = (Map<?, ?>) paginationObj;
        assertEquals(1, pagination.get("page"));
        assertEquals(1, pagination.get("size"));
        assertEquals(1, pagination.get("total"));
        assertEquals(1, pagination.get("totalPages"));

        verify(goodsService).getAllGoods();
    }

    @Test
    void getAllGoods_categoryAll_shouldNotFilterByCategory() {
        Goods a = goods("g1", "A", null, "food", "normal", 0, true, 1, true);
        Goods b = goods("g2", "B", null, "service", "normal", 0, true, 1, true);
        when(goodsService.getAllGoods()).thenReturn(List.of(a, b));

        ResponseMessage<Map<String, Object>> resp = controller.getAllGoods(
                1, 20, "all items", null, null, true
        );

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        List<?> items = (List<?>) resp.getData().get("items");
        // isVipActive=true，不做 vipLevelRequired 过滤；category=all items 不过滤
        assertEquals(2, items.size());
    }

    // ---------- getGoodsById ----------
    @Test
    void getGoodsById_idBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getGoodsById("   ")
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void getGoodsById_success() {
        Goods g = goods("g1", "Coffee", null, "beverage", "normal", 0, true, 1, true);
        when(goodsService.getGoodsById("g1")).thenReturn(g);

        ResponseMessage<Goods> resp = controller.getGoodsById("g1");
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("g1", resp.getData().getId());
    }

    @Test
    void getGoodsById_runtimeException_shouldMapToProductNotExist() {
        when(goodsService.getGoodsById("x")).thenThrow(new RuntimeException("not found"));
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getGoodsById("x")
        );
        assertEquals(ErrorCode.PRODUCT_NOT_EXIST.getCode(), ex.getCode());
    }

    // ---------- createGoods ----------
    @Test
    void createGoods_nullBody_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.createGoods(null)
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void createGoods_negativeStock_throwParamError() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setCategory("food");
        dto.setStock(-1);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.createGoods(dto)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().toLowerCase().contains("stock"));
    }

    @Test
    void createGoods_invalidCategory_throwParamError() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setCategory("unknown");
        dto.setStock(1);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.createGoods(dto)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().toLowerCase().contains("invalid category"));
    }

    @Test
    void createGoods_success_shouldNormalizeCategoryToLowercase() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setCategory(" Food ");
        dto.setStock(1);

        Goods created = goods("g1", "Burger", null, "food", "normal", 0, true, 1, true);
        when(goodsService.createGoods(any(Goods.class))).thenReturn(created);

        ResponseMessage<Goods> resp = controller.createGoods(dto);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("g1", resp.getData().getId());
        verify(goodsService).createGoods(any(Goods.class));
    }

    @Test
    void createGoods_serviceThrowsRuntime_shouldMapToDbError() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setCategory("food");
        dto.setStock(1);

        when(goodsService.createGoods(any(Goods.class))).thenThrow(new RuntimeException("db down"));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.createGoods(dto)
        );
        assertEquals(ErrorCode.DB_ERROR.getCode(), ex.getCode());
    }

    // ---------- updateGoods ----------
    @Test
    void updateGoods_idBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateGoods(" ", new GoodsRequestDto())
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void updateGoods_goodsNull_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateGoods("g1", null)
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void updateGoods_negativeStock_throwParamError() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setStock(-2);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateGoods("g1", dto)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void updateGoods_invalidCategory_throwParamError() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setCategory("xxx");
        dto.setStock(1);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateGoods("g1", dto)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void updateGoods_success_shouldNormalizeCategoryWhenProvided() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setCategory(" Beverage ");
        dto.setStock(1);

        Goods updated = goods("g1", "Coffee", null, "beverage", "normal", 0, true, 1, true);
        when(goodsService.updateGoods(eq("g1"), any(Goods.class))).thenReturn(updated);

        ResponseMessage<Goods> resp = controller.updateGoods("g1", dto);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("g1", resp.getData().getId());
    }

    @Test
    void updateGoods_runtimeException_shouldMapToProductNotExist() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setStock(1);

        when(goodsService.updateGoods(eq("x"), any(Goods.class)))
                .thenThrow(new RuntimeException("not found"));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateGoods("x", dto)
        );
        assertEquals(ErrorCode.PRODUCT_NOT_EXIST.getCode(), ex.getCode());
    }

    // ---------- deleteGoods / deleteAllGoods ----------
    @Test
    void deleteGoods_idBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.deleteGoods(" ")
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void deleteGoods_success() {
        doNothing().when(goodsService).deleteGoods("g1");
        ResponseMessage<Void> resp = controller.deleteGoods("g1");
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(goodsService).deleteGoods("g1");
    }

    @Test
    void deleteAllGoods_success() {
        doNothing().when(goodsService).deleteAllGoods();
        ResponseMessage<Void> resp = controller.deleteAllGoods();
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(goodsService).deleteAllGoods();
    }

    // ---------- batchUpdateStock ----------
    @Test
    void batchUpdateStock_nullRequest_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.batchUpdateStock(null)
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void batchUpdateStock_emptyUpdates_throwParamCannotBeNull() {
        BatchStockUpdateRequest req = new BatchStockUpdateRequest();
        req.setUpdates(List.of());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.batchUpdateStock(req)
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void batchUpdateStock_success() {
        BatchStockUpdateRequest req = new BatchStockUpdateRequest();

        BatchStockUpdateRequest.StockUpdateItem item = new BatchStockUpdateRequest.StockUpdateItem();
        item.setGoodsId("g1");
        item.setStockChange(1);   // 或 item.setNewStock(10);

        req.setUpdates(List.of(item));
        req.setReason("restock");

        doNothing().when(goodsService).batchUpdateStock(req);

        ResponseMessage<Void> resp = controller.batchUpdateStock(req);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(goodsService).batchUpdateStock(req);
}


    // ---------- mobile redemption ----------
    @Test
    void getRedemptionGoodsForMobile_shouldReturnSimplifiedList_onlyForRedemption_andStockPositive() {
        Goods a = goods("g1", "A", "d", "food", "normal", 0, true, 5, true);
        Goods b = goods("g2", "B", "d", "food", "normal", 0, true, 0, true);     // stock=0 -> filter out
        Goods c = goods("g3", "C", "d", "food", "normal", 0, false, 5, true);    // not for redemption -> filter out

        when(goodsService.getAllGoods()).thenReturn(List.of(a, b, c));

        ResponseMessage<List<Map<String, Object>>> resp = controller.getRedemptionGoodsForMobile(null);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals(1, resp.getData().size());

        Map<String, Object> item = resp.getData().get(0);
        assertEquals("g1", item.get("id"));
        assertEquals("A", item.get("name"));
        assertEquals(5, item.get("stock"));
    }

    // ---------- categories ----------
    @Test
    void getGoodsCategories_shouldReturnCategoriesAndAllItemsKey() {
        ResponseMessage<Map<String, Object>> resp = controller.getGoodsCategories();
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());

        Map<String, Object> data = resp.getData();
        assertNotNull(data);
        assertEquals("all items", data.get("allItemsKey"));
        assertEquals("all items", data.get("default"));

        Object cats = data.get("categories");
        assertTrue(cats instanceof List<?>);
        assertEquals(4, ((List<?>) cats).size());
    }

    // ---------- coupons marketplace ----------
    @Test
    void getVoucherMarketplace_shouldFilterVoucherActiveAndForRedemption_andApplyVipFilter() {
        Goods voucherOk = goods("v1", "V1", null, "service", "voucher", 0, true, 5, true);
        Goods voucherInactive = goods("v2", "V2", null, "service", "voucher", 0, true, 5, false);
        Goods nonVoucher = goods("g1", "G1", null, "food", "normal", 0, true, 5, true);
        Goods voucherVip = goods("v3", "V3", null, "service", "voucher", 2, true, 5, true);

        when(goodsService.getAllGoods()).thenReturn(List.of(voucherOk, voucherInactive, nonVoucher, voucherVip));

        // isVipActive=false -> 应过滤掉 vipLevelRequired>0
        ResponseMessage<List<Goods>> resp = controller.getVoucherMarketplace(false);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        // 只剩 voucherOk
        assertEquals(1, resp.getData().size());
        assertEquals("v1", resp.getData().get(0).getId());
    }

    @Test
    void getVoucherMarketplace_serviceThrows_shouldMapDbError() {
        when(goodsService.getAllGoods()).thenThrow(new RuntimeException("db down"));
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getVoucherMarketplace(false)
        );
        assertEquals(ErrorCode.DB_ERROR.getCode(), ex.getCode());
    }

    // ---------- admin vouchers ----------
    @Test
    void createVoucher_shouldSetTypeAndIsForRedemption_andValidateRedemptionPoints() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setRedemptionPoints(10);
        dto.setStock(null);

        Goods created = goods("v1", "V", null, "service", "voucher", 0, true, 0, true);
        when(goodsService.createGoods(any(Goods.class))).thenReturn(created);

        ResponseMessage<Goods> resp = controller.createVoucher(dto);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        verify(goodsService).createGoods(argThat(g -> "voucher".equals(g.getType()) && Boolean.TRUE.equals(g.getIsForRedemption())));
    }

    @Test
    void createVoucher_invalidRedemptionPoints_throwParamError() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setRedemptionPoints(0);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.createVoucher(dto)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void updateVoucher_notAVoucher_throwParamError() {
        Goods existing = goods("g1", "G", null, "food", "normal", 0, true, 1, true);
        when(goodsService.getGoodsById("g1")).thenReturn(existing);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateVoucher("g1", new GoodsRequestDto())
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void updateVoucher_success_shouldForceVoucherFields() {
        Goods existing = goods("v1", "V", null, "service", "voucher", 0, true, 1, true);
        when(goodsService.getGoodsById("v1")).thenReturn(existing);

        Goods updated = goods("v1", "V2", null, "service", "voucher", 0, true, 1, true);
        when(goodsService.updateGoods(eq("v1"), any(Goods.class))).thenReturn(updated);

        GoodsRequestDto dto = new GoodsRequestDto();
        ResponseMessage<Goods> resp = controller.updateVoucher("v1", dto);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        verify(goodsService).updateGoods(eq("v1"), argThat(g -> "voucher".equals(g.getType()) && Boolean.TRUE.equals(g.getIsForRedemption())));
    }

    @Test
    void deleteVoucher_notAVoucher_throwParamError() {
        Goods existing = goods("g1", "G", null, "food", "normal", 0, true, 1, true);
        when(goodsService.getGoodsById("g1")).thenReturn(existing);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.deleteVoucher("g1")
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void deleteVoucher_success() {
        Goods existing = goods("v1", "V", null, "service", "voucher", 0, true, 1, true);
        when(goodsService.getGoodsById("v1")).thenReturn(existing);

        doNothing().when(goodsService).deleteGoods("v1");

        ResponseMessage<Void> resp = controller.deleteVoucher("v1");
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(goodsService).deleteGoods("v1");
    }

    @Test
    void adminGetAllVouchers_shouldReturnOnlyVoucherType() {
        Goods v = goods("v1", "V", null, "service", "voucher", 0, true, 1, true);
        Goods g = goods("g1", "G", null, "food", "normal", 0, true, 1, true);

        when(goodsService.getAllGoods()).thenReturn(List.of(v, g));

        ResponseMessage<List<Goods>> resp = controller.adminGetAllVouchers();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals(1, resp.getData().size());
        assertEquals("v1", resp.getData().get(0).getId());
    }

    @Test
    void adminGetAllVouchers_serviceThrows_shouldMapDbError() {
        when(goodsService.getAllGoods()).thenThrow(new RuntimeException("db down"));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.adminGetAllVouchers()
        );

        assertEquals(ErrorCode.DB_ERROR.getCode(), ex.getCode());
    }
}
