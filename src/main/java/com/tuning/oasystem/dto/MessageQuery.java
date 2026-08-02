package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知分页查询参数
 */
@Data
@Schema(description = "通知分页查询参数")
public class MessageQuery {

    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNum;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize;

    @Schema(description = "已读状态筛选（可选）：0未读 1已读")
    private Integer readFlag;
}
