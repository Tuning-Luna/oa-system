package com.tuning.oasystem.security;

import com.tuning.oasystem.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 认证主体：包装 {@link SysUser}，供 Spring Security 使用。
 * <p>
 * 阶段 2 尚未引入角色/权限，authorities 为空集合；阶段 3 的 RBAC 在此补充。
 */
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final SysUser user;

    public LoginUser(SysUser user) {
        this.user = user;
    }

    public SysUser getUser() {
        return user;
    }

    public Long getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
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
