package com.tuning.oasystem.security;

import com.tuning.oasystem.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 认证主体：包装 {@link SysUser}，供 Spring Security 使用。
 * <p>
 * authorities 在 {@link UserDetailsServiceImpl} 中按 RBAC 聚合生成：
 * 角色编码以 {@code ROLE_xxx} 前缀（支持 hasRole），权限标识原样（支持 hasAuthority）。
 */
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final SysUser user;

    private final Collection<? extends GrantedAuthority> authorities;

    public LoginUser(SysUser user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
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
