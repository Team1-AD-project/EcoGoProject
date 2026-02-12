package com.example.EcoGo.service;

import com.example.EcoGo.model.Goods;
import com.example.EcoGo.dto.BatchStockUpdateRequest;
import java.util.List;

public interface GoodsService {
    // 创建商品
    Goods createGoods(Goods goods);
    
    // 更新商品
    Goods updateGoods(String id, Goods goods);
    
    // 删除商品
    void deleteGoods(String id);
    
    // 获取所有商品
    List<Goods> getAllGoods();
    
    // 删除所有商品
    void deleteAllGoods();
    
    // 根据ID获取商品
    Goods getGoodsById(String id);
    
    // 批量更新商品库存
    void batchUpdateStock(BatchStockUpdateRequest request);

    // 兑换用：扣库存
    void reserveStock(String goodsId, int quantity);

    // 兑换失败回滚库存
    void releaseStock(String goodsId, int quantity);
}