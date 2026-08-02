package com.tuning.oasystem.service;

import java.util.List;
import java.util.Set;

/**
 * 权限聚合服务：按用户查询角色编码与权限标识（user → role → menu(perms)）。
 * <p>
 * 供安全鉴权（authorities 构建）与「当前用户信息」接口共用。
 */
public interface PermissionService {

    /** 用户启用的角色编码列表 */
    List<String> getRoleCodesByUserId(Long userId);

    /** 用户权限标识集合（去重），即 sys_menu.perms 非空项 */
    Set<String> getPermissionsByUserId(Long userId);

    /** 用户拥有的菜单 ID 列表（去重，含目录/菜单/按钮） */
    List<Long> getMenuIdsByUserId(Long userId);
}
