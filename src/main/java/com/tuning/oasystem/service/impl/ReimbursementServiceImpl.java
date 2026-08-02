package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.ApprovalQuery;
import com.tuning.oasystem.dto.ReimburseSubmitRequest;
import com.tuning.oasystem.entity.ReimbursementRequest;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.enums.ApprovalAction;
import com.tuning.oasystem.enums.ApprovalStateMachine;
import com.tuning.oasystem.enums.ApprovalStatus;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.ReimbursementRequestMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.producer.ApprovalNoticeProducer;
import com.tuning.oasystem.service.ApprovalRecordService;
import com.tuning.oasystem.service.ReimbursementService;
import com.tuning.oasystem.utils.UserNameResolver;
import com.tuning.oasystem.vo.ReimburseVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 报销审批服务实现
 * <p>
 * 状态机守卫：所有流转先经 {@link ApprovalStateMachine} 校验合法性，非法直接抛业务异常。
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class ReimbursementServiceImpl implements ReimbursementService {

    private static final long DEFAULT_PAGE_SIZE = 10;
    private static final long MAX_PAGE_SIZE = 100;

    private final ReimbursementRequestMapper reimburseMapper;
    private final SysUserMapper sysUserMapper;
    private final ApprovalRecordService recordService;
    private final UserNameResolver userNameResolver;
    private final ApprovalNoticeProducer noticeProducer;

    public ReimbursementServiceImpl(ReimbursementRequestMapper reimburseMapper,
            SysUserMapper sysUserMapper,
            ApprovalRecordService recordService,
            UserNameResolver userNameResolver,
            ApprovalNoticeProducer noticeProducer) {
        this.reimburseMapper = reimburseMapper;
        this.sysUserMapper = sysUserMapper;
        this.recordService = recordService;
        this.userNameResolver = userNameResolver;
        this.noticeProducer = noticeProducer;
    }

    @Override
    @Transactional
    public ReimburseVO submit(Long applicantId, ReimburseSubmitRequest request) {
        requireEnabledApprover(request.getApproverId());

        ReimbursementRequest entity = new ReimbursementRequest();
        entity.setUserId(applicantId);
        entity.setAmount(request.getAmount());
        entity.setCategory(request.getCategory());
        entity.setReason(request.getReason());
        entity.setApproverId(request.getApproverId());
        entity.setStatus(ApprovalStatus.DRAFT.getValue());
        reimburseMapper.insert(entity);

        // DRAFT → PENDING（经状态机校验）
        ApprovalStatus next = ApprovalStateMachine.next(ApprovalAction.SUBMIT, ApprovalStatus.DRAFT);
        entity.setStatus(next.getValue());
        reimburseMapper.updateById(entity);

        recordService.record(BusinessType.REIMBURSEMENT, entity.getId(), applicantId, ApprovalAction.SUBMIT, request.getReason());
        return toVO(entity, userNameResolver.nameOf(applicantId), userNameResolver.nameOf(request.getApproverId()));
    }

    @Override
    @Transactional
    public void approve(Long operatorId, Long id, String comment) {
        ReimbursementRequest entity = requireEntity(id);
        assertApprover(operatorId, entity);
        ApprovalStatus next = ApprovalStateMachine.next(ApprovalAction.APPROVE, ApprovalStatus.of(entity.getStatus()));
        entity.setStatus(next.getValue());
        reimburseMapper.updateById(entity);
        recordService.record(BusinessType.REIMBURSEMENT, id, operatorId, ApprovalAction.APPROVE, comment);
        // 审批通过 → 异步通知申请人
        noticeProducer.sendApprovalNotice(BusinessType.REIMBURSEMENT, id, entity.getUserId(), operatorId, ApprovalStatus.APPROVED.getValue());
    }

    @Override
    @Transactional
    public void reject(Long operatorId, Long id, String comment) {
        ReimbursementRequest entity = requireEntity(id);
        assertApprover(operatorId, entity);
        ApprovalStatus next = ApprovalStateMachine.next(ApprovalAction.REJECT, ApprovalStatus.of(entity.getStatus()));
        entity.setStatus(next.getValue());
        reimburseMapper.updateById(entity);
        recordService.record(BusinessType.REIMBURSEMENT, id, operatorId, ApprovalAction.REJECT, comment);
        // 审批拒绝 → 异步通知申请人
        noticeProducer.sendApprovalNotice(BusinessType.REIMBURSEMENT, id, entity.getUserId(), operatorId, ApprovalStatus.REJECTED.getValue());
    }

    @Override
    @Transactional
    public void cancel(Long operatorId, Long id) {
        ReimbursementRequest entity = requireEntity(id);
        if (!entity.getUserId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有申请人本人可以撤回");
        }
        ApprovalStatus next = ApprovalStateMachine.next(ApprovalAction.CANCEL, ApprovalStatus.of(entity.getStatus()));
        entity.setStatus(next.getValue());
        reimburseMapper.updateById(entity);
        recordService.record(BusinessType.REIMBURSEMENT, id, operatorId, ApprovalAction.CANCEL, null);
    }

    @Override
    public ReimburseVO detail(Long operatorId, Long id) {
        ReimbursementRequest entity = requireEntity(id);
        if (!entity.getUserId().equals(operatorId) && !entity.getApproverId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该申请单");
        }
        return toVO(entity, userNameResolver.nameOf(entity.getUserId()), userNameResolver.nameOf(entity.getApproverId()));
    }

    @Override
    public PageResult<ReimburseVO> myPage(Long userId, ApprovalQuery query) {
        long pageNum = pageNum(query);
        long pageSize = pageSize(query);
        LambdaQueryWrapper<ReimbursementRequest> wrapper = new LambdaQueryWrapper<ReimbursementRequest>()
                .eq(ReimbursementRequest::getUserId, userId)
                .eq(query.getStatus() != null, ReimbursementRequest::getStatus, query.getStatus())
                .orderByDesc(ReimbursementRequest::getId);
        return toPage(reimburseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @Override
    public PageResult<ReimburseVO> pendingPage(Long userId, ApprovalQuery query) {
        long pageNum = pageNum(query);
        long pageSize = pageSize(query);
        LambdaQueryWrapper<ReimbursementRequest> wrapper = new LambdaQueryWrapper<ReimbursementRequest>()
                .eq(ReimbursementRequest::getApproverId, userId)
                .eq(ReimbursementRequest::getStatus, ApprovalStatus.PENDING.getValue())
                .orderByDesc(ReimbursementRequest::getId);
        return toPage(reimburseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    private PageResult<ReimburseVO> toPage(Page<ReimbursementRequest> page) {
        List<ReimbursementRequest> records = page.getRecords();
        Map<Long, String> applicantNames = userNameResolver.namesOf(records.stream().map(ReimbursementRequest::getUserId).toList());
        Map<Long, String> approverNames = userNameResolver.namesOf(records.stream().map(ReimbursementRequest::getApproverId).toList());
        List<ReimburseVO> vos = records.stream()
                .map(r -> toVO(r, applicantNames.get(r.getUserId()), approverNames.get(r.getApproverId())))
                .toList();
        return PageResult.of(page.getTotal(), vos, page.getCurrent(), page.getSize());
    }

    private ReimburseVO toVO(ReimbursementRequest entity, String applicantName, String approverName) {
        return ReimburseVO.from(entity, applicantName, approverName);
    }

    private ReimbursementRequest requireEntity(Long id) {
        ReimbursementRequest entity = reimburseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报销申请不存在: id=" + id);
        }
        return entity;
    }

    private void assertApprover(Long operatorId, ReimbursementRequest entity) {
        if (!entity.getApproverId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有指定审批人可以审批该申请单");
        }
    }

    private void requireEnabledApprover(Long approverId) {
        SysUser approver = sysUserMapper.selectById(approverId);
        if (approver == null || approver.getStatus() == null || approver.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "审批人不存在或已禁用");
        }
    }

    private long pageNum(ApprovalQuery query) {
        return query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
    }

    private long pageSize(ApprovalQuery query) {
        return query.getPageSize() == null || query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(query.getPageSize(), MAX_PAGE_SIZE);
    }
}
