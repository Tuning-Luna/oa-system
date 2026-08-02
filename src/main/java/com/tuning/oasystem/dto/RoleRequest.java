package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色新增 / 修改请求
 */
@Data
@Schema(description = "角色新增/修改请求")
public class RoleRequest {

    @Schema(description = "角色名称", example = "部门主管")
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称最长 50 字符")
    private String name;

    @Schema(description = "角色编码（字母开头，字母数字下划线，全局唯一）", example = "manager")
    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,49}$", message = "角色编码须以字母开头，仅含字母/数字/下划线，最长 50")
    private String code;

    @Schema(description = "角色描述")
    @Size(max = 255, message = "角色描述最长 255 字符")
    private String description;

    @Schema(description = "状态：1启用 0禁用", example = "1")
    private Integer status;
}
