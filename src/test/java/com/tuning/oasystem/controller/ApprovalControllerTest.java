package com.tuning.oasystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import com.tuning.oasystem.entity.ApprovalRecord;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.mapper.ApprovalRecordMapper;
import com.tuning.oasystem.mapper.LeaveRequestMapper;
import com.tuning.oasystem.mapper.ReimbursementRequestMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 审批接口集成测试（需 MySQL 运行，完整安全过滤器链）：
 * 注册申请人/审批人 → 提交 → 非审批人 403 → 审批通过 → 详情状态变化 + 审批记录。
 */
@SpringBootTest
@AutoConfigureMockMvc
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class ApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private LeaveRequestMapper leaveMapper;

    @Autowired
    private ReimbursementRequestMapper reimburseMapper;

    @Autowired
    private ApprovalRecordMapper recordMapper;

    private final List<String> createdUsernames = new ArrayList<>();
    private final List<Long> createdLeaveIds = new ArrayList<>();
    private final List<Long> createdReimburseIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdLeaveIds) {
            recordMapper.delete(new LambdaQueryWrapper<ApprovalRecord>()
                    .eq(ApprovalRecord::getBusinessType, BusinessType.LEAVE.getValue())
                    .eq(ApprovalRecord::getBusinessId, id));
            leaveMapper.deleteById(id);
        }
        for (Long id : createdReimburseIds) {
            recordMapper.delete(new LambdaQueryWrapper<ApprovalRecord>()
                    .eq(ApprovalRecord::getBusinessType, BusinessType.REIMBURSEMENT.getValue())
                    .eq(ApprovalRecord::getBusinessId, id));
            reimburseMapper.deleteById(id);
        }
        createdLeaveIds.clear();
        createdReimburseIds.clear();
        for (String username : createdUsernames) {
            sysUserMapper.delete(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        }
        createdUsernames.clear();
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private Long registerAndGetId(String username, String password) throws Exception {
        createdUsernames.add(username);
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password
                + "\",\"nickname\":\"测试\"}";
        MvcResult result = mockMvc
                .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void leaveFullFlow() throws Exception {
        String applicantName = uniqueUsername("apv_lv");
        String approverName = uniqueUsername("apv_lv");
        registerAndGetId(applicantName, "Passw0rd123");
        Long approver = registerAndGetId(approverName, "Passw0rd123");
        String applicantToken = loginAndGetToken(applicantName, "Passw0rd123");
        String approverToken = loginAndGetToken(approverName, "Passw0rd123");

        // 提交请假申请 → 待审批
        String submitBody = "{\"leaveType\":2,\"startDate\":\"2026-08-10\",\"endDate\":\"2026-08-12\","
                + "\"days\":3,\"reason\":\"家中有事\",\"approverId\":" + approver + "}";
        MvcResult submitResult = mockMvc.perform(post("/api/approvals/leave")
                .header("Authorization", "Bearer " + applicantToken)
                .contentType(MediaType.APPLICATION_JSON).content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(1))
                .andReturn();
        Long leaveId = ((Number) JsonPath.read(submitResult.getResponse().getContentAsString(), "$.data.id"))
                .longValue();
        createdLeaveIds.add(leaveId);

        // 审批人待办列表含该申请
        mockMvc.perform(get("/api/approvals/leave/pending").header("Authorization", "Bearer " + approverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        // 申请人（非审批人）审批 → 403
        mockMvc.perform(put("/api/approvals/leave/" + leaveId + "/approve")
                .header("Authorization", "Bearer " + applicantToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        // 审批人通过
        mockMvc.perform(put("/api/approvals/leave/" + leaveId + "/approve")
                .header("Authorization", "Bearer " + approverToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 详情状态 → 已通过
        mockMvc.perform(get("/api/approvals/leave/" + leaveId).header("Authorization",
                "Bearer " + applicantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.statusDesc").value("已通过"));

        // 审批记录：提交 + 通过 共 2 条
        mockMvc.perform(get("/api/approvals/records")
                .param("businessType", "1").param("businessId", String.valueOf(leaveId))
                .header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].action").value(2));
    }

    @Test
    void reimburseFullFlow() throws Exception {
        String applicantName = uniqueUsername("apv_rb");
        String approverName = uniqueUsername("apv_rb");
        registerAndGetId(applicantName, "Passw0rd123");
        Long approver = registerAndGetId(approverName, "Passw0rd123");
        String applicantToken = loginAndGetToken(applicantName, "Passw0rd123");
        String approverToken = loginAndGetToken(approverName, "Passw0rd123");

        String submitBody = "{\"amount\":500.00,\"category\":\"交通费\",\"reason\":\"出差打车\",\"approverId\":"
                + approver + "}";
        MvcResult submitResult = mockMvc.perform(post("/api/approvals/reimburse")
                .header("Authorization", "Bearer " + applicantToken)
                .contentType(MediaType.APPLICATION_JSON).content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1))
                .andReturn();
        Long reimburseId = ((Number) JsonPath.read(submitResult.getResponse().getContentAsString(),
                "$.data.id")).longValue();
        createdReimburseIds.add(reimburseId);

        // 审批人拒绝
        mockMvc.perform(put("/api/approvals/reimburse/" + reimburseId + "/reject")
                .header("Authorization", "Bearer " + approverToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"金额不符\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/approvals/reimburse/" + reimburseId).header("Authorization",
                "Bearer " + applicantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(3))
                .andExpect(jsonPath("$.data.statusDesc").value("已拒绝"));

        // 我的报销申请含该单
        mockMvc.perform(get("/api/approvals/reimburse/my").header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void unauthenticatedSubmitShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/approvals/leave").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void submitValidationShouldReturn400() throws Exception {
        String username = uniqueUsername("apv_vd");
        registerAndGetId(username, "Passw0rd123");
        String token = loginAndGetToken(username, "Passw0rd123");

        // 缺必填字段（leaveType/days/approverId 等）
        mockMvc.perform(post("/api/approvals/leave")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"缺字段\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
