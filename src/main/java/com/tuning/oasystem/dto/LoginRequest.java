package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(description = "登录名", example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", example = "Admin@123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
