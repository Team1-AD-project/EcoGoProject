package com.example.EcoGo.service;

import com.example.EcoGo.model.Goods;
import com.example.EcoGo.model.Inventory;
import com.example.EcoGo.repository.GoodsRepository;
import com.example.EcoGo.repository.InventoryRepository;
import com.example.EcoGo.dto.BatchStockUpdateRequest;
import com.example.EcoGo.exception.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import com.example.EcoGo.model.Inventory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.example.EcoGo.exception.errorcode.ErrorCode;



import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Date;

@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Override
    public void reserveStock(String goodsId, int quantity) {
    if (quantity <= 0) quantity = 1;

    Query query = new Query(
            Criteria.where("goodsId").is(goodsId)
                    .and("quantity").gte(quantity)
    );

    Update update = new Update()
            .inc("quantity", -quantity)
            .set("updatedAt", new Date());

    FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

    Inventory after = mongoTemplate.findAndModify(query, update, options, Inventory.class, "inventory");
    if (after == null) {
        throw new BusinessException(ErrorCode.PARAM_ERROR, "OUT_OF_STOCK");
    }

    // 同步 Goods.stock（兼容 mobile/redemption 的 goods.getStock() > 0 过滤）
    goodsRepository.findById(goodsId).ifPresent(g -> {
        g.setStock(after.getQuantity());
        g.setUpdatedAt(new Date());
        goodsRepository.save(g);
        });
    }

    @Override
    public void releaseStock(String goodsId, int quantity) {
        if (quantity <= 0) quantity = 1;

        Query query = new Query(Criteria.where("goodsId").is(goodsId));

        Update update = new Update()
                .inc("quantity", quantity)
                .set("updatedAt", new Date());

        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

        Inventory after = mongoTemplate.findAndModify(query, update, options, Inventory.class, "inventory");

        if (after != null) {
            goodsRepository.findById(goodsId).ifPresent(g -> {
                g.setStock(after.getQuantity());
                g.setUpdatedAt(new Date());
                goodsRepository.save(g);
            });
        }   
    }


    @Autowired
    private GoodsRepository goodsRepository;

    // 创建商品
    @Override
    public Goods createGoods(Goods goods) {
        goods.setCreatedAt(new Date());
        goods.setUpdatedAt(new Date());

         // ✅ 兜底：null 当 0，负数直接改为 0（或改成抛异常，二选一）
        Integer stock = goods.getStock();
        if (stock == null) stock = 0;
        if (stock < 0) stock = 0;  // 如果你想严格：这里改成 throw new BusinessException(...)
        goods.setStock(stock);

        // ✅ VIP 可见性：null 当 0；只允许 0/1
        Integer vipReq = goods.getVipLevelRequired();
        if (vipReq == null) vipReq = 0;
        if (vipReq != 0 && vipReq != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "isVipRequired must be 0 or 1");
        }
        goods.setVipLevelRequired(vipReq);


        Goods saved = goodsRepository.save(goods);

    // ✅ 同步库存到 inventory（id = goodsId）
        Inventory inv = new Inventory();
        inv.setId(saved.getId());
        inv.setGoodsId(saved.getId());
        inv.setQuantity(saved.getStock() == null ? 0 : saved.getStock());
        inv.setUpdatedAt(new Date());
        inventoryRepository.save(inv);

    // ✅ 兼容：Goods.stock 与 inventory.quantity 保持一致
        saved.setStock(inv.getQuantity());
        saved.setUpdatedAt(new Date());
        return goodsRepository.save(saved);
}



    // 更新商品
    @Override
    public Goods updateGoods(String id, Goods goods) {

         // ✅ 兜底：null 当 0，负数直接改为 0（或改成抛异常，二选一）
        Integer stock = goods.getStock();
        if (stock != null && stock < 0) {
            throw new BusinessException(ErrorCode.DB_ERROR, "stock cannot be negative");
        }

        Optional<Goods> existingGoods = goodsRepository.findById(id);
        if (existingGoods.isPresent()) {
            Goods updatedGoods = existingGoods.get();
            updatedGoods.setName(goods.getName());
            updatedGoods.setDescription(goods.getDescription());
            updatedGoods.setPrice(goods.getPrice());
            updatedGoods.setStock(goods.getStock());
            updatedGoods.setCategory(goods.getCategory());
            updatedGoods.setBrand(goods.getBrand());
            updatedGoods.setImageUrl(goods.getImageUrl());
            updatedGoods.setIsActive(goods.getIsActive());
            updatedGoods.setIsForRedemption(goods.getIsForRedemption());
            // ✅ VIP 可见性：null 当 0；只允许 0/1
            Integer vipReq = goods.getVipLevelRequired();
            if (vipReq == null) vipReq = 0;
            if (vipReq != 0 && vipReq != 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "isVipRequired must be 0 or 1");
            }
            updatedGoods.setVipLevelRequired(vipReq);

            updatedGoods.setRedemptionPoints(goods.getRedemptionPoints());
            updatedGoods.setRedemptionLimit(goods.getRedemptionLimit());
            updatedGoods.setUpdatedAt(new Date());
            updatedGoods.setUpdatedAt(new Date());
            Goods saved = goodsRepository.save(updatedGoods);

            // ✅ 同步库存到 inventory（管理员可能改了 stock）
            Inventory inv = new Inventory();
            inv.setId(saved.getId());
            inv.setGoodsId(saved.getId());
            inv.setQuantity(saved.getStock() == null ? 0 : saved.getStock());
            inv.setUpdatedAt(new Date());
            inventoryRepository.save(inv);

            // ✅ 兼容：Goods.stock 与 inventory.quantity 保持一致
            saved.setStock(inv.getQuantity());
            saved.setUpdatedAt(new Date());
            return goodsRepository.save(saved);
        } else {
            throw new RuntimeException("Product not found with id " + id);
        }
    }

    // 删除商品
    @Override
    public void deleteGoods(String id) {
        goodsRepository.deleteById(id);
        inventoryRepository.deleteById(id);
    }

    // 获取所有商品
    @Override
    public List<Goods> getAllGoods() {
        return goodsRepository.findAll();
    }

    // 删除所有商品
    @Override
    public void deleteAllGoods() {
        goodsRepository.deleteAll();
        inventoryRepository.deleteAll();
    }


    // 根据ID获取商品
    @Override
    public Goods getGoodsById(String id) {
        return goodsRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_EXIST, id));
    }

    @Override
    public void batchUpdateStock(BatchStockUpdateRequest request) {
        for (BatchStockUpdateRequest.StockUpdateItem item : request.getUpdates()) {
            Optional<Goods> goodsOpt = goodsRepository.findById(item.getGoodsId());
            if (goodsOpt.isPresent()) {
                Goods goods = goodsOpt.get();

                // 计算最终库存
                int finalStock;
                if (item.getNewStock() != null) {
                    finalStock = item.getNewStock();
                } else if (item.getStockChange() != null) {
                    finalStock = goods.getStock() + item.getStockChange();
                } else {
                    throw new RuntimeException("库存更新请求缺少 newStock 或 stockChange: " + item.getGoodsId());
                }

                if (finalStock < 0) {
                    throw new RuntimeException("库存不能为负数: 商品ID " + item.getGoodsId());
                }

                // ✅ 先更新 goods.stock（兼容现有接口）
                goods.setStock(finalStock);
                goods.setUpdatedAt(new Date());
                goodsRepository.save(goods);

                // ✅ 再同步 inventory（真实库存源）
                Inventory inv = new Inventory();
                inv.setId(goods.getId());
                inv.setGoodsId(goods.getId());
                inv.setQuantity(finalStock);
                inv.setUpdatedAt(new Date());
                inventoryRepository.save(inv);

            } else {
                throw new RuntimeException("商品未找到: " + item.getGoodsId());
            }
        }
    }

}