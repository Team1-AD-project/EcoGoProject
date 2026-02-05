package com.example.EcoGo.service;

import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.model.Goods;
import com.example.EcoGo.model.Order;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.OrderRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.repository.UserVoucherRepository;
import com.example.EcoGo.model.UserVoucher;
import com.example.EcoGo.model.VoucherStatus;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private UserVoucherRepository userVoucherRepository;


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
        
        // ✅ VIP_REQUIRED 强校验：vipLevelRequired=1 的 goods 必须是 VIP 用户
        if (order.getUserId() != null && order.getItems() != null && !order.getItems().isEmpty()) {
            boolean vipActive = isVipActive(order.getUserId());
            for (Order.OrderItem item : order.getItems()) {
                if (item.getGoodsId() == null || item.getGoodsId().isBlank()) continue;

                Goods goods = goodsService.getGoodsById(item.getGoodsId());
                if (goods == null) continue; // 或者 throw GOODS_NOT_FOUND

                if (goods.getVipLevelRequired() != null && goods.getVipLevelRequired() == 1) {
                    if (!vipActive) {
                        throw new RuntimeException("VIP_REQUIRED: " + item.getGoodsId());
                    }
                }
            }
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
    public List<Order> getOrdersByStatus(String status) {
        if (status == null || status.isBlank()) {
            return orderRepository.findAll();
        }
        return orderRepository.findByStatus(status);
    }


    @Override
    public Order updateOrder(String id, Order order) {
        Optional<Order> existingOrder = orderRepository.findById(id);
        if (existingOrder.isPresent()) {
            Order updatedOrder = existingOrder.get();

            // 只更新允许更新的字段
            if (order.getStatus() != null) updatedOrder.setStatus(order.getStatus());
            if (order.getPaymentStatus() != null) updatedOrder.setPaymentStatus(order.getPaymentStatus());
            if (order.getPaymentMethod() != null) updatedOrder.setPaymentMethod(order.getPaymentMethod());
            if (order.getShippingAddress() != null) updatedOrder.setShippingAddress(order.getShippingAddress());
            if (order.getRecipientName() != null) updatedOrder.setRecipientName(order.getRecipientName());
            if (order.getRecipientPhone() != null) updatedOrder.setRecipientPhone(order.getRecipientPhone());
            if (order.getRemark() != null) updatedOrder.setRemark(order.getRemark());

            if (order.getShippingFee() != null) {
                updatedOrder.setShippingFee(order.getShippingFee());
                if (updatedOrder.getTotalAmount() != null) {
                    updatedOrder.setFinalAmount(updatedOrder.getTotalAmount() + updatedOrder.getShippingFee());
                }
            }

            if (order.getTrackingNumber() != null) {
                updatedOrder.setTrackingNumber(order.getTrackingNumber());
            }

            updatedOrder.setUpdatedAt(new Date());
            return orderRepository.save(updatedOrder);
        }
        throw new RuntimeException("ORDER_NOT_FOUND");
    }

    /**
    * 判断用户是否为有效 VIP：vip.is_active=true 且 expiryDate > now
    */
    private boolean isVipActive(String userId) {
        if (userId == null || userId.isBlank()) return false;

        Optional<User> userOpt = userRepository.findByUserid(userId);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findById(userId);
        }
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        User.Vip vip = user.getVip();
        if (vip == null) return false;

        LocalDateTime now = LocalDateTime.now();
        return vip.isActive() && vip.getExpiryDate() != null && vip.getExpiryDate().isAfter(now);
    }

    /**
     * ✅ 积分兑换订单（包含：扣库存 -> 扣积分 -> (如有VIP则开通/续期VIP) -> 保存订单）
     */
    @Override
    public Order createRedemptionOrder(Order order) {

        // 0) 基础校验
        if (order == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "INVALID_ORDER");
        if (order.getUserId() == null || order.getUserId().isEmpty()) throw new BusinessException(ErrorCode.PARAM_ERROR, "MISSING_USER_ID");
        if (order.getItems() == null || order.getItems().isEmpty()) throw new BusinessException(ErrorCode.PARAM_ERROR, "MISSING_ORDER_ITEMS");

        long totalPointsCost = 0L;
        List<String> reservedGoodsIds = new ArrayList<>();
        List<Integer> reservedQty = new ArrayList<>();


        // ✅ 统计本次兑换的 VIP 数量（每个 vip = 30 天）
        int vipQtyTotal = 0;
        List<UserVoucher> vouchersToCreate = new ArrayList<>();

        try {
            String userId = order.getUserId();

            // 1) 校验每个 item，并计算总积分；同时扣库存
            for (Order.OrderItem item : order.getItems()) {
                if (item.getGoodsId() == null || item.getGoodsId().isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "MISSING_GOODS_ID");
                }
                int qty = (item.getQuantity() == null || item.getQuantity() <= 0) ? 1 : item.getQuantity();

                Goods goods = goodsService.getGoodsById(item.getGoodsId());
                if (goods == null) throw new RuntimeException("GOODS_NOT_FOUND: " + item.getGoodsId());
                if (!Boolean.TRUE.equals(goods.getIsForRedemption())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "GOODS_NOT_FOR_REDEMPTION: " + item.getGoodsId());
                }

                // ✅ VIP_REQUIRED：vipLevelRequired=1 => 必须是 VIP 用户
                if (goods.getVipLevelRequired() != null && goods.getVipLevelRequired() == 1) {
                    if (!isVipActive(userId)) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR,"VIP_REQUIRED: " + item.getGoodsId());
                    }
                }

                int pointsPerUnit = (goods.getRedemptionPoints() == null) ? 0 : goods.getRedemptionPoints();
                if (pointsPerUnit <= 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "INVALID_REDEMPTION_POINTS: " + item.getGoodsId());

                long cost = (long) pointsPerUnit * (long) qty;
                totalPointsCost += cost;

                // 先扣库存（失败应由 reserveStock 抛异常）
                boolean isVirtual = "vip".equalsIgnoreCase(goods.getType())
                        || "voucher".equalsIgnoreCase(goods.getType());

                if (!isVirtual) {
                    goodsService.reserveStock(item.getGoodsId(), qty);
                    reservedGoodsIds.add(item.getGoodsId());
                    reservedQty.add(qty);
                }


                // 兑换订单项：price/subtotal 用“积分”
                item.setGoodsName(goods.getName());
                item.setPrice((double) pointsPerUnit);
                item.setSubtotal((double) pointsPerUnit * qty);
                item.setQuantity(qty);

                // ✅ 判断是否为 vip 商品（名字等于 vip，忽略大小写）
                // ✅ VIP：用 type 判断（更稳）
                if ("vip".equalsIgnoreCase(goods.getType())) {
                    vipQtyTotal += qty;
                }

                // ✅ Voucher：用 type 判断，先收集，扣完积分后再落库
                if ("voucher".equalsIgnoreCase(goods.getType())) {
                    for (int i = 0; i < qty; i++) {
                        UserVoucher uv = new UserVoucher();
                        uv.setUserId(userId); // 注意：这里用的是 order.getUserId()，建议你把 userId 提前定义
                        uv.setGoodsId(goods.getId());
                        uv.setVoucherName(goods.getName());
                        uv.setImageUrl(goods.getImageUrl());
                        uv.setStatus(VoucherStatus.ACTIVE);
                        vouchersToCreate.add(uv);
                    }
                }   

            }
            

            // 2) 扣积分（并写 userpointlog：source/store + description）
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

            // 3) ✅ 如果包含 VIP：开通/续期（每个 vip = 30 天）
            if (vipQtyTotal > 0) {
                int days = vipQtyTotal * 30;
                activateVipInternal(userId, days);
            }

            // ✅ 创建用户券（统一 30 天有效期）
            LocalDateTime now = LocalDateTime.now();
            for (UserVoucher uv : vouchersToCreate) {
                uv.setIssuedAt(now);
                uv.setExpiresAt(now.plusDays(30));
                uv.setCreatedAt(now);
                uv.setUpdatedAt(now);
                userVoucherRepository.save(uv);
            }


            // 4) 建订单（兑换订单：金额0、支付状态已支付、记录 pointsUsed）
            order.setIsRedemptionOrder(true);
            order.setTotalAmount((double) totalPointsCost);
            order.setShippingFee(0.0);
            order.setFinalAmount((double) totalPointsCost);

            if (totalPointsCost > Integer.MAX_VALUE) throw new RuntimeException("POINTS_COST_TOO_LARGE");
            order.setPointsUsed((int) totalPointsCost);

            order.setPaymentStatus("PAID");
            if (order.getStatus() == null || order.getStatus().isEmpty()) {
                order.setStatus("PENDING");
            }
            order.setPaymentMethod("POINTS");

            if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
                String orderNumber = "ORD" + System.currentTimeMillis()
                        + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                order.setOrderNumber(orderNumber);
            }

            order.setCreatedAt(new Date());
            order.setUpdatedAt(new Date());

            return orderRepository.save(order);

        } catch (Exception e) {

            // ✅ 发生任何异常：尽量回滚库存
            for (int i = 0; i < reservedGoodsIds.size(); i++) {
                try {
                    goodsService.releaseStock(reservedGoodsIds.get(i), reservedQty.get(i));
                } catch (Exception ignore) {}
            }

            // ✅ 若已扣过积分，也尽量退回（这里无法100%判断是否扣成功，所以“尽力而为”）
            //    规则：如果失败发生在扣积分之后，这里可以把 totalPointsCost 加回去
            try {
                if (order != null && order.getUserId() != null && totalPointsCost > 0) {
                    pointsService.adjustPoints(
                            order.getUserId(),
                            totalPointsCost,
                            "store",
                            "Rollback redemption: " + safeMsg(e),
                            null,
                            null
                    );
                }
            } catch (Exception ignore) {}

            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e.getMessage());
        }
    }

    /**
     * ✅ 内部VIP激活：若用户当前VIP未过期 -> 在 expiryDate 上续期；否则从现在开始
     */
    private void activateVipInternal(String userId, int durationDays) {
        if (durationDays <= 0) return;

        // 你们项目里 userId 一般是 users.userid（不是 _id）
        // 如果你 UserRepository 没有 findByUserid，请在 UserRepository 加这个方法
        Optional<User> userOpt = userRepository.findByUserid(userId);
        if (userOpt.isEmpty()) {
            // 兜底：有的项目用 _id 作为 userId
            userOpt = userRepository.findById(userId);
        }
        User user = userOpt.orElseThrow(() -> new RuntimeException("USER_NOT_FOUND: " + userId));

        LocalDateTime now = LocalDateTime.now();

        User.Vip vip = user.getVip();
        if (vip == null) {
            vip = new User.Vip();
            user.setVip(vip);
        }

        LocalDateTime currentExpiry = vip.getExpiryDate();

        // 判断当前VIP是否仍有效
        boolean stillActive = vip.isActive() && currentExpiry != null && currentExpiry.isAfter(now);

        if (stillActive) {
            vip.setExpiryDate(currentExpiry.plusDays(durationDays));
        } else {
            vip.setActive(true);
            vip.setStartDate(now);
            vip.setExpiryDate(now.plusDays(durationDays));
            vip.setPlan("vip");
            vip.setAutoRenew(false);
            vip.setPointsMultiplier(2); // 如果你们 VIP 是 2x，可改；不需要就设回 1
        }

        user.setUpdatedAt(now);
        userRepository.save(user);
    }

    private String safeMsg(Exception e) {
        String m = e.getMessage();
        return (m == null) ? e.getClass().getSimpleName() : m;
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
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("ORDER_NOT_FOUND"));
    }

    @Override
    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }
}
