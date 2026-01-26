package com.example.EcoGo.repository;

import com.example.EcoGo.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByStatus(String status);
    List<Order> findByUserIdAndStatus(String userId, String status);
    // 可以添加更多查询方法
}