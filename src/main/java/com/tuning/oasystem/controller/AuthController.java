package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.dto.LoginRequest;
import com.tuning.oasystem.dto.RegisterRequest;
import com.tuning.oasystem.security.LoginUser;
import com.tuning.oasystem.service.AuthService;
import com.tuning.oasystem.vo.LoginResponse;
import com.tuning.oasystem.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 当前用户
 */
@Tag(name = "认证", description = "注册 / 登录 / 当前用户信息")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "注册", description = "用户名唯一，密码 BCrypt 加密存储")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "登录", description = "校验账号密码，签发 JWT")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "当前用户信息", description = "根据 JWT 返回当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> me(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(authService.getCurrentUser(loginUser.getUserId()));
    }
}
