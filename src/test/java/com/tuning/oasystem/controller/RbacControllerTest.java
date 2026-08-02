package com.tuning.oasystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import com.tuning.oasystem.entity.SysRole;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.entity.SysUserRole;
import com.tuning.oasystem.mapper.SysRoleMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.mapper.SysUserRoleMapper;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 集成测试（需 MySQL 运行 + V3 初始化数据）：
 * admin 可访问受保护接口；/me 返回角色与权限；无角色用户 403；分配角色后获得权限。
 */
@SpringBootTest
@AutoConfigureMockMvc
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class RbacControllerTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    private final List<String> createdUsernames = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String username : createdUsernames) {
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
            if (user != null) {
                userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
            }
            sysUserMapper.delete(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        }
        createdUsernames.clear();
    }

    private void registerUser(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"nickname\":\"测试\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Test
    void adminCanAccessProtectedEndpoints() throws Exception {
        String token = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/menus/tree").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void adminMeShouldReturnRolesAndPermissions() throws Exception {
        String token = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        MvcResult result = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        List<String> roles = JsonPath.read(json, "$.data.roles");
        List<String> permissions = JsonPath.read(json, "$.data.permissions");

        assertTrue(roles.contains("admin"), "admin 用户应具有 admin 角色");
        assertTrue(permissions.contains("system:user:list"), "admin 应具备 system:user:list 权限");
        assertTrue(permissions.contains("system:role:assign"), "admin 应具备 system:role:assign 权限");
    }

    @Test
    void freshUserMeShouldReturnEmptyRolesAndPermissions() throws Exception {
        String username = uniqueUsername("rbac");
        createdUsernames.add(username);
        registerUser(username, "Passw0rd123");
        String token = loginAndGetToken(username, "Passw0rd123");

        MvcResult result = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        List<String> roles = JsonPath.read(json, "$.data.roles");
        List<String> permissions = JsonPath.read(json, "$.data.permissions");
        assertTrue(roles.isEmpty(), "新注册用户无角色");
        assertTrue(permissions.isEmpty(), "新注册用户无权限");
    }

    @Test
    void assignRoleThenUserGainsAccess() throws Exception {
        // 新用户无权限 → 403
        String username = uniqueUsername("rbac_asg");
        createdUsernames.add(username);
        registerUser(username, "Passw0rd123");
        String userToken = loginAndGetToken(username, "Passw0rd123");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        // admin 给该用户分配 admin 角色
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        SysRole adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "admin"));
        assertTrue(adminRole != null, "依赖 V3 初始化数据：admin 角色");

        mockMvc.perform(put("/api/users/" + user.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + adminRole.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 用户当前角色 ID 回显
        mockMvc.perform(get("/api/users/" + user.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value(adminRole.getId().intValue()));

        // 用户现在拥有权限，可访问用户管理接口
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
