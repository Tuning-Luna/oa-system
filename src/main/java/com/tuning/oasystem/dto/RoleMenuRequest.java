package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 给角色分配菜单请求（全量替换该角色的菜单）
 */
@Data
@Schema(description = "给角色分配菜单请求")
public class RoleMenuRequest {

    @Schema(description = "菜单 ID 列表（全量替换）")
    @NotNull(message = "菜单 ID 列表不能为空")
    private List<Long> menuIds;
}
