package com.example.EcoGo.service;

import com.example.EcoGo.model.Goods;
import com.example.EcoGo.repository.GoodsRepository;
import com.example.EcoGo.dto.BatchStockUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Date;

@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsRepository goodsRepository;

    // 创建商品
    @Override
    public Goods createGoods(Goods goods) {
        goods.setCreatedAt(new Date());
        goods.setUpdatedAt(new Date());
        return goodsRepository.save(goods);
    }

    // 更新商品
    @Override
    public Goods updateGoods(String id, Goods goods) {
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
            updatedGoods.setRedemptionPoints(goods.getRedemptionPoints());
            updatedGoods.setVipLevelRequired(goods.getVipLevelRequired());
            updatedGoods.setRedemptionLimit(goods.getRedemptionLimit());
            updatedGoods.setUpdatedAt(new Date());
            return goodsRepository.save(updatedGoods);
        } else {
            throw new RuntimeException("Product not found with id " + id);
        }
    }

    // 删除商品
    @Override
    public void deleteGoods(String id) {
        goodsRepository.deleteById(id);
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
    }

    // 插入测试商品数据
    @Override
    public void insertTestData() {
        // 清空现有数据
        goodsRepository.deleteAll();

        // 插入测试商品数据（包含VIP兑换商品）
        List<Goods> testGoods = Arrays.asList(
            new Goods("环保水杯", "可重复使用的不锈钢水杯", 59.99, 100),
            new Goods("有机棉T恤", "100%有机棉制成的环保T恤", 89.99, 50),
            new Goods("太阳能充电宝", "使用太阳能充电的移动电源", 199.99, 30),
            new Goods("竹制餐具套装", "环保竹制餐具，包含筷子、勺子和叉子", 39.99, 80),
            new Goods("垃圾分类桶", "四个分类的家用垃圾桶", 129.99, 25)
        );
        
        // 设置分类、品牌和图片
        testGoods.get(0).setCategory("日常用品");
        testGoods.get(0).setBrand("EcoBrand");
        testGoods.get(0).setImageUrl("/images/water-cup.jpg");
        testGoods.get(0).setIsForRedemption(true);
        testGoods.get(0).setRedemptionPoints(500);
        testGoods.get(0).setVipLevelRequired(0);
        
        testGoods.get(1).setCategory("服装");
        testGoods.get(1).setBrand("EcoFashion");
        testGoods.get(1).setImageUrl("/images/t-shirt.jpg");
        testGoods.get(1).setIsForRedemption(true);
        testGoods.get(1).setRedemptionPoints(800);
        testGoods.get(1).setVipLevelRequired(1);
        
        testGoods.get(2).setCategory("电子产品");
        testGoods.get(2).setBrand("EcoTech");
        testGoods.get(2).setImageUrl("/images/power-bank.jpg");
        testGoods.get(2).setIsForRedemption(true);
        testGoods.get(2).setRedemptionPoints(2000);
        testGoods.get(2).setVipLevelRequired(2);
        
        testGoods.get(3).setCategory("厨房用品");
        testGoods.get(3).setBrand("EcoKitchen");
        testGoods.get(3).setImageUrl("/images/utensils.jpg");
        testGoods.get(3).setIsForRedemption(false);
        testGoods.get(3).setRedemptionPoints(0);
        
        testGoods.get(4).setCategory("家居用品");
        testGoods.get(4).setBrand("EcoHome");
        testGoods.get(4).setImageUrl("/images/bin.jpg");
        testGoods.get(4).setIsForRedemption(true);
        testGoods.get(4).setRedemptionPoints(1200);
        testGoods.get(4).setVipLevelRequired(1);
        
        goodsRepository.saveAll(testGoods);
    }

    // 根据ID获取商品
    @Override
    public Goods getGoodsById(String id) {
        return goodsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + id));
    }

    // 批量更新商品库存
    @Override
    public void batchUpdateStock(BatchStockUpdateRequest request) {
        for (BatchStockUpdateRequest.StockUpdateItem item : request.getUpdates()) {
            Optional<Goods> goodsOpt = goodsRepository.findById(item.getGoodsId());
            if (goodsOpt.isPresent()) {
                Goods goods = goodsOpt.get();
                if (item.getNewStock() != null) {
                    goods.setStock(item.getNewStock());
                } else if (item.getStockChange() != null) {
                    int newStock = goods.getStock() + item.getStockChange();
                    if (newStock < 0) {
                        throw new RuntimeException("库存不能为负数: 商品ID " + item.getGoodsId());
                    }
                    goods.setStock(newStock);
                }
                goods.setUpdatedAt(new Date());
                goodsRepository.save(goods);
            } else {
                throw new RuntimeException("商品未找到: " + item.getGoodsId());
            }
        }
    }
}