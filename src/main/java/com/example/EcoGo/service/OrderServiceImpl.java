package com.example.EcoGo.service;

import com.example.EcoGo.model.Order;
import com.example.EcoGo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.model.Goods;
import java.util.ArrayList;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.model.User;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private UserRepository userRepository;

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
    public Order createRedemptionOrder(Order order) {

        // 0) 基础校验
        if (order == null) {
            throw new RuntimeException("INVALID_ORDER");
        }
        if (order.getUserId() == null || order.getUserId().isEmpty()) {
            throw new RuntimeException("MISSING_USER_ID");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new RuntimeException("MISSING_ORDER_ITEMS");
        }

        // 1) 校验每个 item，并计算总积分；同时准备扣库存（先逐个扣）
        long totalPointsCost = 0L;
        List<String> reservedGoodsIds = new ArrayList<>();
        List<Integer> reservedQty = new ArrayList<>();

        try {
            for (Order.OrderItem item : order.getItems()) {
                if (item.getGoodsId() == null || item.getGoodsId().isEmpty()) {
                    throw new RuntimeException("MISSING_GOODS_ID");
                }
                int qty = (item.getQuantity() == null || item.getQuantity() <= 0) ? 1 : item.getQuantity();

                Goods goods = goodsService.getGoodsById(item.getGoodsId());
                if (goods == null) {
                    throw new RuntimeException("GOODS_NOT_FOUND: " + item.getGoodsId());
                }
                if (!Boolean.TRUE.equals(goods.getIsForRedemption())) {
                    throw new RuntimeException("GOODS_NOT_FOR_REDEMPTION: " + item.getGoodsId());
                }

                // 计算积分成本（并验证积分价格合法）
                int pointsPerUnit = (goods.getRedemptionPoints() == null) ? 0 : goods.getRedemptionPoints();
                if (pointsPerUnit <= 0) {
                    throw new RuntimeException("INVALID_REDEMPTION_POINTS: " + item.getGoodsId());
                }

                long cost = (long) pointsPerUnit * (long) qty;
                totalPointsCost += cost;

                // 2) 先扣库存（原子），失败直接抛 OUT_OF_STOCK
                goodsService.reserveStock(item.getGoodsId(), qty);
                reservedGoodsIds.add(item.getGoodsId());
                reservedQty.add(qty);

                // ✅ 兑换订单项：price / subtotal 表示“积分”
                item.setGoodsName(goods.getName());
                item.setPrice((double) pointsPerUnit);
                item.setSubtotal((double) pointsPerUnit * qty);
                item.setQuantity(qty);

            }

            // 3) 再扣积分
            String userId = order.getUserId();

            List<String> purchasedNames = new ArrayList<>();
            for (Order.OrderItem item : order.getItems()) {
                int qty = (item.getQuantity() == null || item.getQuantity() <= 0) ? 1 : item.getQuantity();
                String name = (item.getGoodsName() != null && !item.getGoodsName().isBlank())
                    ? item.getGoodsName()
                    : item.getGoodsId();
                purchasedNames.add(name + " x" + qty);
            }
            String description = "Purchased " + String.join(", ", purchasedNames);

            pointsService.adjustPoints(
                userId,
                -totalPointsCost,
                "store",
                description,
                null,   
                null    
            );


        } catch (Exception e) {
            // 任何异常：回滚已扣的库存
            for (int i = 0; i < reservedGoodsIds.size(); i++) {
                try {
                    goodsService.releaseStock(reservedGoodsIds.get(i), reservedQty.get(i));
                } catch (Exception ignore) {
                    // 回滚失败也不覆盖原异常
                }
            }
            // 原异常继续抛出，让 controller 返回 400
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e.getMessage());
        }

        // 4) 建订单（兑换订单：金额 0、支付状态已支付、记录 pointsUsed）
        order.setIsRedemptionOrder(true);
        order.setTotalAmount((double) totalPointsCost);
        order.setShippingFee(0.0);
        order.setFinalAmount((double) totalPointsCost);

        // pointsUsed 是 Integer
        if (totalPointsCost > Integer.MAX_VALUE) {
            throw new RuntimeException("POINTS_COST_TOO_LARGE");
        }
        order.setPointsUsed((int) totalPointsCost);

        // 你们原逻辑默认 status/paymentStatus 是 PENDING，这里兑换支付已经完成
        order.setPaymentStatus("PAID");
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus("PENDING");
        }
        order.setPaymentMethod("POINTS");

        // 订单号生成沿用 createOrder 的规则（为了保持一致性）
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            String orderNumber = "ORD" + System.currentTimeMillis()
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            order.setOrderNumber(orderNumber);
        }

        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());

        return orderRepository.save(order);
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