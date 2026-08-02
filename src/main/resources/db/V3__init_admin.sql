-- ============================================================
-- 阶段 3：初始化权限数据（幂等，可重复执行）
-- 1. 内置 admin 角色（超级管理员）
-- 2. 基础菜单权限树（系统管理 → 用户/角色/菜单管理 + 按钮权限）
-- 3. admin 用户（来自 V2__sys_user.sql）挂到 admin 角色
-- 4. admin 角色授予全部基础菜单权限
--
-- 幂等策略：菜单/角色指定固定主键，重复执行走 ON DUPLICATE KEY UPDATE；
--           关联表依赖 (user_id,role_id) / (role_id,menu_id) 唯一键去重。
-- ============================================================

-- 1. admin 角色（超级管理员，授予所有权限）
INSERT INTO sys_role (id, name, code, description, status, create_time, update_time, deleted)
VALUES (1, '系统管理员', 'admin', '超级管理员，拥有全部权限', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 2. 基础菜单权限树
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, type, perms, sort, status, create_time, update_time, deleted)
VALUES
    -- 目录：系统管理
    (100, 0,   '系统管理', '/system',         NULL,                  'system', 1, NULL,                  1, 1, NOW(), NOW(), 0),
    -- 菜单：用户管理
    (200, 100, '用户管理', '/system/user',    'system/user/index',   'user',   2, NULL,                  1, 1, NOW(), NOW(), 0),
    (201, 200, '用户查询', NULL,              NULL,                  NULL,     3, 'system:user:list',    1, 1, NOW(), NOW(), 0),
    (202, 200, '用户新增', NULL,              NULL,                  NULL,     3, 'system:user:add',     2, 1, NOW(), NOW(), 0),
    (203, 200, '用户修改', NULL,              NULL,                  NULL,     3, 'system:user:edit',    3, 1, NOW(), NOW(), 0),
    (204, 200, '用户删除', NULL,              NULL,                  NULL,     3, 'system:user:remove',  4, 1, NOW(), NOW(), 0),
    -- 菜单：角色管理
    (210, 100, '角色管理', '/system/role',    'system/role/index',   'role',   2, NULL,                  2, 1, NOW(), NOW(), 0),
    (211, 210, '角色查询', NULL,              NULL,                  NULL,     3, 'system:role:list',    1, 1, NOW(), NOW(), 0),
    (212, 210, '角色新增', NULL,              NULL,                  NULL,     3, 'system:role:add',     2, 1, NOW(), NOW(), 0),
    (213, 210, '角色修改', NULL,              NULL,                  NULL,     3, 'system:role:edit',    3, 1, NOW(), NOW(), 0),
    (214, 210, '角色删除', NULL,              NULL,                  NULL,     3, 'system:role:remove',  4, 1, NOW(), NOW(), 0),
    (215, 210, '分配菜单', NULL,              NULL,                  NULL,     3, 'system:role:assign',  5, 1, NOW(), NOW(), 0),
    -- 菜单：菜单管理
    (220, 100, '菜单管理', '/system/menu',    'system/menu/index',   'menu',   2, NULL,                  3, 1, NOW(), NOW(), 0),
    (221, 220, '菜单查询', NULL,              NULL,                  NULL,     3, 'system:menu:list',    1, 1, NOW(), NOW(), 0),
    (222, 220, '菜单新增', NULL,              NULL,                  NULL,     3, 'system:menu:add',     2, 1, NOW(), NOW(), 0),
    (223, 220, '菜单修改', NULL,              NULL,                  NULL,     3, 'system:menu:edit',    3, 1, NOW(), NOW(), 0),
    (224, 220, '菜单删除', NULL,              NULL,                  NULL,     3, 'system:menu:remove',  4, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 3. admin 用户（V2 已建）挂到 admin 角色
-- 依赖 sys_user_role 唯一键 (user_id, role_id) 去重，重复执行自动忽略
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'admin';

-- 4. admin 角色授予全部基础菜单权限
-- 依赖 sys_role_menu 唯一键 (role_id, menu_id) 去重，重复执行自动忽略
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.code = 'admin'
  AND m.id IN (100, 200, 201, 202, 203, 204, 210, 211, 212, 213, 214, 215, 220, 221, 222, 223, 224);
