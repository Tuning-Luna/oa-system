package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.RoleQuery;
import com.tuning.oasystem.dto.RoleRequest;
import com.tuning.oasystem.entity.SysRole;
import com.tuning.oasystem.entity.SysRoleMenu;
import com.tuning.oasystem.entity.SysUserRole;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.SysRoleMapper;
import com.tuning.oasystem.mapper.SysRoleMenuMapper;
import com.tuning.oasystem.mapper.SysUserRoleMapper;
import com.tuning.oasystem.service.RoleService;
import com.tuning.oasystem.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色管理服务实现
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class RoleServiceImpl implements RoleService {

    private static final long DEFAULT_PAGE_SIZE = 10;
    private static final long MAX_PAGE_SIZE = 100;

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public RoleServiceImpl(SysRoleMapper roleMapper,
            SysRoleMenuMapper roleMenuMapper,
            SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public PageResult<RoleVO> page(RoleQuery query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(query.getPageSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(query.getName()), SysRole::getName, query.getName())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByDesc(SysRole::getId);

        Page<SysRole> page = roleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<RoleVO> records = page.getRecords().stream().map(RoleVO::from).toList();
        return PageResult.of(page.getTotal(), records, page.getCurrent(), page.getSize());
    }

    @Override
    public List<RoleVO> listAll() {
        return roleMapper.selectList(
                        new LambdaQueryWrapper<SysRole>()
                                .eq(SysRole::getStatus, 1)
                                .orderByAsc(SysRole::getId))
                .stream()
                .map(RoleVO::from)
                .toList();
    }

    @Override
    public RoleVO getById(Long id) {
        return RoleVO.from(requireRole(id));
    }

    @Override
    public RoleVO create(RoleRequest request) {
        if (existsCode(request.getCode(), null)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色编码已存在: " + request.getCode());
        }
        SysRole role = new SysRole();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        roleMapper.insert(role);
        return RoleVO.from(roleMapper.selectById(role.getId()));
    }

    @Override
    public RoleVO update(Long id, RoleRequest request) {
        requireRole(id);
        if (existsCode(request.getCode(), id)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色编码已存在: " + request.getCode());
        }
        SysRole role = new SysRole();
        role.setId(id);
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        roleMapper.updateById(role);
        return RoleVO.from(requireRole(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireRole(id);
        roleMapper.deleteById(id);
        // 物理清理关联，避免角色逻辑删除后残留权限
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        if (menuIds == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "菜单 ID 列表不能为空");
        }
        List<Long> distinctIds = menuIds.stream().distinct().toList();
        // 全量替换：先删旧关联，再插入新关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        for (Long menuId : distinctIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        requireRole(roleId);
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }

    private boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        Long count = roleMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在: id=" + id);
        }
        return role;
    }
}
