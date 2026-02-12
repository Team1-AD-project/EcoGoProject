package com.example.EcoGo.service;

import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.interfacemethods.VipSwitchService;
import com.example.EcoGo.model.Goods;
import com.example.EcoGo.model.Order;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserVoucher;
import com.example.EcoGo.repository.OrderRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.repository.UserVoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    private OrderServiceImpl service;

    private VipSwitchService vipSwitchService;
    private UserVoucherRepository userVoucherRepository;
    private OrderRepository orderRepository;
    private GoodsService goodsService;
    private PointsService pointsService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        vipSwitchService = mock(VipSwitchService.class);
        userVoucherRepository = mock(UserVoucherRepository.class);
        orderRepository = mock(OrderRepository.class);
        goodsService = mock(GoodsService.class);
        pointsService = mock(PointsService.class);
        userRepository = mock(UserRepository.class);

        service = new OrderServiceImpl();

        inject("vipSwitchService", vipSwitchService);
        inject("userVoucherRepository", userVoucherRepository);
        inject("orderRepository", orderRepository);
        inject("goodsService", goodsService);
        inject("pointsService", pointsService);
        inject("userRepository", userRepository);
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field f = OrderServiceImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, value);
    }

    private static Goods goods(String id, String type, int points, Integer vipReq, boolean forRedemption) {
        Goods g = new Goods();
        g.setId(id);
        g.setType(type);
        g.setName("G-" + id);
        g.setIsForRedemption(forRedemption);
        g.setRedemptionPoints(points);
        g.setVipLevelRequired(vipReq);
        g.setImageUrl("img");
        return g;
    }

    private static Order redemptionOrder(String userId, String goodsId, Integer qty) {
        Order o = new Order();
        o.setUserId(userId);

        Order.OrderItem item = new Order.OrderItem();
        item.setGoodsId(goodsId);
        item.setQuantity(qty);

        o.setItems(List.of(item));
        return o;
    }

    // ---------- createRedemptionOrder 参数校验 ----------
    @Test
    void createRedemptionOrder_nullOrder_throwParamError() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(null));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createRedemptionOrder_missingUserId_throwParamError() {
        Order o = redemptionOrder(null, "g1", 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createRedemptionOrder_missingItems_throwParamError() {
        Order o = new Order();
        o.setUserId("u1");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    // ---------- 普通 goods：扣库存 + 扣积分 + 保存订单 ----------
    @Test
    void createRedemptionOrder_normalGoods_shouldReserveStock_andDeductPoints_andSaveOrder() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "g1", 2);

        Goods g1 = goods("g1", "normal", 100, 0, true);
        when(goodsService.getGoodsById("g1")).thenReturn(g1);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order saved = service.createRedemptionOrder(o);

        verify(goodsService).reserveStock("g1", 2);
        verify(pointsService).adjustPoints(eq(userId), eq(-200L), eq("store"), contains("Purchased"), isNull(), isNull());
        verify(orderRepository).save(any(Order.class));

        // voucher 不创建
        verify(userVoucherRepository, never()).save(any(UserVoucher.class));

        assertEquals(Boolean.TRUE, saved.getIsRedemptionOrder());
        assertEquals("POINTS", saved.getPaymentMethod());
        assertEquals(200, saved.getPointsUsed());
    }

    // ---------- voucher：不扣库存，但创建 userVoucher qty 次 ----------
    @Test
    void createRedemptionOrder_voucher_shouldCreateUserVouchers_andDeductPoints() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "v1", 3);

        Goods v1 = goods("v1", "voucher", 50, 0, true);
        when(goodsService.getGoodsById("v1")).thenReturn(v1);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createRedemptionOrder(o);

        verify(goodsService, never()).reserveStock(anyString(), anyInt());
        verify(pointsService).adjustPoints(eq(userId), eq(-150L), eq("store"), contains("Purchased"), isNull(), isNull());
        verify(userVoucherRepository, times(3)).save(any(UserVoucher.class));
    }

    // ---------- vip：不扣库存，但应激活/续期 vip（userRepository.save 被调用） ----------
    @Test
    void createRedemptionOrder_vip_shouldActivateVip() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "vip1", 1);

        Goods vip = goods("vip1", "vip", 300, 0, true);
        when(goodsService.getGoodsById("vip1")).thenReturn(vip);

        User user = new User();
        user.setUserid(userId);
        User.Vip v = new User.Vip();
        v.setActive(false);
        v.setExpiryDate(LocalDateTime.now().minusDays(1));
        user.setVip(v);

        when(userRepository.findByUserid(userId)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createRedemptionOrder(o);

        verify(goodsService, never()).reserveStock(anyString(), anyInt());
        verify(pointsService).adjustPoints(eq(userId), eq(-300L), eq("store"), contains("Purchased"), isNull(), isNull());
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    // ---------- VIP-exclusive：switch 关 => VIP_DISABLED ----------
    @Test
    void createRedemptionOrder_vipExclusive_switchOff_shouldThrowVipDisabled() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "gx", 1);

        Goods g = goods("gx", "normal", 10, 1, true);
        when(goodsService.getGoodsById("gx")).thenReturn(g);

        when(vipSwitchService.isSwitchEnabled("Exclusive_goods")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createRedemptionOrder(o));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("VIP_DISABLED"));
    }

    // ---------- 回滚：reserve 成功后 pointsService 抛异常 => releaseStock 被调用 ----------
    @Test
    void createRedemptionOrder_pointsServiceThrows_shouldRollbackStock() {
        String userId = "u1";
        Order o = redemptionOrder(userId, "g1", 1);

        Goods g1 = goods("g1", "normal", 100, 0, true);
        when(goodsService.getGoodsById("g1")).thenReturn(g1);

        doThrow(new RuntimeException("points fail"))
                .when(pointsService).adjustPoints(eq(userId), anyLong(), anyString(), anyString(), any(), any());

        assertThrows(RuntimeException.class, () -> service.createRedemptionOrder(o));

        verify(goodsService).reserveStock("g1", 1);
        verify(goodsService).releaseStock("g1", 1);

        // 扣分失败时 pointsDeducted 还没 true，不会走退款那次 adjustPoints(+)
        verify(pointsService, times(1))
                .adjustPoints(eq(userId), anyLong(), anyString(), anyString(), any(), any());
    }
}
