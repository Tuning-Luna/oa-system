package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 报销申请提交请求
 */
@Data
@Schema(description = "报销申请提交请求")
public class ReimburseSubmitRequest {

    @Schema(description = "报销金额", example = "500.00")
    @NotNull(message = "报销金额不能为空")
    @DecimalMin(value = "0.01", message = "报销金额必须大于 0")
    private BigDecimal amount;

    @Schema(description = "报销类别", example = "交通费")
    @NotBlank(message = "报销类别不能为空")
    @Size(max = 50, message = "报销类别最长 50 字符")
    private String category;

    @Schema(description = "报销事由")
    @NotBlank(message = "报销事由不能为空")
    @Size(max = 500, message = "报销事由最长 500 字符")
    private String reason;

    @Schema(description = "审批人ID", example = "1")
    @NotNull(message = "审批人不能为空")
    private Long approverId;
}
