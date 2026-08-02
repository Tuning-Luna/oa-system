package com.tuning.oasystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.consumer.ApprovalNoticeConsumer;
import com.tuning.oasystem.dto.ApprovalNoticeMessage;
import com.tuning.oasystem.dto.LeaveSubmitRequest;
import com.tuning.oasystem.entity.ApprovalRecord;
import com.tuning.oasystem.entity.SysMessage;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.enums.ApprovalStatus;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.mapper.ApprovalRecordMapper;
import com.tuning.oasystem.mapper.LeaveRequestMapper;
import com.tuning.oasystem.mapper.SysMessageMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.vo.LeaveVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审批通知集成测试（需 MySQL + RabbitMQ 运行）：
 * 消费者消费消息落库；审批通过后异步产生申请人通知（真实 MQ 全链路）。
 */
@SpringBootTest
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class ApprovalNoticeIntegrationTest {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private LeaveRequestMapper leaveMapper;

    @Autowired
    private ApprovalRecordMapper recordMapper;

    @Autowired
    private SysMessageMapper messageMapper;

    @Autowired
    private ApprovalNoticeConsumer consumer;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdLeaveIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long userId : createdUserIds) {
            messageMapper.delete(new LambdaQueryWrapper<SysMessage>().eq(SysMessage::getReceiverId, userId));
        }
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
    void consumerShouldPersistNotification() {
        Long applicant = createUser("mq_cons");

        ApprovalNoticeMessage msg = new ApprovalNoticeMessage();
        msg.setBusinessType(BusinessType.LEAVE.getValue());
        msg.setBusinessId(100L);
        msg.setApplicantId(applicant);
        msg.setApproverId(applicant);
        msg.setResult(ApprovalStatus.APPROVED.getValue());
        msg.setCreateTime(LocalDateTime.now());

        consumer.onMessage(msg);

        SysMessage saved = messageMapper.selectOne(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, applicant)
                .last("LIMIT 1"));
        assertNotNull(saved, "消费者应写入通知");
        assertTrue(saved.getTitle().contains("请假"));
        assertTrue(saved.getContent().contains("通过"));
        assertEquals(0, saved.getReadFlag());
        assertEquals("APPROVAL", saved.getType());
    }

    @Test
    void approvalShouldProduceAsyncNotification() throws InterruptedException {
        Long applicant = createUser("mq_app");
        Long approver = createUser("mq_apr");

        LeaveVO vo = leaveService.submit(applicant, leaveRequest(approver));
        createdLeaveIds.add(vo.getId());

        // 审批通过 → 异步通知申请人（真实 MQ → 消费者 → 落库）
        leaveService.approve(approver, vo.getId(), "同意");

        SysMessage notice = pollLatest(applicant);
        assertNotNull(notice, "审批通过后应异步产生申请人通知");
        assertTrue(notice.getContent().contains("通过"), "通知内容应包含审批结果");
        assertTrue(notice.getTitle().contains("请假"));
    }

    private SysMessage pollLatest(Long receiverId) throws InterruptedException {
        SysMessage message = null;
        for (int i = 0; i < 60 && message == null; i++) {
            message = messageMapper.selectOne(new LambdaQueryWrapper<SysMessage>()
                    .eq(SysMessage::getReceiverId, receiverId)
                    .orderByDesc(SysMessage::getId)
                    .last("LIMIT 1"));
            if (message == null) {
                Thread.sleep(100);
            }
        }
        return message;
    }
}
