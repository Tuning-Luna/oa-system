package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 给用户分配角色请求（全量替换该用户的角色）
 */
@Data
@Schema(description = "给用户分配角色请求")
public class AssignRoleRequest {

    @Schema(description = "角色 ID 列表（全量替换）", example = "[1]")
    @NotNull(message = "角色 ID 列表不能为空")
    private List<Long> roleIds;
}
