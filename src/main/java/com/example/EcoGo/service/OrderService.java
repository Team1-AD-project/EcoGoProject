package com.example.EcoGo.service;

import com.example.EcoGo.model.Order;
import java.util.List;

public interface OrderService {
    // 创建订单
    Order createOrder(Order order);
    
    // 更新订单
    Order updateOrder(String id, Order order);
    
    // 删除订单
    void deleteOrder(String id);
    
    // 获取所有订单
    List<Order> getAllOrders();
    
    // 根据ID获取订单
    Order getOrderById(String id);
    
    // 根据用户ID获取订单
    List<Order> getOrdersByUserId(String userId);
    
    // 根据状态获取订单
    List<Order> getOrdersByStatus(String status);

    // 创建兑换订单（积分兑换）
    Order createRedemptionOrder(Order order);

}