package com.tuning.oasystem.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.service.PermissionService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 用户加载：登录时按用户名，JWT 过滤器按用户 ID；并聚合 RBAC 权限写入 authorities。
 * <p>
 * MyBatis-Plus 的 @TableLogic 会自动附加 deleted=0 条件，无需手动处理逻辑删除。
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final PermissionService permissionService;

    public UserDetailsServiceImpl(SysUserMapper sysUserMapper, PermissionService permissionService) {
        this.sysUserMapper = sysUserMapper;
        this.permissionService = permissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return buildLoginUser(user);
    }

    /** 供 JWT 过滤器按用户 ID 加载（用户被删除/禁用时返回 null 的检查由调用方处理） */
    public LoginUser loadUserById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: id=" + id);
        }
        return buildLoginUser(user);
    }

    private LoginUser buildLoginUser(SysUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 角色编码 → ROLE_xxx（支持 @PreAuthorize("hasRole('...')")）
        for (String code : permissionService.getRoleCodesByUserId(user.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + code));
        }
        // 权限标识原样（支持 @PreAuthorize("hasAuthority('system:user:list')")）
        Set<String> permissions = permissionService.getPermissionsByUserId(user.getId());
        for (String perm : permissions) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }
        return new LoginUser(user, authorities);
    }
}
