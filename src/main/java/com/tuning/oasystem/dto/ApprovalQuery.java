package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批单分页查询参数（我的申请 / 待我审批共用）
 */
@Data
@Schema(description = "审批单分页查询参数")
public class ApprovalQuery {

    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNum;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize;

    @Schema(description = "状态筛选（可选）：0草稿 1待审批 2通过 3拒绝 4撤回")
    private Integer status;
}
