package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 请假申请提交请求
 */
@Data
@Schema(description = "请假申请提交请求")
public class LeaveSubmitRequest {

    @Schema(description = "请假类型：1年假 2事假 3病假", example = "2")
    @NotNull(message = "请假类型不能为空")
    @Min(value = 1, message = "请假类型取值 1/2/3")
    @Max(value = 3, message = "请假类型取值 1/2/3")
    private Integer leaveType;

    @Schema(description = "开始日期", example = "2026-08-10")
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-08-12")
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "请假天数", example = "3")
    @NotNull(message = "请假天数不能为空")
    @Min(value = 1, message = "请假天数至少为 1")
    private Integer days;

    @Schema(description = "请假事由")
    @NotBlank(message = "请假事由不能为空")
    @Size(max = 500, message = "请假事由最长 500 字符")
    private String reason;

    @Schema(description = "审批人ID", example = "1")
    @NotNull(message = "审批人不能为空")
    private Long approverId;
}
