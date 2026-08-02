package com.tuning.oasystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.common.CacheKeys;
import com.tuning.oasystem.dto.LoginRequest;
import com.tuning.oasystem.dto.MenuRequest;
import com.tuning.oasystem.dto.RegisterRequest;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.mapper.SysMenuMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.vo.LoginResponse;
import com.tuning.oasystem.vo.MenuVO;
import com.tuning.oasystem.vo.UserInfoVO;
import com.tuning.oasystem.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Redis 缓存集成测试（需 Redis + MySQL 运行）：
 * 登录态缓存、登出 token 失效、分配角色写后失效、菜单树缓存命中。
 */
@SpringBootTest
@AutoConfigureMockMvc
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class RedisCacheIntegrationTest {

    @Autowired
    private RedisService redisService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private MockMvc mockMvc;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdMenuIds = new ArrayList<>();
    private final List<String> createdTokens = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String token : createdTokens) {
            redisService.delete(CacheKeys.AUTH_TOKEN + token);
        }
        createdTokens.clear();
        for (Long id : createdUserIds) {
            redisService.delete(CacheKeys.USER_INFO + id);
            redisService.delete(CacheKeys.USER_DETAIL + id);
            sysUserMapper.deleteById(id);
        }
        createdUserIds.clear();
        for (Long id : createdMenuIds) {
            menuMapper.deleteById(id);
        }
        createdMenuIds.clear();
        redisService.delete(CacheKeys.MENU_TREE);
    }

    private String register(String prefix) {
        RegisterRequest req = new RegisterRequest();
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        req.setUsername(username);
        req.setPassword("Passw0rd123");
        req.setNickname("缓存测试");
        UserVO vo = authService.register(req);
        createdUserIds.add(vo.getId());
        return username;
    }

    private String login(String username) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword("Passw0rd123");
        LoginResponse resp = authService.login(req);
        createdTokens.add(resp.getToken());
        return resp.getToken();
    }

    private Long userIdOf(String username) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        assertNotNull(user);
        return user.getId();
    }

    @Test
    void loginShouldPopulateTokenAndUserInfoCache() {
        String username = register("cache_login");
        Long userId = userIdOf(username);
        String token = login(username);

        assertTrue(redisService.hasKey(CacheKeys.AUTH_TOKEN + token), "登录应缓存 token→userId");
        assertTrue(redisService.hasKey(CacheKeys.USER_INFO + userId), "登录应缓存用户信息");

        // 缓存读取回程正确（UserInfoVO JSON 往返）
        UserInfoVO info = authService.getCurrentUser(userId);
        assertNotNull(info.getUser());
        assertEquals(username, info.getUser().getUsername());
        assertTrue(info.getRoles().isEmpty(), "新注册用户无角色");
    }

    @Test
    void logoutShouldInvalidateTokenAndReturn401() throws Exception {
        String username = register("cache_logout");
        String token = login(username);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        authService.logout("Bearer " + token);
        assertFalse(redisService.hasKey(CacheKeys.AUTH_TOKEN + token), "登出应删除 token 登录态");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void assignRolesShouldEvictUserInfoCache() {
        String username = register("cache_assign");
        Long userId = userIdOf(username);
        login(username);
        assertTrue(redisService.hasKey(CacheKeys.USER_INFO + userId));

        // 分配空角色 → 权限变更 → 缓存失效
        userService.assignRoles(userId, List.of());
        assertFalse(redisService.hasKey(CacheKeys.USER_INFO + userId), "分配角色后应失效用户权限缓存");
    }

    @Test
    void menuTreeShouldBeServedFromCacheAfterDirectDbDelete() {
        redisService.delete(CacheKeys.MENU_TREE);
        menuService.tree(); // 预热缓存

        // 新增菜单（触发缓存失效），再重建缓存
        MenuRequest request = new MenuRequest();
        request.setParentId(0L);
        request.setName("缓存目录_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6));
        request.setType(1);
        request.setSort(99);
        request.setStatus(1);
        MenuVO created = menuService.create(request);
        createdMenuIds.add(created.getId());
        menuService.tree(); // 重建缓存，应包含新菜单

        // 绕过服务直接逻辑删除（不触发缓存失效）
        menuMapper.deleteById(created.getId());

        // 再次查询应命中缓存：仍包含已逻辑删除的菜单
        List<MenuVO> tree = menuService.tree();
        assertNotNull(findNode(tree, created.getId()), "命中缓存应仍包含已删除菜单（证明未走 DB）");
    }

    private MenuVO findNode(List<MenuVO> nodes, Long id) {
        for (MenuVO node : nodes) {
            if (node.getId().equals(id)) {
                return node;
            }
            MenuVO found = findNode(node.getChildren(), id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
