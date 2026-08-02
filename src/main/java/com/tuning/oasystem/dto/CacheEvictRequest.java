package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 清除缓存请求
 */
@Data
@Schema(description = "清除缓存请求")
public class CacheEvictRequest {

    @Schema(description = "缓存 key；含 * 时按模式清除（如 oa:user:info:*）", example = "oa:menu:tree")
    @NotBlank(message = "缓存 key 不能为空")
    private String key;
}
