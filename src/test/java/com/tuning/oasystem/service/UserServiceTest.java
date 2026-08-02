package com.tuning.oasystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.dto.UserQuery;
import com.tuning.oasystem.dto.UserUpdateRequest;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 用户服务测试（需 MySQL 运行）：分页、用户名/状态筛选、修改、逻辑删除。
 */
@SpringBootTest
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class UserServiceTest {

    @Autowired
    private UserService userService;

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

    private void createUser(String username, int status) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword("$2a$10$d4RHUelT9g8KZoidi6pTAO2m.VGr5Dc0Acc6u.d7ga1pSB3TZLEHO");
        u.setNickname("测试用户");
        u.setStatus(status);
        sysUserMapper.insert(u);
        createdUsernames.add(username);
    }

    private UserQuery query(String usernameKeyword, Integer status) {
        UserQuery q = new UserQuery();
        q.setPageNum(1L);
        q.setPageSize(10L);
        q.setUsername(usernameKeyword);
        q.setStatus(status);
        return q;
    }

    @Test
    void pageShouldReturnRecordsAndMatchTotal() {
        String prefix = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        createUser(prefix + "_a", 1);
        createUser(prefix + "_b", 0);

        PageResult<UserVO> result = userService.page(query(prefix, null));

        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals(1L, result.getPageNum());
        assertEquals(10L, result.getPageSize());
    }

    @Test
    void pageShouldFilterByStatus() {
        String prefix = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        createUser(prefix + "_a", 1);
        createUser(prefix + "_b", 0);

        PageResult<UserVO> result = userService.page(query(prefix, 1));

        assertEquals(1, result.getTotal());
        assertEquals(prefix + "_a", result.getRecords().get(0).getUsername());
    }

    @Test
    void updateShouldModifyAndDeleteShouldBeLogical() {
        String username = "usr_upd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        createUser(username, 1);
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

        UserUpdateRequest update = new UserUpdateRequest();
        update.setNickname("新昵称");
        update.setStatus(0);
        UserVO updated = userService.update(user.getId(), update);

        assertEquals("新昵称", updated.getNickname());
        assertEquals(0, updated.getStatus());

        userService.delete(user.getId());
        assertNull(sysUserMapper.selectById(user.getId()), "逻辑删除后 selectById 应查不到");
    }
}
