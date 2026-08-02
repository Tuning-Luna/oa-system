package com.tuning.oasystem.vo;

import com.tuning.oasystem.entity.SysMenu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单信息返回（含子菜单 children，用于树形展示）
 */
@Data
@Schema(description = "菜单信息（含子菜单）")
public class MenuVO {

    @Schema(description = "菜单 ID")
    private Long id;

    @Schema(description = "父菜单 ID，0 为根")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "类型：1目录 2菜单 3按钮")
    private Integer type;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "子菜单")
    private List<MenuVO> children = new ArrayList<>();

    public static MenuVO from(SysMenu menu) {
        if (menu == null) {
            return null;
        }
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setType(menu.getType());
        vo.setPerms(menu.getPerms());
        vo.setSort(menu.getSort());
        vo.setStatus(menu.getStatus());
        vo.setCreateTime(menu.getCreateTime());
        vo.setUpdateTime(menu.getUpdateTime());
        return vo;
    }
}
