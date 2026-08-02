package com.tuning.oasystem.service;

import com.tuning.oasystem.dto.MenuRequest;
import com.tuning.oasystem.vo.MenuVO;

import java.util.List;

/**
 * 菜单管理服务
 */
public interface MenuService {

    /** 全部启用菜单的树形结构（按 sort 排序） */
    List<MenuVO> tree();

    /** 菜单详情 */
    MenuVO getById(Long id);

    /** 新增菜单 */
    MenuVO create(MenuRequest request);

    /** 修改菜单 */
    MenuVO update(Long id, MenuRequest request);

    /** 删除菜单（有子菜单时拒绝；并清理 role_menu 关联） */
    void delete(Long id);
}
