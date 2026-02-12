package com.example.EcoGo.service;

import com.example.EcoGo.dto.BatchStockUpdateRequest;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.Goods;
import com.example.EcoGo.model.Inventory;
import com.example.EcoGo.repository.GoodsRepository;
import com.example.EcoGo.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoodsServiceImplTest {

    @Mock private GoodsRepository goodsRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private MongoTemplate mongoTemplate;

    @InjectMocks private GoodsServiceImpl goodsService;

    @BeforeEach
    void setupDefaultSaveBehavior() {
        // 让 goodsRepository.save(...) “像真的 save 一样”：
        // - 如果 id 为空，给一个默认 id，避免 createGoods 里 inventory 需要 saved.getId() 时 NPE
        lenient().when(goodsRepository.save(any(Goods.class))).thenAnswer(inv -> {
            Goods g = inv.getArgument(0);
            if (g.getId() == null || g.getId().isBlank()) {
                g.setId("g1");
            }
            return g;
        });

        lenient().when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- createGoods ----------
    @Test
    void createGoods_shouldSetDates_defaultStockAndVipReq_andSyncInventory_andSaveTwice() {
        Goods input = new Goods();
        input.setName("Coffee");
        input.setStock(null);
        input.setVipLevelRequired(null);

        Goods created = goodsService.createGoods(input);

        assertNotNull(created);
        assertEquals("g1", created.getId());
        assertNotNull(created.getUpdatedAt());
        assertNotNull(created.getCreatedAt());
        assertEquals(0, created.getStock());
        assertEquals(0, created.getVipLevelRequired());

        // createGoods：goodsRepo.save 2 次（先存 goods，再同步库存后再存 goods）
        verify(goodsRepository, times(2)).save(any(Goods.class));
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void createGoods_whenVipReqNot0or1_shouldThrowParamError() {
        Goods input = new Goods();
        input.setVipLevelRequired(2);

        BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.createGoods(input));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(goodsRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
    }

    // ---------- getGoodsById ----------
    @Test
    void getGoodsById_whenNotFound_shouldThrowProductNotExist() {
        when(goodsRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.getGoodsById("x"));
        assertEquals(ErrorCode.PRODUCT_NOT_EXIST.getCode(), ex.getCode());
    }

    @Test
    void getGoodsById_found_shouldReturn() {
        Goods g = new Goods();
        g.setId("x");
        when(goodsRepository.findById("x")).thenReturn(Optional.of(g));

        Goods got = goodsService.getGoodsById("x");
        assertEquals("x", got.getId());
    }

    // ---------- getAllGoods ----------
    @Test
    void getAllGoods_shouldDelegateFindAll() {
        when(goodsRepository.findAll()).thenReturn(List.of(new Goods(), new Goods()));
        List<Goods> list = goodsService.getAllGoods();
        assertEquals(2, list.size());
        verify(goodsRepository).findAll();
    }

    // ---------- updateGoods ----------
    @Test
    void updateGoods_whenNegativeStock_shouldThrowDbError_asCurrentImplementation() {
        Goods patch = new Goods();
        patch.setStock(-1);

        BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.updateGoods("g1", patch));
        assertEquals(ErrorCode.DB_ERROR.getCode(), ex.getCode());
    }

    @Test
    void updateGoods_whenNotFound_shouldThrowRuntimeException() {
        when(goodsRepository.findById("g1")).thenReturn(Optional.empty());

        Goods patch = new Goods();
        patch.setStock(10);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> goodsService.updateGoods("g1", patch));
        assertTrue(ex.getMessage().contains("Product not found"));
    }

    @Test
    void updateGoods_shouldUpdateFields_syncInventory_andSaveTwice() {
        Goods existing = new Goods();
        existing.setId("g1");
        existing.setStock(5);

        when(goodsRepository.findById("g1")).thenReturn(Optional.of(existing));

        Goods patch = new Goods();
        patch.setName("NewName");
        patch.setDescription("NewDesc");
        patch.setStock(9);
        patch.setVipLevelRequired(null); // should default to 0

        Goods updated = goodsService.updateGoods("g1", patch);

        assertNotNull(updated);
        assertEquals("g1", updated.getId());
        assertEquals("NewName", updated.getName());
        assertEquals(9, updated.getStock());
        assertEquals(0, updated.getVipLevelRequired());

        verify(goodsRepository, times(2)).save(any(Goods.class));
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void updateGoods_whenVipReqNot0or1_shouldThrowParamError() {
        Goods existing = new Goods();
        existing.setId("g1");
        when(goodsRepository.findById("g1")).thenReturn(Optional.of(existing));

        Goods patch = new Goods();
        patch.setVipLevelRequired(9);

        BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.updateGoods("g1", patch));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(goodsRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
    }

    // ---------- deleteGoods / deleteAllGoods ----------
    @Test
    void deleteGoods_shouldDeleteFromGoodsAndInventoryById() {
        goodsService.deleteGoods("g1");
        verify(goodsRepository).deleteById("g1");
        verify(inventoryRepository).deleteById("g1");
    }

    @Test
    void deleteAllGoods_shouldDeleteAllFromBothCollections() {
        goodsService.deleteAllGoods();
        verify(goodsRepository).deleteAll();
        verify(inventoryRepository).deleteAll();
    }

    // ---------- batchUpdateStock ----------
    @Test
    void batchUpdateStock_withNewStock_shouldUpdateGoodsAndInventory() {
        Goods g = new Goods();
        g.setId("g1");
        g.setStock(5);
        when(goodsRepository.findById("g1")).thenReturn(Optional.of(g));

        BatchStockUpdateRequest.StockUpdateItem item = new BatchStockUpdateRequest.StockUpdateItem();
        item.setGoodsId("g1");
        item.setNewStock(12);

        BatchStockUpdateRequest req = new BatchStockUpdateRequest();
        req.setUpdates(List.of(item));

        goodsService.batchUpdateStock(req);

        verify(goodsRepository).save(argThat(saved -> saved.getStock() == 12));
        verify(inventoryRepository).save(argThat(inv -> inv.getGoodsId().equals("g1") && inv.getQuantity() == 12));
    }

    @Test
    void batchUpdateStock_withStockChange_shouldApplyDelta() {
        Goods g = new Goods();
        g.setId("g1");
        g.setStock(5);
        when(goodsRepository.findById("g1")).thenReturn(Optional.of(g));

        BatchStockUpdateRequest.StockUpdateItem item = new BatchStockUpdateRequest.StockUpdateItem();
        item.setGoodsId("g1");
        item.setStockChange(3); // 5 + 3 = 8

        BatchStockUpdateRequest req = new BatchStockUpdateRequest();
        req.setUpdates(List.of(item));

        goodsService.batchUpdateStock(req);

        verify(goodsRepository).save(argThat(saved -> saved.getStock() == 8));
        verify(inventoryRepository).save(argThat(inv -> inv.getQuantity() == 8));
    }

    @Test
    void batchUpdateStock_missingFields_shouldThrowRuntimeException() {
        Goods g = new Goods();
        g.setId("g1");
        g.setStock(5);
        when(goodsRepository.findById("g1")).thenReturn(Optional.of(g));

        BatchStockUpdateRequest.StockUpdateItem item = new BatchStockUpdateRequest.StockUpdateItem();
        item.setGoodsId("g1");
        // newStock 和 stockChange 都没给

        BatchStockUpdateRequest req = new BatchStockUpdateRequest();
        req.setUpdates(List.of(item));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> goodsService.batchUpdateStock(req));
        assertTrue(ex.getMessage().contains("缺少 newStock 或 stockChange"));
    }

    @Test
    void batchUpdateStock_negativeFinalStock_shouldThrowRuntimeException() {
        Goods g = new Goods();
        g.setId("g1");
        g.setStock(1);
        when(goodsRepository.findById("g1")).thenReturn(Optional.of(g));

        BatchStockUpdateRequest.StockUpdateItem item = new BatchStockUpdateRequest.StockUpdateItem();
        item.setGoodsId("g1");
        item.setStockChange(-2); // 1 + (-2) = -1

        BatchStockUpdateRequest req = new BatchStockUpdateRequest();
        req.setUpdates(List.of(item));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> goodsService.batchUpdateStock(req));
        assertTrue(ex.getMessage().contains("库存不能为负数"));
    }

    @Test
    void batchUpdateStock_goodsNotFound_shouldThrowRuntimeException() {
        when(goodsRepository.findById("g1")).thenReturn(Optional.empty());

        BatchStockUpdateRequest.StockUpdateItem item = new BatchStockUpdateRequest.StockUpdateItem();
        item.setGoodsId("g1");
        item.setNewStock(10);

        BatchStockUpdateRequest req = new BatchStockUpdateRequest();
        req.setUpdates(List.of(item));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> goodsService.batchUpdateStock(req));
        assertTrue(ex.getMessage().contains("商品未找到"));
    }

    // ---------- reserveStock / releaseStock ----------
    @Test
    void reserveStock_whenOutOfStock_shouldThrowBusinessParamError() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Inventory.class), eq("inventory"))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.reserveStock("g1", 1));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void reserveStock_success_shouldSyncGoodsStock() {
        Inventory after = new Inventory();
        after.setGoodsId("g1");
        after.setQuantity(7);

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Inventory.class), eq("inventory"))).thenReturn(after);

        Goods g = new Goods();
        g.setId("g1");
        when(goodsRepository.findById("g1")).thenReturn(Optional.of(g));

        goodsService.reserveStock("g1", 2);

        verify(goodsRepository).save(argThat(saved -> saved.getStock() == 7));
    }

    @Test
    void releaseStock_whenInventoryNull_shouldNotThrow() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Inventory.class), eq("inventory"))).thenReturn(null);

        assertDoesNotThrow(() -> goodsService.releaseStock("g1", 1));
        verify(goodsRepository, never()).save(any());
    }

    @Test
    void releaseStock_success_shouldSyncGoodsStock() {
        Inventory after = new Inventory();
        after.setGoodsId("g1");
        after.setQuantity(11);

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Inventory.class), eq("inventory"))).thenReturn(after);

        Goods g = new Goods();
        g.setId("g1");
        when(goodsRepository.findById("g1")).thenReturn(Optional.of(g));

        goodsService.releaseStock("g1", 3);

        verify(goodsRepository).save(argThat(saved -> saved.getStock() == 11));
    }
}
