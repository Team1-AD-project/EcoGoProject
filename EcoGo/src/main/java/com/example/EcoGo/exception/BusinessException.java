package com.example.EcoGo.exception;

import com.example.EcoGo.exception.errorcode.ErrorCode;

/**
 * 业务异常类：仅用于业务逻辑错误（如“用户不存在”）
 */
public class BusinessException extends RuntimeException {
    // 错误码（从 ErrorCode 枚举获取）
    private final int code;

    // 构造方法1：直接传入 ErrorCode 枚举（无动态参数）
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // 异常消息 = 枚举的 message
        this.code = errorCode.getCode();
    }

    // 构造方法2：传入 ErrorCode 枚举 + 动态参数（填充占位符）
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessage(args)); // 动态填充消息（如 "参数错误：手机号"）
        this.code = errorCode.getCode();
    }

    // getter：供全局异常处理器获取 code
    public int getCode() {
        return code;
    }
}