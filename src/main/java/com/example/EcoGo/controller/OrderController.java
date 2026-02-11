package com.example.EcoGo.controller;

import com.example.EcoGo.dto.OrderRequestDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.Order;
import com.example.EcoGo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 获取所有订单（支持筛选）
    @GetMapping
    public ResponseMessage<Map<String, Object>> getAllOrders(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isRedemption,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page <= 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "page must be >= 1");
        if (size <= 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "size must be >= 1");

        List<Order> orders;

        // 根据参数筛选
        if (userId != null && !userId.isEmpty()) {
            orders = orderService.getOrdersByUserId(userId);
        } else if (status != null && !status.isEmpty()) {
            orders = orderService.getOrdersByStatus(status);
        } else {
            orders = orderService.getAllOrders();
        }

        if (orders == null) {
            orders = Collections.emptyList();
        }

        // 兑换订单筛选
        if (isRedemption != null) {
            orders = orders.stream()
                    .filter(order -> isRedemption.equals(order.getIsRedemptionOrder()))
                    .collect(Collectors.toList());
        }

        // 按创建时间倒序排序（避免 createdAt 为空时 NPE）
        orders.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        // 分页逻辑
        int total = orders.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<Order> pageOrders;
        if (fromIndex < total) {
            pageOrders = orders.subList(fromIndex, toIndex);
        } else {
            pageOrders = Collections.emptyList();
        }

        Map<String, Object> pagination = Map.of(
                "page", page,
                "size", size,
                "total", total,
                "totalPages", (int) Math.ceil((double) total / size)
        );

        Map<String, Object> data = new HashMap<>();
        data.put("orders", pageOrders);
        data.put("pagination", pagination);

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), "获取订单列表成功", data);
    }

    // 获取订单详情
    @GetMapping("/{id}")
    public ResponseMessage<Order> getOrderById(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "id");
        }

        Order order = orderService.getOrderById(id);

        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), "获取订单详情成功", order);
    }

    // 创建兑换订单
    @PostMapping("/redemption")
    public ResponseMessage<Order> createRedemptionOrder(@RequestBody OrderRequestDto dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "request body is empty");
        }

        Order order = dto.toEntity();
        // 设置为兑换订单
        order.setIsRedemptionOrder(true);

        Order createdOrder = orderService.createRedemptionOrder(order);

        if (createdOrder == null) {
            // 这里按"系统/数据库失败"处理更合理（业务失败应由 service 抛 BusinessException）
            throw new BusinessException(ErrorCode.DB_ERROR);
        }

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), "兑换订单创建成功", createdOrder);
    }

    // 更新订单（含状态等字段）
    @PutMapping("/{id}")
    public ResponseMessage<Order> updateOrder(@PathVariable String id, @RequestBody OrderRequestDto dto) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "id");
        }
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "request body is empty");
        }

        Order order = dto.toEntity();
        Order updatedOrder = orderService.updateOrder(id, order);

        if (updatedOrder == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), "订单更新成功", updatedOrder);
    }

    // 删除订单
    @DeleteMapping("/{id}")
    public ResponseMessage<Void> deleteOrder(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "id");
        }

        orderService.deleteOrder(id);

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), "订单删除成功", null);
    }

    // Mobile端专用 - 用户订单历史
    @GetMapping("/mobile/user/{userId}")
    public ResponseMessage<Map<String, Object>> getUserOrderHistoryForMobile(
            @PathVariable String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "userId");
        }
        if (page <= 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "page must be >= 1");
        if (size <= 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "size must be >= 1");

        List<Order> userOrders = orderService.getOrdersByUserId(userId);
        if (userOrders == null) userOrders = Collections.emptyList();

        // 状态筛选
        if (status != null && !status.isEmpty()) {
            userOrders = userOrders.stream()
                    .filter(order -> status.equals(order.getStatus()))
                    .collect(Collectors.toList());
        }

        // 按创建时间倒序排序（避免 createdAt 为空时 NPE）
        userOrders.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

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

        Map<String, Object> pagination = Map.of(
                "page", page,
                "size", size,
                "total", total,
                "totalPages", (int) Math.ceil((double) total / size)
        );

        Map<String, Object> data = new HashMap<>();
        data.put("orders", simplifiedOrders);
        data.put("pagination", pagination);

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), "获取用户订单历史成功", data);
    }

    // 更新订单状态（单独接口）
    @PutMapping("/{id}/status")
    public ResponseMessage<Order> updateOrderStatus(
            @PathVariable String id,
            @RequestParam String status) {

        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "id");
        }
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "status");
        }

        Order patch = new Order();
        patch.setStatus(status);

        Order updatedOrder = orderService.updateOrder(id, patch);

        if (updatedOrder == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), "订单状态更新成功", updatedOrder);
    }
}
