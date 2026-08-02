package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户分页查询参数
 */
@Data
@Schema(description = "用户分页查询参数")
public class UserQuery {

    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNum = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize = 10L;

    @Schema(description = "用户名（模糊匹配）")
    private String username;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;
}
