package com.example.EcoGo.service;

import com.example.EcoGo.model.Order;
import com.example.EcoGo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Order createOrder(Order order) {
        // 生成订单号
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            String orderNumber = "ORD" + System.currentTimeMillis() + 
                               UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            order.setOrderNumber(orderNumber);
        }
        
        // 计算总金额
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            double total = order.getItems().stream()
                .mapToDouble(item -> {
                    if (item.getSubtotal() != null) {
                        return item.getSubtotal();
                    } else if (item.getPrice() != null && item.getQuantity() != null) {
                        return item.getPrice() * item.getQuantity();
                    }
                    return 0.0;
                })
                .sum();
            
            order.setTotalAmount(total);
            order.setFinalAmount(total + (order.getShippingFee() != null ? order.getShippingFee() : 0.0));
        }
        
        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());
        
        // 设置默认状态
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus("PENDING");
        }
        if (order.getPaymentStatus() == null || order.getPaymentStatus().isEmpty()) {
            order.setPaymentStatus("PENDING");
        }
        
        return orderRepository.save(order);
    }

    @Override
    public Order updateOrder(String id, Order order) {
        Optional<Order> existingOrder = orderRepository.findById(id);
        if (existingOrder.isPresent()) {
            Order updatedOrder = existingOrder.get();
            
            // 只更新允许更新的字段
            if (order.getStatus() != null) {
                updatedOrder.setStatus(order.getStatus());
            }
            if (order.getPaymentStatus() != null) {
                updatedOrder.setPaymentStatus(order.getPaymentStatus());
            }
            if (order.getPaymentMethod() != null) {
                updatedOrder.setPaymentMethod(order.getPaymentMethod());
            }
            if (order.getShippingAddress() != null) {
                updatedOrder.setShippingAddress(order.getShippingAddress());
            }
            if (order.getRecipientName() != null) {
                updatedOrder.setRecipientName(order.getRecipientName());
            }
            if (order.getRecipientPhone() != null) {
                updatedOrder.setRecipientPhone(order.getRecipientPhone());
            }
            if (order.getRemark() != null) {
                updatedOrder.setRemark(order.getRemark());
            }
            if (order.getShippingFee() != null) {
                updatedOrder.setShippingFee(order.getShippingFee());
                // 重新计算最终金额
                if (updatedOrder.getTotalAmount() != null) {
                    updatedOrder.setFinalAmount(updatedOrder.getTotalAmount() + updatedOrder.getShippingFee());
                }
            }
            if (order.getTrackingNumber() != null) {
                updatedOrder.setTrackingNumber(order.getTrackingNumber());
            }
            if (order.getCarrier() != null) {
                updatedOrder.setCarrier(order.getCarrier());
            }
            
            updatedOrder.setUpdatedAt(new Date());
            return orderRepository.save(updatedOrder);
        } else {
            throw new RuntimeException("Order not found with id " + id);
        }
    }

    @Override
    public void deleteOrder(String id) {
        orderRepository.deleteById(id);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id " + id));
    }

    @Override
    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }
}