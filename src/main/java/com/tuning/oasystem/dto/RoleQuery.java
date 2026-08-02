package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色分页查询参数
 */
@Data
@Schema(description = "角色分页查询参数")
public class RoleQuery {

    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNum;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize;

    @Schema(description = "角色名称（模糊）")
    private String name;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;
}
