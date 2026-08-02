package com.tuning.oasystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import com.tuning.oasystem.entity.SysMessage;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.mapper.SysMessageMapper;
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
 * 系统通知接口集成测试（需 MySQL + Redis 运行）：分页、未读计数、标记已读、他人通知 403。
 */
@SpringBootTest
@AutoConfigureMockMvc
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysMessageMapper messageMapper;

    private final List<String> createdUsernames = new ArrayList<>();
    private final List<Long> createdMessageIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdMessageIds) {
            messageMapper.deleteById(id);
        }
        createdMessageIds.clear();
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
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"nickname\":\"测试\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private Long insertMessage(Long receiver, Integer readFlag) {
        SysMessage m = new SysMessage();
        m.setReceiverId(receiver);
        m.setType("APPROVAL");
        m.setTitle("审批通知");
        m.setContent("您的请假申请已通过");
        m.setReadFlag(readFlag);
        messageMapper.insert(m);
        createdMessageIds.add(m.getId());
        return m.getId();
    }

    @Test
    void messageFullFlow() throws Exception {
        String username = uniqueUsername("msg");
        Long userId = registerAndGetId(username, "Passw0rd123");
        String token = loginAndGetToken(username, "Passw0rd123");
        insertMessage(userId, 0);
        Long id2 = insertMessage(userId, 0);

        mockMvc.perform(get("/api/messages").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/messages/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        mockMvc.perform(put("/api/messages/" + id2 + "/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/messages/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(put("/api/messages/read-all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/messages/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void markOtherUserMessageShouldReturn403() throws Exception {
        String usernameA = uniqueUsername("msg_a");
        String usernameB = uniqueUsername("msg_b");
        Long userB = registerAndGetId(usernameB, "Passw0rd123");
        registerAndGetId(usernameA, "Passw0rd123");
        String tokenA = loginAndGetToken(usernameA, "Passw0rd123");

        Long msgForB = insertMessage(userB, 0);

        mockMvc.perform(put("/api/messages/" + msgForB + "/read").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void unauthenticatedShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
