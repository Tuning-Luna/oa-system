package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户请求参数
 */
@Data
@Schema(description = "修改用户请求")
public class UserUpdateRequest {

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称最长 50 个字符")
    private String nickname;

    @Schema(description = "邮箱")
    @Pattern(regexp = "^$|^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$", message = "邮箱格式不正确")
    private String email;

    @Schema(description = "手机号")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "头像 URL")
    @Size(max = 255, message = "头像 URL 最长 255 个字符")
    private String avatar;

    @Schema(description = "状态：1启用 0禁用")
    @Min(value = 0, message = "status 只能是 0 或 1")
    @Max(value = 1, message = "status 只能是 0 或 1")
    private Integer status;
}
