package com.example.EcoGo.controller;

import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.UserVoucher;
import com.example.EcoGo.model.VoucherStatus;
import com.example.EcoGo.repository.UserVoucherRepository;
import com.example.EcoGo.dto.ResponseMessage;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
public class UserVoucherController {

    private final UserVoucherRepository userVoucherRepository;

    public UserVoucherController(UserVoucherRepository userVoucherRepository) {
        this.userVoucherRepository = userVoucherRepository;
    }

    /**
     * 获取用户 vouchers
     * tab:
     *  - my      : ACTIVE 且未过期
     *  - used    : USED
     *  - expired : EXPIRED（或 ACTIVE 但已过期会被自动标记为 EXPIRED）
     */
    @GetMapping
    public ResponseMessage<List<UserVoucher>> getUserVouchers(
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "my") String tab
    ) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_CANNOT_BE_NULL, "userId");
        }

        try {
            LocalDateTime now = LocalDateTime.now();

            // ✅ 自动把已过期但仍 ACTIVE 的券标记为 EXPIRED（保持一致性）
            List<UserVoucher> activeAll = userVoucherRepository.findByUserIdAndStatus(userId, VoucherStatus.ACTIVE);
            for (UserVoucher uv : activeAll) {
                if (uv.getExpiresAt() != null && !uv.getExpiresAt().isAfter(now)) {
                    uv.setStatus(VoucherStatus.EXPIRED);
                    uv.setUpdatedAt(now);
                    userVoucherRepository.save(uv);
                }
            }

            tab = tab.trim().toLowerCase();

            List<UserVoucher> result;
            switch (tab) {
                case "used" -> result = userVoucherRepository.findByUserIdAndStatus(userId, VoucherStatus.USED);
                case "expired" -> result = userVoucherRepository.findByUserIdAndStatus(userId, VoucherStatus.EXPIRED);
                case "my" -> result = userVoucherRepository.findByUserIdAndStatusAndExpiresAtAfter(
                        userId, VoucherStatus.ACTIVE, now
                );
                default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "tab must be my/used/expired");
            }

            return new ResponseMessage<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), result);

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_ERROR, e.getMessage());
        }
    }
}
