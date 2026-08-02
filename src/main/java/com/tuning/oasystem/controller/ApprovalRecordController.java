package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.service.ApprovalRecordService;
import com.tuning.oasystem.vo.ApprovalRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审批记录接口
 */
@Tag(name = "审批记录", description = "按业务单查询审批流水")
@RestController
@RequestMapping("/api/approvals/records")
public class ApprovalRecordController {

    private final ApprovalRecordService recordService;

    public ApprovalRecordController(ApprovalRecordService recordService) {
        this.recordService = recordService;
    }

    @Operation(summary = "审批记录查询", description = "按业务类型（1请假 2报销）+ 业务单号查询审批流水")
    @GetMapping
    public Result<List<ApprovalRecordVO>> list(
            @Parameter(description = "业务类型：1请假 2报销") @RequestParam Integer businessType,
            @Parameter(description = "业务单号") @RequestParam Long businessId) {
        return Result.success(recordService.listByBusiness(BusinessType.of(businessType), businessId));
    }
}
