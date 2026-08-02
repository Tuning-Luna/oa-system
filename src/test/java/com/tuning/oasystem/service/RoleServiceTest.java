package com.tuning.oasystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.dto.RoleQuery;
import com.tuning.oasystem.dto.RoleRequest;
import com.tuning.oasystem.entity.SysMenu;
import com.tuning.oasystem.entity.SysRoleMenu;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.SysMenuMapper;
import com.tuning.oasystem.mapper.SysRoleMapper;
import com.tuning.oasystem.mapper.SysRoleMenuMapper;
import com.tuning.oasystem.vo.RoleVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角色服务测试（需 MySQL 运行）：角色 CRUD、编码唯一、分配菜单、删除清理关联。
 */
@SpringBootTest
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class RoleServiceTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    private final List<Long> createdRoleIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdRoleIds) {
            try {
                roleService.delete(id);
            } catch (BusinessException ignore) {
                // 测试内已删除
            }
        }
        createdRoleIds.clear();
    }

    private String uniqueCode(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private RoleRequest request(String name, String code, Integer status) {
        RoleRequest r = new RoleRequest();
        r.setName(name);
        r.setCode(code);
        r.setDescription("测试角色");
        r.setStatus(status);
        return r;
    }

    @Test
    void createShouldPersistAndReturn() {
        String code = uniqueCode("role");
        RoleVO vo = roleService.create(request("测试角色", code, 1));
        createdRoleIds.add(vo.getId());

        assertNotNull(vo.getId());
        assertEquals(code, vo.getCode());
        assertEquals("测试角色", vo.getName());
        assertEquals(1, vo.getStatus());

        RoleVO db = roleService.getById(vo.getId());
        assertNotNull(db);
        assertEquals(code, db.getCode());
    }

    @Test
    void createDuplicateCodeShouldThrow() {
        String code = uniqueCode("dup");
        createdRoleIds.add(roleService.create(request("角色A", code, 1)).getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roleService.create(request("角色B", code, 1)));
        assertTrue(ex.getMessage().contains("已存在"));
    }

    @Test
    void updateShouldModifyFields() {
        Long id = roleService.create(request("原名", uniqueCode("upd"), 1)).getId();
        createdRoleIds.add(id);

        RoleRequest update = request("新名", "upd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8), 0);
        RoleVO vo = roleService.update(id, update);

        assertEquals("新名", vo.getName());
        assertEquals(0, vo.getStatus());
        assertEquals(update.getCode(), vo.getCode());
    }

    @Test
    void deleteShouldRemoveRoleAndAssociations() {
        Long id = roleService.create(request("待删", uniqueCode("del"), 1)).getId();
        createdRoleIds.add(id);

        // 取一个已有的按钮权限菜单（V3 初始化数据）
        SysMenu menu = menuMapper.selectOne(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPerms, "system:user:list"));
        assertNotNull(menu, "依赖 V3 初始化数据：菜单 system:user:list");

        roleService.assignMenus(id, List.of(menu.getId()));
        assertTrue(roleService.getMenuIds(id).contains(menu.getId()));

        roleService.delete(id);
        createdRoleIds.remove(id);

        assertNull(roleMapper.selectById(id), "角色逻辑删除后查不到");
        Long relCount = roleMenuMapper.selectCount(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        assertEquals(0, relCount, "删除角色应清理角色-菜单关联");
        assertThrows(BusinessException.class, () -> roleService.getById(id));
    }

    @Test
    void pageShouldFilterByNameAndStatus() {
        String prefix = "RBAC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        createdRoleIds.add(roleService.create(request(prefix + "_启用", uniqueCode("pg"), 1)).getId());
        createdRoleIds.add(roleService.create(request(prefix + "_禁用", uniqueCode("pg"), 0)).getId());

        RoleQuery q = new RoleQuery();
        q.setPageNum(1L);
        q.setPageSize(10L);
        q.setName(prefix);
        PageResult<RoleVO> result = roleService.page(q);
        assertEquals(2, result.getTotal());

        q.setStatus(1);
        PageResult<RoleVO> enabled = roleService.page(q);
        assertEquals(1, enabled.getTotal());
        assertTrue(enabled.getRecords().get(0).getName().endsWith("_启用"));
    }

    @Test
    void listAllShouldReturnEnabledOnly() {
        String code = uniqueCode("all");
        createdRoleIds.add(roleService.create(request("启用角色", code, 1)).getId());
        createdRoleIds.add(roleService.create(request("禁用角色", uniqueCode("all"), 0)).getId());

        List<RoleVO> all = roleService.listAll();
        assertTrue(all.stream().anyMatch(r -> r.getCode().equals(code)), "listAll 应包含启用角色");
        assertTrue(all.stream().noneMatch(r -> "禁用角色".equals(r.getName())), "listAll 不应包含禁用角色");
    }

    @Test
    void assignMenusShouldReplaceAll() {
        Long id = roleService.create(request("分配", uniqueCode("am"), 1)).getId();
        createdRoleIds.add(id);

        SysMenu m1 = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPerms, "system:user:list"));
        SysMenu m2 = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPerms, "system:role:list"));
        assertNotNull(m1);
        assertNotNull(m2);

        roleService.assignMenus(id, List.of(m1.getId(), m2.getId()));
        assertEquals(2, roleService.getMenuIds(id).size());

        // 全量替换为单个菜单
        roleService.assignMenus(id, List.of(m2.getId()));
        List<Long> ids = roleService.getMenuIds(id);
        assertEquals(1, ids.size());
        assertEquals(m2.getId(), ids.get(0));
    }
}
