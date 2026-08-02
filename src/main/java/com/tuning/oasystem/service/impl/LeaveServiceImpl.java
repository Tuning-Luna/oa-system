package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.ApprovalQuery;
import com.tuning.oasystem.dto.LeaveSubmitRequest;
import com.tuning.oasystem.entity.LeaveRequest;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.enums.ApprovalAction;
import com.tuning.oasystem.enums.ApprovalStateMachine;
import com.tuning.oasystem.enums.ApprovalStatus;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.LeaveRequestMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.producer.ApprovalNoticeProducer;
import com.tuning.oasystem.service.ApprovalRecordService;
import com.tuning.oasystem.service.LeaveService;
import com.tuning.oasystem.utils.UserNameResolver;
import com.tuning.oasystem.vo.LeaveVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 请假审批服务实现
 * <p>
 * 状态机守卫：所有流转先经 {@link ApprovalStateMachine} 校验合法性，非法直接抛业务异常。
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class LeaveServiceImpl implements LeaveService {

    private static final long DEFAULT_PAGE_SIZE = 10;
    private static final long MAX_PAGE_SIZE = 100;

    private final LeaveRequestMapper leaveMapper;
    private final SysUserMapper sysUserMapper;
    private final ApprovalRecordService recordService;
    private final UserNameResolver userNameResolver;
    private final ApprovalNoticeProducer noticeProducer;

    public LeaveServiceImpl(LeaveRequestMapper leaveMapper,
            SysUserMapper sysUserMapper,
            ApprovalRecordService recordService,
            UserNameResolver userNameResolver,
            ApprovalNoticeProducer noticeProducer) {
        this.leaveMapper = leaveMapper;
        this.sysUserMapper = sysUserMapper;
        this.recordService = recordService;
        this.userNameResolver = userNameResolver;
        this.noticeProducer = noticeProducer;
    }

    @Override
    @Transactional
    public LeaveVO submit(Long applicantId, LeaveSubmitRequest request) {
        requireEnabledApprover(request.getApproverId());
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }

        LeaveRequest entity = new LeaveRequest();
        entity.setUserId(applicantId);
        entity.setLeaveType(request.getLeaveType());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setDays(request.getDays());
        entity.setReason(request.getReason());
        entity.setApproverId(request.getApproverId());
        entity.setStatus(ApprovalStatus.DRAFT.getValue());
        leaveMapper.insert(entity);

        // DRAFT → PENDING（经状态机校验）
        ApprovalStatus next = ApprovalStateMachine.next(ApprovalAction.SUBMIT, ApprovalStatus.DRAFT);
        entity.setStatus(next.getValue());
        leaveMapper.updateById(entity);

        recordService.record(BusinessType.LEAVE, entity.getId(), applicantId, ApprovalAction.SUBMIT, request.getReason());
        return toVO(entity, userNameResolver.nameOf(applicantId), userNameResolver.nameOf(request.getApproverId()));
    }

    @Override
    @Transactional
    public void approve(Long operatorId, Long id, String comment) {
        LeaveRequest entity = requireEntity(id);
        assertApprover(operatorId, entity);
        ApprovalStatus next = ApprovalStateMachine.next(ApprovalAction.APPROVE, ApprovalStatus.of(entity.getStatus()));
        entity.setStatus(next.getValue());
        leaveMapper.updateById(entity);
        recordService.record(BusinessType.LEAVE, id, operatorId, ApprovalAction.APPROVE, comment);
        // 审批通过 → 异步通知申请人
        noticeProducer.sendApprovalNotice(BusinessType.LEAVE, id, entity.getUserId(), operatorId, ApprovalStatus.APPROVED.getValue());
    }

    @Override
    @Transactional
    public void reject(Long operatorId, Long id, String comment) {
        LeaveRequest entity = requireEntity(id);
        assertApprover(operatorId, entity);
        ApprovalStatus next = ApprovalStateMachine.next(ApprovalAction.REJECT, ApprovalStatus.of(entity.getStatus()));
        entity.setStatus(next.getValue());
        leaveMapper.updateById(entity);
        recordService.record(BusinessType.LEAVE, id, operatorId, ApprovalAction.REJECT, comment);
        // 审批拒绝 → 异步通知申请人
        noticeProducer.sendApprovalNotice(BusinessType.LEAVE, id, entity.getUserId(), operatorId, ApprovalStatus.REJECTED.getValue());
    }

    @Override
    @Transactional
    public void cancel(Long operatorId, Long id) {
        LeaveRequest entity = requireEntity(id);
        if (!entity.getUserId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有申请人本人可以撤回");
        }
        ApprovalStatus next = ApprovalStateMachine.next(ApprovalAction.CANCEL, ApprovalStatus.of(entity.getStatus()));
        entity.setStatus(next.getValue());
        leaveMapper.updateById(entity);
        recordService.record(BusinessType.LEAVE, id, operatorId, ApprovalAction.CANCEL, null);
    }

    @Override
    public LeaveVO detail(Long operatorId, Long id) {
        LeaveRequest entity = requireEntity(id);
        if (!entity.getUserId().equals(operatorId) && !entity.getApproverId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该申请单");
        }
        return toVO(entity, userNameResolver.nameOf(entity.getUserId()), userNameResolver.nameOf(entity.getApproverId()));
    }

    @Override
    public PageResult<LeaveVO> myPage(Long userId, ApprovalQuery query) {
        long pageNum = pageNum(query);
        long pageSize = pageSize(query);
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getUserId, userId)
                .eq(query.getStatus() != null, LeaveRequest::getStatus, query.getStatus())
                .orderByDesc(LeaveRequest::getId);
        return toPage(leaveMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @Override
    public PageResult<LeaveVO> pendingPage(Long userId, ApprovalQuery query) {
        long pageNum = pageNum(query);
        long pageSize = pageSize(query);
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getApproverId, userId)
                .eq(LeaveRequest::getStatus, ApprovalStatus.PENDING.getValue())
                .orderByDesc(LeaveRequest::getId);
        return toPage(leaveMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    private PageResult<LeaveVO> toPage(Page<LeaveRequest> page) {
        List<LeaveRequest> records = page.getRecords();
        Map<Long, String> applicantNames = userNameResolver.namesOf(records.stream().map(LeaveRequest::getUserId).toList());
        Map<Long, String> approverNames = userNameResolver.namesOf(records.stream().map(LeaveRequest::getApproverId).toList());
        List<LeaveVO> vos = records.stream()
                .map(r -> toVO(r, applicantNames.get(r.getUserId()), approverNames.get(r.getApproverId())))
                .toList();
        return PageResult.of(page.getTotal(), vos, page.getCurrent(), page.getSize());
    }

    private LeaveVO toVO(LeaveRequest entity, String applicantName, String approverName) {
        return LeaveVO.from(entity, applicantName, approverName);
    }

    private LeaveRequest requireEntity(Long id) {
        LeaveRequest entity = leaveMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "请假申请不存在: id=" + id);
        }
        return entity;
    }

    private void assertApprover(Long operatorId, LeaveRequest entity) {
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
