package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.entity.SysMenu;
import com.tuning.oasystem.entity.SysRole;
import com.tuning.oasystem.entity.SysRoleMenu;
import com.tuning.oasystem.entity.SysUserRole;
import com.tuning.oasystem.mapper.SysMenuMapper;
import com.tuning.oasystem.mapper.SysRoleMapper;
import com.tuning.oasystem.mapper.SysRoleMenuMapper;
import com.tuning.oasystem.mapper.SysUserRoleMapper;
import com.tuning.oasystem.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限聚合服务实现
 * <p>
 * 采用多次单表查询组装（user_role → role → role_menu → menu），
 * 数据量小、查询简单，避免引入自定义 XML/JOIN，符合现有代码风格。
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class PermissionServiceImpl implements PermissionService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public PermissionServiceImpl(SysUserRoleMapper userRoleMapper,
            SysRoleMapper roleMapper,
            SysRoleMenuMapper roleMenuMapper,
            SysMenuMapper menuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        List<Long> roleIds = roleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectList(
                        new LambdaQueryWrapper<SysRole>()
                                .in(SysRole::getId, roleIds)
                                .eq(SysRole::getStatus, 1))
                .stream()
                .map(SysRole::getCode)
                .toList();
    }

    @Override
    public Set<String> getPermissionsByUserId(Long userId) {
        List<Long> menuIds = menuIdsOf(userId);
        if (menuIds.isEmpty()) {
            return Collections.emptySet();
        }
        // perms 非空的菜单（type=3 按钮）才有权限标识；去重
        return menuMapper.selectList(
                        new LambdaQueryWrapper<SysMenu>()
                                .in(SysMenu::getId, menuIds)
                                .eq(SysMenu::getStatus, 1)
                                .isNotNull(SysMenu::getPerms)
                                .ne(SysMenu::getPerms, ""))
                .stream()
                .map(SysMenu::getPerms)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public List<Long> getMenuIdsByUserId(Long userId) {
        return menuIdsOf(userId);
    }

    private List<Long> roleIdsOf(Long userId) {
        return userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }

    private List<Long> menuIdsOf(Long userId) {
        List<Long> roleIds = roleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .toList();
    }
}
