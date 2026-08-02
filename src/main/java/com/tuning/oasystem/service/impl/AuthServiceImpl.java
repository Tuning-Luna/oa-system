package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.common.CacheKeys;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.LoginRequest;
import com.tuning.oasystem.dto.RegisterRequest;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.security.JwtTokenProvider;
import com.tuning.oasystem.service.AuthService;
import com.tuning.oasystem.service.PermissionService;
import com.tuning.oasystem.service.RedisService;
import com.tuning.oasystem.vo.LoginResponse;
import com.tuning.oasystem.vo.UserInfoVO;
import com.tuning.oasystem.vo.UserVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证服务实现
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionService permissionService;
    private final RedisService redisService;
    private final long expirationSeconds;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            PermissionService permissionService,
            RedisService redisService,
            @Value("${jwt.expiration}") long expirationSeconds) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.permissionService = permissionService;
        this.redisService = redisService;
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public UserVO register(RegisterRequest request) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        sysUserMapper.insert(user);
        return UserVO.from(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 统一提示，避免泄露用户名是否存在
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        // 登录态缓存：token → 用户ID（登出删除，token 立即失效）
        redisService.set(CacheKeys.AUTH_TOKEN + token, String.valueOf(user.getId()), expirationSeconds);
        // 用户信息缓存（含角色/权限）：鉴权过滤器与 /me 共用
        UserInfoVO userInfo = UserInfoVO.of(
                UserVO.from(user),
                permissionService.getRoleCodesByUserId(user.getId()),
                permissionService.getPermissionsByUserId(user.getId()));
        redisService.set(CacheKeys.USER_INFO + user.getId(), userInfo, expirationSeconds);
        return new LoginResponse(token, expirationSeconds, UserVO.from(user));
    }

    @Override
    public void logout(String authorization) {
        String token = authorization;
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length());
        }
        if (StringUtils.hasText(token)) {
            redisService.delete(CacheKeys.AUTH_TOKEN + token);
        }
    }

    @Override
    public UserInfoVO getCurrentUser(Long userId) {
        // 优先读缓存；miss 查 DB 并回填
        UserInfoVO cached = redisService.get(CacheKeys.USER_INFO + userId, UserInfoVO.class);
        if (cached != null && cached.getUser() != null) {
            return cached;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在或已被删除");
        }
        UserInfoVO userInfo = UserInfoVO.of(
                UserVO.from(user),
                permissionService.getRoleCodesByUserId(userId),
                permissionService.getPermissionsByUserId(userId));
        redisService.set(CacheKeys.USER_INFO + userId, userInfo, expirationSeconds);
        return userInfo;
    }
}
