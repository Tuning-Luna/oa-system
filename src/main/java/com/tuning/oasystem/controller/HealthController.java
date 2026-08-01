package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查
 */
@Tag(name = "健康检查", description = "系统健康检查相关接口")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "健康检查", description = "用于验证服务是否正常运行，可作为存活探针。")
    @ApiResponse(responseCode = "200", description = "服务正常")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("ok");
    }
}
