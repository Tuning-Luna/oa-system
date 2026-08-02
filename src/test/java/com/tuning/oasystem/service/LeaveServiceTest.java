package com.tuning.oasystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.ApprovalQuery;
import com.tuning.oasystem.dto.LeaveSubmitRequest;
import com.tuning.oasystem.entity.ApprovalRecord;
import com.tuning.oasystem.entity.LeaveRequest;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.enums.ApprovalAction;
import com.tuning.oasystem.enums.ApprovalStatus;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.ApprovalRecordMapper;
import com.tuning.oasystem.mapper.LeaveRequestMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.vo.LeaveVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 请假审批服务测试（需 MySQL 运行）：
 * 提交→通过/拒绝/撤回、非审批人禁止、重复审批拒绝、列表与详情权限。
 */
@SpringBootTest
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class LeaveServiceTest {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private LeaveRequestMapper leaveMapper;

    @Autowired
    private ApprovalRecordMapper recordMapper;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdLeaveIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdLeaveIds) {
            recordMapper.delete(new LambdaQueryWrapper<ApprovalRecord>()
                    .eq(ApprovalRecord::getBusinessType, BusinessType.LEAVE.getValue())
                    .eq(ApprovalRecord::getBusinessId, id));
            leaveMapper.deleteById(id);
        }
        createdLeaveIds.clear();
        for (Long uid : createdUserIds) {
            sysUserMapper.deleteById(uid);
        }
        createdUserIds.clear();
    }

    private Long createUser(String prefix) {
        SysUser u = new SysUser();
        u.setUsername(prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        u.setPassword("$2a$10$d4RHUelT9g8KZoidi6pTAO2m.VGr5Dc0Acc6u.d7ga1pSB3TZLEHO");
        u.setNickname("测试用户");
        u.setStatus(1);
        sysUserMapper.insert(u);
        createdUserIds.add(u.getId());
        return u.getId();
    }

    private LeaveSubmitRequest leaveRequest(Long approverId) {
        LeaveSubmitRequest r = new LeaveSubmitRequest();
        r.setLeaveType(2);
        r.setStartDate(LocalDate.of(2026, 8, 10));
        r.setEndDate(LocalDate.of(2026, 8, 12));
        r.setDays(3);
        r.setReason("家中有事");
        r.setApproverId(approverId);
        return r;
    }

    @Test
    void submitShouldCreatePendingWithSubmitRecord() {
        Long applicant = createUser("lv_app");
        Long approver = createUser("lv_apr");

        LeaveVO vo = leaveService.submit(applicant, leaveRequest(approver));
        createdLeaveIds.add(vo.getId());

        assertEquals(ApprovalStatus.PENDING.getValue(), vo.getStatus());
        assertEquals(approver, vo.getApproverId());

        LeaveRequest db = leaveMapper.selectById(vo.getId());
        assertEquals(ApprovalStatus.PENDING.getValue(), db.getStatus());

        Long recordCount = recordMapper.selectCount(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getBusinessType, BusinessType.LEAVE.getValue())
                .eq(ApprovalRecord::getBusinessId, vo.getId())
                .eq(ApprovalRecord::getAction, ApprovalAction.SUBMIT.getValue()));
        assertEquals(1, recordCount, "提交应落一条『提交』审批记录");
    }

    @Test
    void approveFlowShouldReachApprovedAndRejectReApprove() {
        Long applicant = createUser("lv_app");
        Long approver = createUser("lv_apr");
        Long other = createUser("lv_oth");
        LeaveVO vo = leaveService.submit(applicant, leaveRequest(approver));
        createdLeaveIds.add(vo.getId());

        // 非审批人审批被拒（403）
        BusinessException forbidden = assertThrows(BusinessException.class,
                () -> leaveService.approve(other, vo.getId(), "越权"));
        assertEquals(ResultCode.FORBIDDEN.getCode(), forbidden.getCode());

        // 审批人通过
        leaveService.approve(approver, vo.getId(), "同意");
        assertEquals(ApprovalStatus.APPROVED.getValue(), leaveMapper.selectById(vo.getId()).getStatus());

        // 已通过再通过被拒
        assertThrows(BusinessException.class, () -> leaveService.approve(approver, vo.getId(), "再通过"));
    }

    @Test
    void rejectFlowShouldReachRejected() {
        Long applicant = createUser("lv_app");
        Long approver = createUser("lv_apr");
        LeaveVO vo = leaveService.submit(applicant, leaveRequest(approver));
        createdLeaveIds.add(vo.getId());

        leaveService.reject(approver, vo.getId(), "不通过");
        assertEquals(ApprovalStatus.REJECTED.getValue(), leaveMapper.selectById(vo.getId()).getStatus());
    }

    @Test
    void cancelFlowShouldReachCancelled() {
        Long applicant = createUser("lv_app");
        Long approver = createUser("lv_apr");
        LeaveVO vo = leaveService.submit(applicant, leaveRequest(approver));
        createdLeaveIds.add(vo.getId());

        // 非申请人撤回被拒
        assertThrows(BusinessException.class, () -> leaveService.cancel(approver, vo.getId()));

        // 申请人撤回
        leaveService.cancel(applicant, vo.getId());
        assertEquals(ApprovalStatus.CANCELLED.getValue(), leaveMapper.selectById(vo.getId()).getStatus());

        // 已撤回再撤回被拒
        assertThrows(BusinessException.class, () -> leaveService.cancel(applicant, vo.getId()));
    }

    @Test
    void cancelAfterApprovedShouldThrow() {
        Long applicant = createUser("lv_app");
        Long approver = createUser("lv_apr");
        LeaveVO vo = leaveService.submit(applicant, leaveRequest(approver));
        createdLeaveIds.add(vo.getId());

        leaveService.approve(approver, vo.getId(), null);
        assertThrows(BusinessException.class, () -> leaveService.cancel(applicant, vo.getId()),
                "已通过的单不可撤回");
    }

    @Test
    void submitWithInvalidApproverOrDateShouldThrow() {
        Long applicant = createUser("lv_app");
        Long approver = createUser("lv_apr");

        // 审批人不存在
        LeaveSubmitRequest badApprover = leaveRequest(999999L);
        assertThrows(BusinessException.class, () -> leaveService.submit(applicant, badApprover));

        // 日期区间非法
        LeaveSubmitRequest badDate = leaveRequest(approver);
        badDate.setStartDate(LocalDate.of(2026, 8, 12));
        badDate.setEndDate(LocalDate.of(2026, 8, 10));
        assertThrows(BusinessException.class, () -> leaveService.submit(applicant, badDate));
    }

    @Test
    void myPageAndPendingPageShouldReturnRecords() {
        Long applicant = createUser("lv_app");
        Long approver = createUser("lv_apr");
        LeaveVO vo = leaveService.submit(applicant, leaveRequest(approver));
        createdLeaveIds.add(vo.getId());

        ApprovalQuery query = new ApprovalQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);

        PageResult<LeaveVO> my = leaveService.myPage(applicant, query);
        assertEquals(1, my.getTotal());
        assertEquals(vo.getId(), my.getRecords().get(0).getId());

        PageResult<LeaveVO> pending = leaveService.pendingPage(approver, query);
        assertEquals(1, pending.getTotal(), "审批人待办应包含该申请");

        // 非审批人待办为空
        PageResult<LeaveVO> empty = leaveService.pendingPage(applicant, query);
        assertEquals(0, empty.getTotal());
    }

    @Test
    void detailShouldRestrictToApplicantAndApprover() {
        Long applicant = createUser("lv_app");
        Long approver = createUser("lv_apr");
        Long other = createUser("lv_oth");
        LeaveVO vo = leaveService.submit(applicant, leaveRequest(approver));
        createdLeaveIds.add(vo.getId());

        assertNotNull(leaveService.detail(applicant, vo.getId()));
        assertNotNull(leaveService.detail(approver, vo.getId()));

        BusinessException ex = assertThrows(BusinessException.class, () -> leaveService.detail(other, vo.getId()));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("无权查看"));
    }
}
