package com.example.EcoGo.controller;

import com.example.EcoGo.model.Order;
import com.example.EcoGo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 获取所有订单（支持筛选）
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isRedemption,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<Order> orders;
            
            // 根据参数筛选
            if (userId != null && !userId.isEmpty()) {
                orders = orderService.getOrdersByUserId(userId);
            } else if (status != null && !status.isEmpty()) {
                orders = orderService.getOrdersByStatus(status);
            } else {
                orders = orderService.getAllOrders();
            }
            
            // 兑换订单筛选
            if (isRedemption != null) {
                orders = orders.stream()
                    .filter(order -> isRedemption.equals(order.getIsRedemptionOrder()))
                    .collect(Collectors.toList());
            }
            
            // 按创建时间倒序排序
            orders.sort(Comparator.comparing(Order::getCreatedAt).reversed());
            
            // 分页逻辑
            int total = orders.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            if (fromIndex < total) {
                orders = orders.subList(fromIndex, toIndex);
            } else {
                orders = Collections.emptyList();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取订单列表成功");
            response.put("data", orders);
            response.put("pagination", Map.of(
                "page", page,
                "size", size,
                "total", total,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取订单列表失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    //  获取订单详情
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable String id) {
        try {
            Order order = orderService.getOrderById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取订单详情成功");
            response.put("data", order);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 404);
            errorResponse.put("message", "订单未找到");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取订单详情失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    // 创建兑换订单
    @PostMapping("/redemption")
    public ResponseEntity<Map<String, Object>> createRedemptionOrder(@RequestBody Order order) {
        try {
            // 设置为兑换订单
            order.setIsRedemptionOrder(true);
            
            Order createdOrder = orderService.createRedemptionOrder(order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "兑换订单创建成功");
            response.put("data", createdOrder);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", "兑换失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "服务器内部错误");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 5. 更新订单状态
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateOrder(@PathVariable String id, @RequestBody Order order) {
        try {
            Order updatedOrder = orderService.updateOrder(id, order);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "订单更新成功");
            response.put("data", updatedOrder);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 404);
            errorResponse.put("message", "订单未找到");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", "更新订单失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    //  删除订单
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteOrder(@PathVariable String id) {
        try {
            orderService.deleteOrder(id);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "订单删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "删除订单失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    //  Mobile端专用 - 用户订单历史
    @GetMapping("/mobile/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserOrderHistoryForMobile(
            @PathVariable String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<Order> userOrders = orderService.getOrdersByUserId(userId);
            
            // 状态筛选
            if (status != null && !status.isEmpty()) {
                userOrders = userOrders.stream()
                    .filter(order -> status.equals(order.getStatus()))
                    .collect(Collectors.toList());
            }
            
            // 按创建时间倒序排序
            userOrders.sort(Comparator.comparing(Order::getCreatedAt).reversed());
            
            // 分页
            int total = userOrders.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<Order> pageOrders;
            if (fromIndex < total) {
                pageOrders = userOrders.subList(fromIndex, toIndex);
            } else {
                pageOrders = Collections.emptyList();
            }
            
            // 简化返回数据
            List<Map<String, Object>> simplifiedOrders = pageOrders.stream()
                .map(order -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", order.getId());
                    item.put("orderNumber", order.getOrderNumber());
                    item.put("status", order.getStatus());
                    item.put("finalAmount", order.getFinalAmount());
                    item.put("createdAt", order.getCreatedAt());
                    item.put("itemCount", order.getItems() != null ? order.getItems().size() : 0);
                    item.put("isRedemption", order.getIsRedemptionOrder());
                    item.put("trackingNumber", order.getTrackingNumber());
                    item.put("carrier", order.getCarrier());
                    return item;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取用户订单历史成功");
            response.put("data", simplifiedOrders);
            response.put("pagination", Map.of(
                "page", page,
                "size", size,
                "total", total,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取订单历史失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    //  更新订单状态（单独接口）
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable String id,
            @RequestParam String status) {
        try {
            Order order = new Order();
            order.setStatus(status);
            Order updatedOrder = orderService.updateOrder(id, order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "订单状态更新成功");
            response.put("data", updatedOrder);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 404);
            errorResponse.put("message", "订单未找到");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", "更新订单状态失败");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}