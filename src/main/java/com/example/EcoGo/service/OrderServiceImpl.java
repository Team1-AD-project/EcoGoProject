package com.example.EcoGo.service;

import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.interfacemethods.VipSwitchService;
import com.example.EcoGo.model.Goods;
import com.example.EcoGo.model.Order;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserVoucher;
import com.example.EcoGo.model.VoucherStatus;
import com.example.EcoGo.repository.OrderRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.repository.UserVoucherRepository;
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

    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";

    @Autowired
    private VipSwitchService vipSwitchService;

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
        generateOrderNumber(order);
        validateVipRequirements(order);
        calculateOrderAmount(order);
        setDefaultStatus(order);
        setTimestamps(order);

        return orderRepository.save(order);
    }

    private void generateOrderNumber(Order order) {
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            String orderNumber = "ORD" + System.currentTimeMillis() +
                    UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            order.setOrderNumber(orderNumber);
        }
    }

    private void validateVipRequirements(Order order) {
        if (order == null) return;
        if (order.getUserId() == null || order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        boolean vipActive = isVipActive(order.getUserId());
        for (Order.OrderItem item : order.getItems()) {
            if (item == null || item.getGoodsId() == null || item.getGoodsId().isBlank()) {
                continue;
            }

            Goods goods = goodsService.getGoodsById(item.getGoodsId());
            if (goods == null) {
                continue;
            }

            if (isVipRequired(goods) && !vipActive) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "VIP_REQUIRED: " + item.getGoodsId());
            }
        }
    }

    private boolean isVipRequired(Goods goods) {
        return goods != null && goods.getVipLevelRequired() != null && goods.getVipLevelRequired() == 1;
    }

    private void calculateOrderAmount(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        double total = order.getItems().stream()
                .mapToDouble(this::calculateItemPrice)
                .sum();

        order.setTotalAmount(total);
        double shippingFee = order.getShippingFee() != null ? order.getShippingFee() : 0.0;
        order.setFinalAmount(total + shippingFee);
    }

    private double calculateItemPrice(Order.OrderItem item) {
        if (item == null) return 0.0;
        if (item.getSubtotal() != null) {
            return item.getSubtotal();
        }
        if (item.getPrice() != null && item.getQuantity() != null) {
            return item.getPrice() * item.getQuantity();
        }
        return 0.0;
    }

    private void setDefaultStatus(Order order) {
        if (order == null) return;
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus(ORDER_STATUS_COMPLETED);
        }
        if (order.getPaymentStatus() == null || order.getPaymentStatus().isEmpty()) {
            order.setPaymentStatus(ORDER_STATUS_COMPLETED);
        }
    }

    private void setTimestamps(Order order) {
        if (order == null) return;
        Date now = new Date();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
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
     * ✅ 已重构：降低 Cognitive Complexity
     */
    @Override
    public Order createRedemptionOrder(Order order) {
        validateRedemptionOrderBase(order);

        RollbackContext rb = new RollbackContext();
        try {
            String userId = order.getUserId();

            RedemptionPlan plan = buildRedemptionPlanAndReserveStock(order, userId, rb);

            deductPoints(userId, plan.getTotalPointsCost(), order, rb);

            if (plan.getVipQtyTotal() > 0) {
                activateVipInternal(userId, plan.getVipQtyTotal() * 30);
            }

            createUserVouchers(plan.getVouchersToCreate());

            Order saved = finalizeAndSaveRedemptionOrder(order, plan.getTotalPointsCost());

            rb.commit();
            return saved;

        } catch (Exception e) {
            rb.rollbackAll(this, goodsService, pointsService, order, e);
            rethrowPreservingType(e);
            return null; // unreachable
        }
    }

    // =========================
    // Redemption: Data holders
    // =========================
    private static class RedemptionPlan {
        private final long totalPointsCost;
        private final int vipQtyTotal;
        private final List<UserVoucher> vouchersToCreate;

        private RedemptionPlan(long totalPointsCost, int vipQtyTotal, List<UserVoucher> vouchersToCreate) {
            this.totalPointsCost = totalPointsCost;
            this.vipQtyTotal = vipQtyTotal;
            this.vouchersToCreate = vouchersToCreate;
        }

        long getTotalPointsCost() {
            return totalPointsCost;
        }

        int getVipQtyTotal() {
            return vipQtyTotal;
        }

        List<UserVoucher> getVouchersToCreate() {
            return vouchersToCreate;
        }
    }

    private static class ItemProcessResult {
        private final long costPoints;
        private final int vipQty;
        private final List<UserVoucher> vouchers;

        private ItemProcessResult(long costPoints, int vipQty, List<UserVoucher> vouchers) {
            this.costPoints = costPoints;
            this.vipQty = vipQty;
            this.vouchers = vouchers;
        }

        long getCostPoints() {
            return costPoints;
        }

        int getVipQty() {
            return vipQty;
        }

        List<UserVoucher> getVouchers() {
            return vouchers;
        }
    }

    private static class RollbackContext {
        private final List<String> reservedGoodsIds = new ArrayList<>();
        private final List<Integer> reservedQty = new ArrayList<>();
        private boolean pointsDeducted = false;
        private long deductedPoints = 0L;
        private String deductedUserId = null;

        void addReservedStock(String goodsId, int qty) {
            reservedGoodsIds.add(goodsId);
            reservedQty.add(qty);
        }

        void markPointsDeducted(String userId, long points) {
            this.pointsDeducted = true;
            this.deductedPoints = points;
            this.deductedUserId = userId;
        }

        void commit() {
            // success: no-op
        }

        void rollbackAll(
                OrderServiceImpl self,
                GoodsService goodsService,
                PointsService pointsService,
                Order order,
                Exception e
        ) {
            rollbackStock(goodsService);
            rollbackPoints(self, pointsService, order, e);
        }

        private void rollbackStock(GoodsService goodsService) {
            for (int i = 0; i < reservedGoodsIds.size(); i++) {
                try {
                    goodsService.releaseStock(reservedGoodsIds.get(i), reservedQty.get(i));
                } catch (Exception ignore) {
                    // swallow
                }
            }
        }

        private void rollbackPoints(OrderServiceImpl self, PointsService pointsService, Order order, Exception e) {
            if (!pointsDeducted) return;

            String userId = (deductedUserId != null) ? deductedUserId : (order != null ? order.getUserId() : null);
            if (userId == null || deductedPoints <= 0) return;

            try {
                pointsService.adjustPoints(
                        userId,
                        deductedPoints,
                        "store",
                        "Rollback redemption: " + self.safeMsg(e),
                        null,
                        null
                );
            } catch (Exception ignore) {
                // swallow
            }
        }
    }

    // =========================
    // Redemption: Orchestration helpers
    // =========================
    private void validateRedemptionOrderBase(Order order) {
        if (order == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "INVALID_ORDER");
        if (order.getUserId() == null || order.getUserId().isEmpty())
            throw new BusinessException(ErrorCode.PARAM_ERROR, "MISSING_USER_ID");
        if (order.getItems() == null || order.getItems().isEmpty())
            throw new BusinessException(ErrorCode.PARAM_ERROR, "MISSING_ORDER_ITEMS");
    }

    private RedemptionPlan buildRedemptionPlanAndReserveStock(Order order, String userId, RollbackContext rb) {
        long totalPointsCost = 0L;
        int vipQtyTotal = 0;
        List<UserVoucher> vouchersToCreate = new ArrayList<>();

        for (Order.OrderItem item : order.getItems()) {
            ItemProcessResult r = processOneRedemptionItem(item, userId, rb);
            totalPointsCost += r.getCostPoints();
            vipQtyTotal += r.getVipQty();
            vouchersToCreate.addAll(r.getVouchers());
        }

        return new RedemptionPlan(totalPointsCost, vipQtyTotal, vouchersToCreate);
    }

    private ItemProcessResult processOneRedemptionItem(Order.OrderItem item, String userId, RollbackContext rb) {
        String goodsId = requireGoodsId(item);
        int qty = normalizeQty(item);

        Goods goods = requireGoods(goodsId);
        ensureGoodsRedeemable(goods, goodsId);

        enforceVipExclusiveRules(goods, userId, goodsId);

        int pointsPerUnit = requireRedemptionPoints(goods, goodsId);
        long cost = (long) pointsPerUnit * (long) qty;

        if (!isVirtualGoods(goods)) {
            goodsService.reserveStock(goodsId, qty);
            rb.addReservedStock(goodsId, qty);
        }

        fillRedemptionOrderItem(item, goods, qty, pointsPerUnit);

        int vipQty = isVipSubscription(goods) ? qty : 0;
        List<UserVoucher> vouchers = isVoucher(goods)
                ? buildUserVouchers(userId, goods, qty)
                : List.of();

        return new ItemProcessResult(cost, vipQty, vouchers);
    }

    private void deductPoints(String userId, long totalPointsCost, Order order, RollbackContext rb) {
        String description = buildPurchasedDescription(order);

        pointsService.adjustPoints(
                userId,
                -totalPointsCost,
                "store",
                description,
                null,
                null
        );
        rb.markPointsDeducted(userId, totalPointsCost);
    }

    private String buildPurchasedDescription(Order order) {
        List<String> purchasedNames = new ArrayList<>();
        for (Order.OrderItem item : order.getItems()) {
            int qty = normalizeQty(item);
            String name = (item.getGoodsName() != null && !item.getGoodsName().isBlank())
                    ? item.getGoodsName()
                    : item.getGoodsId();
            purchasedNames.add(name + " x" + qty);
        }
        return "Purchased " + String.join(", ", purchasedNames);
    }

    private void createUserVouchers(List<UserVoucher> vouchersToCreate) {
        if (vouchersToCreate == null || vouchersToCreate.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (UserVoucher uv : vouchersToCreate) {
            uv.setIssuedAt(now);
            uv.setExpiresAt(now.plusDays(30));
            uv.setCreatedAt(now);
            uv.setUpdatedAt(now);
            userVoucherRepository.save(uv);
        }
    }

    private Order finalizeAndSaveRedemptionOrder(Order order, long totalPointsCost) {
        order.setIsRedemptionOrder(true);

        // 兑换订单：金额字段用“积分成本”（保持你原逻辑）
        order.setTotalAmount((double) totalPointsCost);
        order.setShippingFee(0.0);
        order.setFinalAmount((double) totalPointsCost);

        if (totalPointsCost > Integer.MAX_VALUE) throw new RuntimeException("POINTS_COST_TOO_LARGE");
        order.setPointsUsed((int) totalPointsCost);

        order.setPaymentStatus("PAID");
        order.setStatus(ORDER_STATUS_COMPLETED);
        order.setPaymentMethod("POINTS");

        generateOrderNumber(order);

        Date now = new Date();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        return orderRepository.save(order);
    }

    private void rethrowPreservingType(Exception e) {
        if (e instanceof BusinessException) throw (BusinessException) e;
        if (e instanceof RuntimeException) throw (RuntimeException) e;
        throw new RuntimeException(e.getMessage(), e);
    }

    // =========================
    // Redemption: Micro helpers
    // =========================
    private String requireGoodsId(Order.OrderItem item) {
        if (item == null || item.getGoodsId() == null || item.getGoodsId().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "MISSING_GOODS_ID");
        }
        return item.getGoodsId();
    }

    private int normalizeQty(Order.OrderItem item) {
        if (item == null) return 1;
        Integer q = item.getQuantity();
        return (q == null || q <= 0) ? 1 : q;
    }

    private Goods requireGoods(String goodsId) {
        Goods goods = goodsService.getGoodsById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "GOODS_NOT_FOUND: " + goodsId);
        }
        return goods;
    }

    private void ensureGoodsRedeemable(Goods goods, String goodsId) {
        if (!Boolean.TRUE.equals(goods.getIsForRedemption())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "GOODS_NOT_FOR_REDEMPTION: " + goodsId);
        }
    }

    private int requireRedemptionPoints(Goods goods, String goodsId) {
        int pointsPerUnit = (goods.getRedemptionPoints() == null) ? 0 : goods.getRedemptionPoints();
        if (pointsPerUnit <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "INVALID_REDEMPTION_POINTS: " + goodsId);
        }
        return pointsPerUnit;
    }

    /**
     * VIP-exclusive 兑换校验（只在兑换阶段拦）
     * 规则：
     * - VIP 商品定义：vipLevelRequired == 1
     * - 全局开关：goods 用 Exclusive_goods；voucher 用 Exclusive_vouchers
     * - vip订阅商品(type=vip)必须放行（否则普通用户无法购买VIP）
     */
    private void enforceVipExclusiveRules(Goods goods, String userId, String goodsId) {
        if (!isVipExclusive(goods)) return;
        if (isVipSubscription(goods)) return;

        boolean enabled = isVoucher(goods)
                ? vipSwitchService.isSwitchEnabled("Exclusive_vouchers")
                : vipSwitchService.isSwitchEnabled("Exclusive_goods");

        if (!enabled) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "VIP_DISABLED: " + goodsId);
        }
        if (!isVipActive(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "VIP_REQUIRED: " + goodsId);
        }
    }

    private boolean isVipExclusive(Goods goods) {
        return goods.getVipLevelRequired() != null && goods.getVipLevelRequired() == 1;
    }

    private boolean isVipSubscription(Goods goods) {
        return "vip".equalsIgnoreCase(goods.getType());
    }

    private boolean isVoucher(Goods goods) {
        return "voucher".equalsIgnoreCase(goods.getType());
    }

    private boolean isVirtualGoods(Goods goods) {
        return isVipSubscription(goods) || isVoucher(goods);
    }

    private void fillRedemptionOrderItem(Order.OrderItem item, Goods goods, int qty, int pointsPerUnit) {
        item.setGoodsName(goods.getName());
        item.setPrice((double) pointsPerUnit);
        item.setSubtotal((double) pointsPerUnit * qty);
        item.setQuantity(qty);
    }

    private List<UserVoucher> buildUserVouchers(String userId, Goods goods, int qty) {
        List<UserVoucher> list = new ArrayList<>(Math.max(qty, 1));
        for (int i = 0; i < qty; i++) {
            UserVoucher uv = new UserVoucher();
            uv.setUserId(userId);
            uv.setGoodsId(goods.getId());
            uv.setVoucherName(goods.getName());
            uv.setImageUrl(goods.getImageUrl());
            uv.setStatus(VoucherStatus.ACTIVE);
            list.add(uv);
        }
        return list;
    }

    /**
     * ✅ 内部VIP激活：若用户当前VIP未过期 -> 在 expiryDate 上续期；否则从现在开始
     */
    private void activateVipInternal(String userId, int durationDays) {
        if (durationDays <= 0) return;

        // 你们项目里 userId 一般是 users.userid（不是 _id）
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
