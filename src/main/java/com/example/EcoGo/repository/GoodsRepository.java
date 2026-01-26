package com.example.EcoGo.repository;

import com.example.EcoGo.model.Goods;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsRepository extends MongoRepository<Goods, String> {
    // 可以根据需要添加自定义查询方法
    // List<Goods> findByCategory(String category);
    // List<Goods> findByIsForRedemption(boolean isForRedemption);
}