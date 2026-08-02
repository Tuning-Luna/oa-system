package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.UserQuery;
import com.tuning.oasystem.dto.UserUpdateRequest;
import com.tuning.oasystem.entity.SysRole;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.entity.SysUserRole;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.SysRoleMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.mapper.SysUserRoleMapper;
import com.tuning.oasystem.service.UserService;
import com.tuning.oasystem.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户管理服务实现
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class UserServiceImpl implements UserService {

    private static final long DEFAULT_PAGE_SIZE = 10;
    private static final long MAX_PAGE_SIZE = 100;

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public UserServiceImpl(SysUserMapper sysUserMapper,
            SysUserRoleMapper userRoleMapper,
            SysRoleMapper roleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public PageResult<UserVO> page(UserQuery query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(query.getPageSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
            .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
            .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
            .orderByDesc(SysUser::getId);

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<UserVO> records = page.getRecords().stream().map(UserVO::from).toList();
        return PageResult.of(page.getTotal(), records, page.getCurrent(), page.getSize());
    }

    @Override
    public UserVO getById(Long id) {
        return UserVO.from(requireUser(id));
    }

    @Override
    public UserVO update(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        sysUserMapper.updateById(user);
        return UserVO.from(sysUserMapper.selectById(id));
    }

    @Override
    public void delete(Long id) {
        requireUser(id);
        sysUserMapper.deleteById(id);
        // 清理用户-角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        requireUser(userId);
        if (roleIds == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色 ID 列表不能为空");
        }
        List<Long> distinctIds = roleIds.stream().distinct().toList();
        if (!distinctIds.isEmpty()) {
            Long existing = roleMapper.selectCount(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, distinctIds));
            if (existing == null || existing != distinctIds.size()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "存在无效的角色 ID");
            }
        }
        // 全量替换：先删旧关联，再插入新关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (Long roleId : distinctIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        requireUser(userId);
        return userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在: id=" + id);
        }
        return user;
    }
}
