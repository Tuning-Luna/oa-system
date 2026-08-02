-- ============================================================
-- 阶段 3：权限模块（RBAC）
-- 角色表 / 菜单权限表 / 用户-角色关联 / 角色-菜单关联
-- 说明：脚本可重复执行（幂等）。表不存在则创建。
-- 关联表（sys_user_role / sys_role_menu）加唯一键，配合初始化脚本幂等插入。
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50)  NOT NULL COMMENT '角色名称',
    code        VARCHAR(50)  NOT NULL COMMENT '角色编码（唯一）',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) COMMENT = '角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为根',
    name        VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    path        VARCHAR(255) DEFAULT NULL COMMENT '路由地址',
    component   VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    icon        VARCHAR(50)  DEFAULT NULL COMMENT '图标',
    type        TINYINT      NOT NULL DEFAULT 2 COMMENT '1目录 2菜单 3按钮',
    perms       VARCHAR(100) DEFAULT NULL COMMENT '权限标识，如 system:user:list',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (id)
) COMMENT = '菜单/权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT = '用户-角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) COMMENT = '角色-菜单关联表';
