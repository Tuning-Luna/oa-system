package com.tuning.oasystem.exception;

import com.tuning.oasystem.common.ResultCode;

/**
 * 业务异常
 * <p>
 * Service/Controller 中通过抛出该异常中断流程，由 {@link GlobalExceptionHandler} 统一转为标准响应。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务状态码 */
    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
