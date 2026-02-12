package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.UserVoucher;
import com.example.EcoGo.model.VoucherStatus;
import com.example.EcoGo.repository.UserVoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserVoucherControllerTest {

    private UserVoucherRepository repo;
    private UserVoucherController controller;

    @BeforeEach
    void setUp() {
        repo = mock(UserVoucherRepository.class);
        controller = new UserVoucherController(repo);

        // ✅ 关键：controller 的“自动过期同步”逻辑在任何 tab 下都会先查 ACTIVE
        // 所以给所有测试一个默认兜底：没 ACTIVE 券 -> 不触发 save
        when(repo.findByUserIdAndStatus(anyString(), eq(VoucherStatus.ACTIVE)))
                .thenReturn(List.of());

        // ✅ 兜底：save 返回入参，避免返回 null 引发链式问题（即使当前实现不使用返回值也更稳）
        when(repo.save(any(UserVoucher.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- getUserVouchers ----------

    @Test
    void getUserVouchers_userIdBlank_throwParamCannotBeNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getUserVouchers("   ", "my")
        );
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), ex.getCode());
    }

    @Test
    void getUserVouchers_tabUsed_callsUsedQuery() {
        String userId = "u1";
        when(repo.findByUserIdAndStatus(userId, VoucherStatus.USED)).thenReturn(List.of());

        ResponseMessage<List<UserVoucher>> resp = controller.getUserVouchers(userId, "used");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        verify(repo).findByUserIdAndStatus(userId, VoucherStatus.USED);
        verify(repo, never()).findByUserIdAndStatus(userId, VoucherStatus.EXPIRED);
        verify(repo, never()).findByUserIdAndStatusAndExpiresAtAfter(eq(userId), eq(VoucherStatus.ACTIVE), any());
    }

    @Test
    void getUserVouchers_tabExpired_callsExpiredQuery() {
        String userId = "u1";
        when(repo.findByUserIdAndStatus(userId, VoucherStatus.EXPIRED)).thenReturn(List.of());

        ResponseMessage<List<UserVoucher>> resp = controller.getUserVouchers(userId, "expired");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        verify(repo).findByUserIdAndStatus(userId, VoucherStatus.EXPIRED);
        verify(repo, never()).findByUserIdAndStatus(userId, VoucherStatus.USED);
        verify(repo, never()).findByUserIdAndStatusAndExpiresAtAfter(eq(userId), eq(VoucherStatus.ACTIVE), any());
    }

    @Test
    void getUserVouchers_tabMy_callsActiveNotExpiredQuery() {
        String userId = "u1";
        UserVoucher uv = new UserVoucher();
        uv.setId("v2");
        uv.setUserId(userId);
        uv.setStatus(VoucherStatus.ACTIVE);

        // my tab 查询（未过期 ACTIVE）
        when(repo.findByUserIdAndStatusAndExpiresAtAfter(eq(userId), eq(VoucherStatus.ACTIVE), any()))
                .thenReturn(List.of(uv));

        ResponseMessage<List<UserVoucher>> resp = controller.getUserVouchers(userId, "my");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals(1, resp.getData().size());
        assertEquals("v2", resp.getData().get(0).getId());

        verify(repo).findByUserIdAndStatusAndExpiresAtAfter(eq(userId), eq(VoucherStatus.ACTIVE), any());
    }

    @Test
    void getUserVouchers_invalidTab_throwParamError() {
        String userId = "u1";

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getUserVouchers(userId, "abc")
        );

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void getUserVouchers_repoThrows_wrapToDbError() {
        String userId = "u1";

        // 让“前置 ACTIVE 查询”直接抛异常 -> 会被 catch(Exception) 包成 DB_ERROR
        when(repo.findByUserIdAndStatus(eq(userId), eq(VoucherStatus.ACTIVE)))
                .thenThrow(new RuntimeException("mongo down"));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> controller.getUserVouchers(userId, "my")
        );

        assertEquals(ErrorCode.DB_ERROR.getCode(), ex.getCode());
    }

    @Test
    void getUserVouchers_myTab_shouldAutoExpireActiveExpiredOnes_andSave() {
        String userId = "u1";
        LocalDateTime now = LocalDateTime.now();

        // ACTIVE 列表里：一张已过期，一张未过期
        UserVoucher expiredActive = new UserVoucher();
        expiredActive.setId("v1");
        expiredActive.setUserId(userId);
        expiredActive.setStatus(VoucherStatus.ACTIVE);
        expiredActive.setExpiresAt(now.minusDays(2));

        UserVoucher validActive = new UserVoucher();
        validActive.setId("v2");
        validActive.setUserId(userId);
        validActive.setStatus(VoucherStatus.ACTIVE);
        validActive.setExpiresAt(now.plusDays(2));

        // 覆盖默认兜底：让前置 ACTIVE 查询返回我们造的数据
        when(repo.findByUserIdAndStatus(eq(userId), eq(VoucherStatus.ACTIVE)))
                .thenReturn(List.of(expiredActive, validActive));

        // my tab 最终查询（未过期 ACTIVE）
        when(repo.findByUserIdAndStatusAndExpiresAtAfter(eq(userId), eq(VoucherStatus.ACTIVE), any()))
                .thenReturn(List.of(validActive));

        ResponseMessage<List<UserVoucher>> resp = controller.getUserVouchers(userId, "my");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals(1, resp.getData().size());
        assertEquals("v2", resp.getData().get(0).getId());

        // 验证：过期但仍 ACTIVE 的券被标记 EXPIRED 并保存
        ArgumentCaptor<UserVoucher> captor = ArgumentCaptor.forClass(UserVoucher.class);
        verify(repo, atLeastOnce()).save(captor.capture());

        boolean hasExpiredSaved = captor.getAllValues().stream()
                .anyMatch(uv -> "v1".equals(uv.getId()) && VoucherStatus.EXPIRED.equals(uv.getStatus()) && uv.getUpdatedAt() != null);

        assertTrue(hasExpiredSaved, "Expected expired ACTIVE voucher to be saved as EXPIRED with updatedAt");
    }

    // ---------- getUserVoucherById ----------

    @Test
    void getUserVoucherById_idBlank_returnsParamCannotBeNullResponse() {
        ResponseMessage<UserVoucher> resp = controller.getUserVoucherById("  ");
        assertEquals(ErrorCode.PARAM_CANNOT_BE_NULL.getCode(), resp.getCode());
        assertNull(resp.getData());
    }

    @Test
    void getUserVoucherById_notFound_returnsParamErrorResponse() {
        when(repo.findById("x")).thenReturn(Optional.empty());

        ResponseMessage<UserVoucher> resp = controller.getUserVoucherById("x");
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), resp.getCode());
        assertNull(resp.getData());
    }

    @Test
    void getUserVoucherById_found_returnsSuccess() {
        UserVoucher uv = new UserVoucher();
        uv.setId("x");

        when(repo.findById("x")).thenReturn(Optional.of(uv));

        ResponseMessage<UserVoucher> resp = controller.getUserVoucherById("x");
        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("x", resp.getData().getId());
    }
}
