package com.tuning.oasystem.security;

import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.vo.UserInfoVO;
import com.tuning.oasystem.vo.UserVO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 认证主体：包装 {@link SysUser}，供 Spring Security 使用。
 * <p>
 * authorities 在 {@link UserDetailsServiceImpl} 中按 RBAC 聚合生成：
 * 角色编码以 {@code ROLE_xxx} 前缀（支持 hasRole），权限标识原样（支持 hasAuthority）。
 * 阶段 4 起支持与 Redis 缓存的 {@link UserInfoVO} 互转（鉴权过滤器按缓存快速还原主体）。
 */
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final SysUser user;

    private final Collection<? extends GrantedAuthority> authorities;

    public LoginUser(SysUser user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
    }

    /** 从缓存 UserInfoVO 还原 LoginUser（仅缓存关键字段，避免依赖 DB） */
    public static LoginUser fromInfoVO(UserInfoVO vo) {
        UserVO userVO = vo.getUser();
        SysUser user = new SysUser();
        user.setId(userVO.getId());
        user.setUsername(userVO.getUsername());
        user.setNickname(userVO.getNickname());
        user.setEmail(userVO.getEmail());
        user.setPhone(userVO.getPhone());
        user.setAvatar(userVO.getAvatar());
        user.setStatus(userVO.getStatus());
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : vo.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String perm : vo.getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }
        return new LoginUser(user, authorities);
    }

    /** 转成可缓存的 UserInfoVO（按 authorities 拆分角色与权限，无需额外 DB 查询） */
    public UserInfoVO toInfoVO() {
        List<String> roles = new ArrayList<>();
        Set<String> permissions = new LinkedHashSet<>();
        for (GrantedAuthority authority : authorities) {
            String value = authority.getAuthority();
            if (value.startsWith("ROLE_")) {
                roles.add(value.substring(5));
            } else {
                permissions.add(value);
            }
        }
        return UserInfoVO.of(UserVO.from(user), roles, permissions);
    }

    public SysUser getUser() {
        return user;
    }

    public Long getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() != null && user.getStatus() == 1;
    }
}
