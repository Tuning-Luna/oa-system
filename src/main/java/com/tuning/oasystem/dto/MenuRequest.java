package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜单新增 / 修改请求
 */
@Data
@Schema(description = "菜单新增/修改请求")
public class MenuRequest {

    @Schema(description = "父菜单 ID，0 为根", example = "0")
    @NotNull(message = "父菜单 ID 不能为空")
    private Long parentId;

    @Schema(description = "菜单名称", example = "用户管理")
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称最长 50 字符")
    private String name;

    @Schema(description = "路由地址", example = "/system/user")
    @Size(max = 255, message = "路由地址最长 255 字符")
    private String path;

    @Schema(description = "组件路径", example = "system/user/index")
    @Size(max = 255, message = "组件路径最长 255 字符")
    private String component;

    @Schema(description = "图标", example = "user")
    @Size(max = 50, message = "图标最长 50 字符")
    private String icon;

    @Schema(description = "类型：1目录 2菜单 3按钮", example = "2")
    @NotNull(message = "菜单类型不能为空")
    @Min(value = 1, message = "菜单类型取值 1/2/3")
    @Max(value = 3, message = "菜单类型取值 1/2/3")
    private Integer type;

    @Schema(description = "权限标识，如 system:user:list（按钮必填）")
    @Size(max = 100, message = "权限标识最长 100 字符")
    private String perms;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

    @Schema(description = "状态：1启用 0禁用", example = "1")
    private Integer status;
}
