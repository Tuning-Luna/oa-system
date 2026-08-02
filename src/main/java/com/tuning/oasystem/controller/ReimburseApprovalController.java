package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.dto.ApprovalCommentRequest;
import com.tuning.oasystem.dto.ApprovalQuery;
import com.tuning.oasystem.dto.ReimburseSubmitRequest;
import com.tuning.oasystem.security.LoginUser;
import com.tuning.oasystem.service.ReimbursementService;
import com.tuning.oasystem.vo.ReimburseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报销审批接口
 */
@Tag(name = "报销审批", description = "报销申请提交 / 审批 / 撤回 / 我的申请 / 待我审批")
@RestController
@RequestMapping("/api/approvals/reimburse")
public class ReimburseApprovalController {

    private final ReimbursementService reimbursementService;

    public ReimburseApprovalController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @Operation(summary = "提交报销申请", description = "提交后进入待审批状态，需指定审批人")
    @PostMapping
    public Result<ReimburseVO> submit(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody ReimburseSubmitRequest request) {
        return Result.success(reimbursementService.submit(loginUser.getUserId(), request));
    }

    @Operation(summary = "我的报销申请", description = "当前登录用户提交的申请，可按状态筛选")
    @GetMapping("/my")
    public Result<PageResult<ReimburseVO>> my(@AuthenticationPrincipal LoginUser loginUser, ApprovalQuery query) {
        return Result.success(reimbursementService.myPage(loginUser.getUserId(), query));
    }

    @Operation(summary = "待我审批列表", description = "当前登录用户作为审批人的待审批申请")
    @GetMapping("/pending")
    public Result<PageResult<ReimburseVO>> pending(@AuthenticationPrincipal LoginUser loginUser, ApprovalQuery query) {
        return Result.success(reimbursementService.pendingPage(loginUser.getUserId(), query));
    }

    @Operation(summary = "报销申请详情", description = "申请人或审批人可查看")
    @GetMapping("/{id}")
    public Result<ReimburseVO> detail(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        return Result.success(reimbursementService.detail(loginUser.getUserId(), id));
    }

    @Operation(summary = "审批通过")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id,
                                @RequestBody(required = false) ApprovalCommentRequest request) {
        reimbursementService.approve(loginUser.getUserId(), id, request != null ? request.getComment() : null);
        return Result.success();
    }

    @Operation(summary = "审批拒绝")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id,
                               @RequestBody(required = false) ApprovalCommentRequest request) {
        reimbursementService.reject(loginUser.getUserId(), id, request != null ? request.getComment() : null);
        return Result.success();
    }

    @Operation(summary = "撤回申请", description = "仅申请人本人可在草稿/待审批状态撤回")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        reimbursementService.cancel(loginUser.getUserId(), id);
        return Result.success();
    }
}
