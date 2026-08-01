package com.tuning.oasystem.exception;

import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.common.ResultCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试用控制器：仅用于触发各类异常，验证 {@link GlobalExceptionHandler} 全局异常处理。
 * <p>
 * 注意：Boot 4 的 @WebMvcTest 不会注册嵌套内部类的控制器，因此使用顶层类。
 */
@RestController
public class TestExceptionController {

    @GetMapping("/test/business-error")
    public Result<Void> businessError() {
        throw new BusinessException(ResultCode.BAD_REQUEST, "业务校验失败");
    }

    @GetMapping("/test/unknown-error")
    public Result<Void> unknownError() {
        throw new IllegalStateException("unexpected error");
    }
}
