package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审批操作请求（通过 / 拒绝时的审批意见）
 */
@Data
@Schema(description = "审批操作请求")
public class ApprovalCommentRequest {

    @Schema(description = "审批意见（可选）", example = "同意")
    @Size(max = 500, message = "审批意见最长 500 字符")
    private String comment;
}
