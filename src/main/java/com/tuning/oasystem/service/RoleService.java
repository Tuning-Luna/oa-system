package com.tuning.oasystem.service;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.dto.RoleQuery;
import com.tuning.oasystem.dto.RoleRequest;
import com.tuning.oasystem.vo.RoleVO;

import java.util.List;

/**
 * 角色管理服务
 */
public interface RoleService {

    /** 角色分页查询（名称模糊 / 状态筛选） */
    PageResult<RoleVO> page(RoleQuery query);

    /** 全部启用角色（供用户分配角色下拉） */
    List<RoleVO> listAll();

    /** 角色详情 */
    RoleVO getById(Long id);

    /** 新增角色 */
    RoleVO create(RoleRequest request);

    /** 修改角色 */
    RoleVO update(Long id, RoleRequest request);

    /** 删除角色（逻辑删除，并清理 user_role / role_menu 关联） */
    void delete(Long id);

    /** 给角色分配菜单（全量替换） */
    void assignMenus(Long roleId, List<Long> menuIds);

    /** 角色当前菜单 ID 列表 */
    List<Long> getMenuIds(Long roleId);
}
