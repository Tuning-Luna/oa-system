package com.tuning.oasystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单/权限实体（sys_menu）
 * <p>
 * type：1目录 2菜单 3按钮；perms 权限标识仅在按钮（type=3）上配置，
 * 用户权限 = 用户 → 角色 → 菜单（perms 非空）聚合。
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单ID，0为根 */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 图标 */
    private String icon;

    /** 类型：1目录 2菜单 3按钮 */
    private Integer type;

    /** 权限标识，如 system:user:list */
    private String perms;

    /** 显示顺序 */
    private Integer sort;

    /** 状态：1启用 0禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0否 1是 */
    @TableLogic
    private Integer deleted;
}
