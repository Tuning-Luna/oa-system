package com.tuning.oasystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.ApprovalQuery;
import com.tuning.oasystem.dto.ReimburseSubmitRequest;
import com.tuning.oasystem.entity.ApprovalRecord;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.enums.ApprovalAction;
import com.tuning.oasystem.enums.ApprovalStatus;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.ApprovalRecordMapper;
import com.tuning.oasystem.mapper.ReimbursementRequestMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.vo.ReimburseVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 报销审批服务测试（需 MySQL 运行）：提交→通过/拒绝/撤回、非审批人禁止、金额/审批人校验。
 */
@SpringBootTest
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class ReimburseServiceTest {

    @Autowired
    private ReimbursementService reimburseService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ReimbursementRequestMapper reimburseMapper;

    @Autowired
    private ApprovalRecordMapper recordMapper;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdReimburseIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdReimburseIds) {
            recordMapper.delete(new LambdaQueryWrapper<ApprovalRecord>()
                    .eq(ApprovalRecord::getBusinessType, BusinessType.REIMBURSEMENT.getValue())
                    .eq(ApprovalRecord::getBusinessId, id));
            reimburseMapper.deleteById(id);
        }
        createdReimburseIds.clear();
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

    private ReimburseSubmitRequest reimburseRequest(Long approverId) {
        ReimburseSubmitRequest r = new ReimburseSubmitRequest();
        r.setAmount(new BigDecimal("500.00"));
        r.setCategory("交通费");
        r.setReason("出差打车");
        r.setApproverId(approverId);
        return r;
    }

    @Test
    void submitShouldCreatePendingWithSubmitRecord() {
        Long applicant = createUser("rb_app");
        Long approver = createUser("rb_apr");

        ReimburseVO vo = reimburseService.submit(applicant, reimburseRequest(approver));
        createdReimburseIds.add(vo.getId());

        assertEquals(ApprovalStatus.PENDING.getValue(), vo.getStatus());
        assertEquals(new BigDecimal("500.00"), vo.getAmount());
        assertEquals(approver, vo.getApproverId());

        Long recordCount = recordMapper.selectCount(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getBusinessType, BusinessType.REIMBURSEMENT.getValue())
                .eq(ApprovalRecord::getBusinessId, vo.getId())
                .eq(ApprovalRecord::getAction, ApprovalAction.SUBMIT.getValue()));
        assertEquals(1, recordCount);
    }

    @Test
    void approveRejectAndCancelFlow() {
        Long applicant = createUser("rb_app");
        Long approver = createUser("rb_apr");
        ReimburseVO vo = reimburseService.submit(applicant, reimburseRequest(approver));
        createdReimburseIds.add(vo.getId());

        // 非审批人审批被拒
        Long other = createUser("rb_oth");
        BusinessException forbidden = assertThrows(BusinessException.class,
                () -> reimburseService.approve(other, vo.getId(), null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), forbidden.getCode());

        // 通过
        reimburseService.approve(approver, vo.getId(), "同意");
        assertEquals(ApprovalStatus.APPROVED.getValue(), reimburseMapper.selectById(vo.getId()).getStatus());
        // 已通过不可再通过
        assertThrows(BusinessException.class, () -> reimburseService.approve(approver, vo.getId(), null));

        // 再提交一笔走拒绝
        ReimburseVO vo2 = reimburseService.submit(applicant, reimburseRequest(approver));
        createdReimburseIds.add(vo2.getId());
        reimburseService.reject(approver, vo2.getId(), "金额不符");
        assertEquals(ApprovalStatus.REJECTED.getValue(), reimburseMapper.selectById(vo2.getId()).getStatus());

        // 第三笔走撤回
        ReimburseVO vo3 = reimburseService.submit(applicant, reimburseRequest(approver));
        createdReimburseIds.add(vo3.getId());
        reimburseService.cancel(applicant, vo3.getId());
        assertEquals(ApprovalStatus.CANCELLED.getValue(), reimburseMapper.selectById(vo3.getId()).getStatus());
    }

    @Test
    void submitWithInvalidApproverShouldThrow() {
        Long applicant = createUser("rb_app");
        ReimburseSubmitRequest req = reimburseRequest(999999L);
        assertThrows(BusinessException.class, () -> reimburseService.submit(applicant, req));
    }

    @Test
    void myPageAndPendingPageShouldReturnRecords() {
        Long applicant = createUser("rb_app");
        Long approver = createUser("rb_apr");
        ReimburseVO vo = reimburseService.submit(applicant, reimburseRequest(approver));
        createdReimburseIds.add(vo.getId());

        ApprovalQuery query = new ApprovalQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);

        PageResult<ReimburseVO> my = reimburseService.myPage(applicant, query);
        assertEquals(1, my.getTotal());

        PageResult<ReimburseVO> pending = reimburseService.pendingPage(approver, query);
        assertEquals(1, pending.getTotal());

        PageResult<ReimburseVO> empty = reimburseService.pendingPage(applicant, query);
        assertEquals(0, empty.getTotal());
    }

    @Test
    void detailShouldRestrictToApplicantAndApprover() {
        Long applicant = createUser("rb_app");
        Long approver = createUser("rb_apr");
        Long other = createUser("rb_oth");
        ReimburseVO vo = reimburseService.submit(applicant, reimburseRequest(approver));
        createdReimburseIds.add(vo.getId());

        assertNotNull(reimburseService.detail(applicant, vo.getId()));
        assertNotNull(reimburseService.detail(approver, vo.getId()));
        assertThrows(BusinessException.class, () -> reimburseService.detail(other, vo.getId()));
    }
}
