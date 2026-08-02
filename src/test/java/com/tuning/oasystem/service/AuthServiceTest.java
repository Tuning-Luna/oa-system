package com.tuning.oasystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.dto.LoginRequest;
import com.tuning.oasystem.dto.RegisterRequest;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.vo.LoginResponse;
import com.tuning.oasystem.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 认证服务测试（需 MySQL 运行）：
 * 注册 BCrypt 加密、用户名唯一；登录签发 token、密码错误/禁用账号抛异常。
 */
@SpringBootTest
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private SysUserMapper sysUserMapper;

    private final List<String> createdUsernames = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String username : createdUsernames) {
            sysUserMapper.delete(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        }
        createdUsernames.clear();
    }

    private String uniqueUsername(String prefix) {
        // 固定长度后缀，避免超过用户名 20 字符限制
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private RegisterRequest registerRequest(String username, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setPassword(password);
        r.setNickname("测试用户");
        r.setEmail(username + "@test.local");
        return r;
    }

    @Test
    void registerShouldEncryptPassword() {
        String username = uniqueUsername("svc_reg");
        createdUsernames.add(username);

        UserVO vo = authService.register(registerRequest(username, "Passw0rd123"));

        assertNotNull(vo.getId());
        assertEquals(username, vo.getUsername());

        SysUser dbUser = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        assertNotNull(dbUser);
        assertNotEquals("Passw0rd123", dbUser.getPassword(), "数据库不应存明文");
        assertTrue(dbUser.getPassword().startsWith("$2"), "密码应为 BCrypt 密文");
        assertEquals(1, dbUser.getStatus());
    }

    @Test
    void registerDuplicateUsernameShouldThrow() {
        String username = uniqueUsername("svc_dup");
        createdUsernames.add(username);
        authService.register(registerRequest(username, "Passw0rd123"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(registerRequest(username, "Passw0rd456")));
        assertTrue(ex.getMessage().contains("已存在"));
    }

    @Test
    void loginShouldReturnToken() {
        String username = uniqueUsername("svc_login");
        createdUsernames.add(username);
        String password = "Passw0rd123";
        authService.register(registerRequest(username, password));

        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        LoginResponse resp = authService.login(req);

        assertNotNull(resp.getToken(), "登录应签发 token");
        assertEquals("Bearer", resp.getTokenType());
        assertEquals(username, resp.getUser().getUsername());
    }

    @Test
    void loginWrongPasswordShouldThrow() {
        String username = uniqueUsername("svc_wrong");
        createdUsernames.add(username);
        authService.register(registerRequest(username, "Passw0rd123"));

        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword("Wrong123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void loginDisabledUserShouldThrow() {
        String username = uniqueUsername("svc_dis");
        createdUsernames.add(username);
        authService.register(registerRequest(username, "Passw0rd123"));

        SysUser dbUser = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        dbUser.setStatus(0);
        sysUserMapper.updateById(dbUser);

        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword("Passw0rd123");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().contains("禁用"));
    }
}
