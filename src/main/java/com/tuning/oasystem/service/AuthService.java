package com.tuning.oasystem.service;

import com.tuning.oasystem.dto.LoginRequest;
import com.tuning.oasystem.dto.RegisterRequest;
import com.tuning.oasystem.vo.LoginResponse;
import com.tuning.oasystem.vo.UserInfoVO;
import com.tuning.oasystem.vo.UserVO;

/**
 * 认证服务：注册 / 登录 / 当前用户信息
 */
public interface AuthService {

    /** 注册：用户名唯一性校验 + BCrypt 加密存储 */
    UserVO register(RegisterRequest request);

    /** 登录：校验账号密码，签发 JWT 并写入 Redis 登录态 */
    LoginResponse login(LoginRequest request);

    /** 登出：删除 Redis 登录态缓存，token 立即失效 */
    void logout(String authorization);

    /** 按用户 ID 返回当前用户信息（含角色与权限），优先读缓存 */
    UserInfoVO getCurrentUser(Long userId);
}
