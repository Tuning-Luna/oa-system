package com.tuning.oasystem.exception;

import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * <p>
 * 统一将各类异常转换为标准返回结构 {@code {code, message, data}}，并映射对应的 HTTP 状态码
 * （认证失败 401、无权限 403、参数错误 400、资源不存在 404）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：HTTP 状态码与业务码对齐（400/401/403/404），其余业务码返回 200 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.status(toHttpStatus(e.getCode())).body(Result.error(e.getCode(), e.getMessage()));
    }

    /** 参数校验异常（@RequestBody + @Valid 触发） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("请求参数错误");
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.badRequest().body(Result.error(ResultCode.BAD_REQUEST, message));
    }

    /** 请求体缺失或 JSON 解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体不可读: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Result.error(ResultCode.BAD_REQUEST, "请求体缺失或格式错误"));
    }

    /** 方法级鉴权失败（阶段 3 @PreAuthorize 启用后生效） */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("无访问权限: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(ResultCode.FORBIDDEN));
    }

    /** 兜底异常：记录日志，避免暴露内部细节 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(ResultCode.INTERNAL_ERROR));
    }

    private HttpStatus toHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.OK;
        }
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.OK;
        };
    }
}
