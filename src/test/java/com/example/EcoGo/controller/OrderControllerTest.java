package com.example.EcoGo.controller;

import com.example.EcoGo.dto.OrderRequestDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.Order;
import com.example.EcoGo.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    private OrderService orderService;
    private OrderController controller;

    @BeforeEach
    void setUp() throws Exception {
        orderService = mock(OrderService.class);
        controller = new OrderController();

        // 注入 @Autowired private OrderService orderService;
        Field f = OrderController.class.getDeclaredField("orderService");
        f.setAccessible(true);
        f.set(controller, orderService);
    }

    private static Order order(String id, String userId, String status, Boolean isRedemption, LocalDateTime createdAt) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setStatus(status);
        o.setIsRedemptionOrder(isRedemption);
        if (createdAt != null) {
            o.setCreatedAt(Date.from(createdAt.atZone(ZoneId.systemDefault()).toInstant()));
        } else {
            o.setCreatedAt(null);
        }
        o.setOrderNumber("NO-" + id);
        o.setTrackingNumber("TN-" + id);
        o.setCarrier("DHL");
        o.setItems(new ArrayList<>());
        // 如果你们 Order.finalAmount 是 BigDecimal 就用这个；如果是 Double/Integer，不影响测试核心
        try {
            o.setFinalAmount(12.34);
        } catch (Throwable ignore) {
            // 某些项目 finalAmount 不是 BigDecimal，忽略即可
        }
        return o;
    }

    // ---------- getAllOrders ----------
    @Test
    void getAllOrders_pageInvalid_throwParamError() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getAllOrders(null, null, null, 0, 20)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void getAllOrders_sizeInvalid_throwParamError() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getAllOrders(null, null, null, 1, 0)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void getAllOrders_whenUserIdProvided_shouldCallGetOrdersByUserId_andPaginateAndSort() {
        String userId = "u1";
        // createdAt：让排序可验证（倒序）
        Order o1 = order("1", userId, "CREATED", false, LocalDateTime.now().minusDays(1));
        Order o2 = order("2", userId, "CREATED", false, LocalDateTime.now()); // newer
        when(orderService.getOrdersByUserId(userId)).thenReturn(new ArrayList<>(List.of(o1, o2)));

        ResponseMessage<Map<String, Object>> resp = controller.getAllOrders(userId, "IGNORED", null, 1, 1);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        Map<String, Object> data = resp.getData();
        assertNotNull(data);

        @SuppressWarnings("unchecked")
        List<Order> pageOrders = (List<Order>) data.get("orders");
        assertEquals(1, pageOrders.size());
        // page=1,size=1，且倒序 => o2 在第一页
        assertEquals("2", pageOrders.get(0).getId());

        verify(orderService).getOrdersByUserId(userId);
        verify(orderService, never()).getOrdersByStatus(anyString());
        verify(orderService, never()).getAllOrders();
    }

    @Test
    void getAllOrders_whenStatusProvided_shouldCallGetOrdersByStatus() {
        when(orderService.getOrdersByStatus("PAID")).thenReturn(new ArrayList<>());

        ResponseMessage<Map<String, Object>> resp = controller.getAllOrders(null, "PAID", null, 1, 20);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(orderService).getOrdersByStatus("PAID");
        verify(orderService, never()).getAllOrders();
        verify(orderService, never()).getOrdersByUserId(anyString());
    }

    @Test
    void getAllOrders_whenNoFilters_shouldCallGetAllOrders_andIsRedemptionFilterApplied() {
        Order r1 = order("1", "u1", "CREATED", true, LocalDateTime.now());
        Order r2 = order("2", "u2", "CREATED", false, LocalDateTime.now());
        when(orderService.getAllOrders()).thenReturn(new ArrayList<>(List.of(r1, r2)));

        ResponseMessage<Map<String, Object>> resp = controller.getAllOrders(null, null, true, 1, 20);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());

        @SuppressWarnings("unchecked")
        List<Order> filtered = (List<Order>) resp.getData().get("orders");
        assertEquals(1, filtered.size());
        assertEquals("1", filtered.get(0).getId());

        verify(orderService).getAllOrders();
    }

    @Test
    void getAllOrders_whenServiceReturnsNull_shouldReturnEmptyList() {
        when(orderService.getAllOrders()).thenReturn(null);

        ResponseMessage<Map<String, Object>> resp = controller.getAllOrders(null, null, null, 1, 20);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        @SuppressWarnings("unchecked")
        List<Order> orders = (List<Order>) resp.getData().get("orders");
        assertNotNull(orders);
        assertEquals(0, orders.size());
    }

    @Test
    void getAllOrders_sortShouldHandleNullCreatedAt_withoutNpe() {
        Order a = order("1", "u1", "CREATED", false, null);
        Order b = order("2", "u1", "CREATED", false, LocalDateTime.now());
        when(orderService.getOrdersByUserId("u1")).thenReturn(new ArrayList<>(List.of(a, b)));

        ResponseMessage<Map<String, Object>> resp = controller.getAllOrders("u1", null, null, 1, 20);
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());

        @SuppressWarnings("unchecked")
        List<Order> orders = (List<Order>) resp.getData().get("orders");
        // b(createdAt!=null) 应排在前面
        assertEquals("2", orders.get(0).getId());
    }

    // ---------- getOrderById ----------
    @Test
    void getOrderById_idBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getOrderById("  ")
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void getOrderById_notFound_throwOrderNotFound() {
        when(orderService.getOrderById("x")).thenReturn(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getOrderById("x")
        );
        assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getOrderById_success() {
        Order o = order("x", "u1", "CREATED", false, LocalDateTime.now());
        when(orderService.getOrderById("x")).thenReturn(o);

        ResponseMessage<Order> resp = controller.getOrderById("x");
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("x", resp.getData().getId());
    }

    // ---------- createRedemptionOrder ----------
    @Test
    void createRedemptionOrder_nullBody_throwParamError() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.createRedemptionOrder(null)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createRedemptionOrder_shouldForceIsRedemption_true_andReturnSuccess() {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId("u1");
        // service 返回
        Order created = order("o1", "u1", "CREATED", true, LocalDateTime.now());
        when(orderService.createRedemptionOrder(any(Order.class))).thenReturn(created);

        ResponseMessage<Order> resp = controller.createRedemptionOrder(dto);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        verify(orderService).createRedemptionOrder(argThat(o -> Boolean.TRUE.equals(o.getIsRedemptionOrder())));
    }

    @Test
    void createRedemptionOrder_serviceReturnsNull_throwDbError() {
        OrderRequestDto dto = new OrderRequestDto();
        when(orderService.createRedemptionOrder(any(Order.class))).thenReturn(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.createRedemptionOrder(dto)
        );
        assertEquals(ErrorCode.DB_ERROR.getCode(), ex.getCode());
    }

    // ---------- updateOrder ----------
    @Test
    void updateOrder_idBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateOrder(" ", new OrderRequestDto())
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void updateOrder_nullBody_throwParamError() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateOrder("x", null)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void updateOrder_serviceReturnsNull_throwOrderNotFound() {
        when(orderService.updateOrder(eq("x"), any(Order.class))).thenReturn(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateOrder("x", new OrderRequestDto())
        );
        assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void updateOrder_success() {
        Order updated = order("x", "u1", "PAID", false, LocalDateTime.now());
        when(orderService.updateOrder(eq("x"), any(Order.class))).thenReturn(updated);

        OrderRequestDto dto = new OrderRequestDto();
        dto.setStatus("PAID");
        ResponseMessage<Order> resp = controller.updateOrder("x", dto);
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("x", resp.getData().getId());
    }

    // ---------- deleteOrder ----------
    @Test
    void deleteOrder_idBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.deleteOrder(" ")
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void deleteOrder_success() {
        doNothing().when(orderService).deleteOrder("x");
        ResponseMessage<Void> resp = controller.deleteOrder("x");
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(orderService).deleteOrder("x");
    }

    // ---------- getUserOrderHistoryForMobile ----------
    @Test
    void getUserOrderHistoryForMobile_userIdBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getUserOrderHistoryForMobile("  ", null, 1, 10)
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void getUserOrderHistoryForMobile_pageInvalid_throwParamError() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getUserOrderHistoryForMobile("u1", null, 0, 10)
        );
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void getUserOrderHistoryForMobile_shouldFilterByStatus_andReturnSimplifiedOrders_withPagination() {
        String userId = "u1";
        Order a = order("1", userId, "CREATED", true, LocalDateTime.now().minusDays(1));
        Order b = order("2", userId, "PAID", true, LocalDateTime.now());
        Order.OrderItem item1 = new Order.OrderItem();
        a.getItems().add(item1);


        when(orderService.getOrdersByUserId(userId)).thenReturn(List.of(a, b));

        // 只要 status=PAID，size=10
        ResponseMessage<Map<String, Object>> resp =
                controller.getUserOrderHistoryForMobile(userId, "PAID", 1, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        Map<String, Object> data = resp.getData();
        assertNotNull(data);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) data.get("orders");
        assertEquals(1, orders.size());
        Map<String, Object> item = orders.get(0);

        assertEquals("2", item.get("id"));
        assertEquals("PAID", item.get("status"));
        assertEquals(true, item.get("isRedemption"));

        Object paginationObj = data.get("pagination");
        assertTrue(paginationObj instanceof Map<?, ?>);
        Map<?, ?> pagination = (Map<?, ?>) paginationObj;
        assertEquals(1, pagination.get("page"));
        assertEquals(10, pagination.get("size"));
    }

    @Test
    void getUserOrderHistoryForMobile_whenServiceReturnsNull_shouldReturnEmpty() {
        when(orderService.getOrdersByUserId("u1")).thenReturn(null);

        ResponseMessage<Map<String, Object>> resp =
                controller.getUserOrderHistoryForMobile("u1", null, 1, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) resp.getData().get("orders");
        assertNotNull(orders);
        assertEquals(0, orders.size());
    }

    // ---------- updateOrderStatus ----------
    @Test
    void updateOrderStatus_idBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateOrderStatus(" ", "PAID")
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void updateOrderStatus_statusBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateOrderStatus("x", "  ")
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void updateOrderStatus_serviceReturnsNull_throwOrderNotFound() {
        when(orderService.updateOrder(eq("x"), any(Order.class))).thenReturn(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.updateOrderStatus("x", "PAID")
        );
        assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void updateOrderStatus_success_shouldCallUpdateOrderWithPatchStatus() {
        Order updated = order("x", "u1", "PAID", false, LocalDateTime.now());
        when(orderService.updateOrder(eq("x"), any(Order.class))).thenReturn(updated);

        ResponseMessage<Order> resp = controller.updateOrderStatus("x", "PAID");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("x", resp.getData().getId());

        // 验证 patch 的 status 传入
        verify(orderService).updateOrder(eq("x"), argThat(o -> "PAID".equals(o.getStatus())));
    }
}
