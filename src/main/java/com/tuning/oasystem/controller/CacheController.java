package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.dto.CacheEvictRequest;
import com.tuning.oasystem.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存管理接口（供运维 / 测试手动清缓存）
 */
@Tag(name = "缓存管理", description = "Redis 缓存清除（运维/测试）")
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final RedisService redisService;

    public CacheController(RedisService redisService) {
        this.redisService = redisService;
    }

    @Operation(summary = "清除缓存", description = "按 key 清除；key 含 * 时按模式批量清除（如 oa:user:info:*）")
    @PostMapping("/evict")
    public Result<Void> evict(@Valid @RequestBody CacheEvictRequest request) {
        if (request.getKey().contains("*")) {
            redisService.deleteByPattern(request.getKey());
        } else {
            redisService.delete(request.getKey());
        }
        return Result.success();
    }
}
