package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.MenuRequest;
import com.tuning.oasystem.entity.SysMenu;
import com.tuning.oasystem.entity.SysRoleMenu;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.SysMenuMapper;
import com.tuning.oasystem.mapper.SysRoleMenuMapper;
import com.tuning.oasystem.service.MenuService;
import com.tuning.oasystem.vo.MenuVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理服务实现
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public MenuServiceImpl(SysMenuMapper menuMapper, SysRoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public List<MenuVO> tree() {
        List<SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getStatus, 1)
                        .orderByAsc(SysMenu::getSort));
        return buildTree(menus.stream().map(MenuVO::from).toList());
    }

    @Override
    public MenuVO getById(Long id) {
        return MenuVO.from(requireMenu(id));
    }

    @Override
    public MenuVO create(MenuRequest request) {
        validateParent(request.getParentId());
        SysMenu menu = new SysMenu();
        applyRequest(menu, request);
        menuMapper.insert(menu);
        return MenuVO.from(menuMapper.selectById(menu.getId()));
    }

    @Override
    public MenuVO update(Long id, MenuRequest request) {
        requireMenu(id);
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父菜单不能是自身");
        }
        validateParent(request.getParentId());
        SysMenu menu = new SysMenu();
        menu.setId(id);
        applyRequest(menu, request);
        menuMapper.updateById(menu);
        return MenuVO.from(requireMenu(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireMenu(id);
        Long childrenCount = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childrenCount != null && childrenCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
        // 清理角色-菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
    }

    private void applyRequest(SysMenu menu, MenuRequest request) {
        menu.setParentId(request.getParentId());
        menu.setName(request.getName());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setIcon(request.getIcon());
        menu.setType(request.getType());
        menu.setPerms(request.getPerms());
        menu.setSort(request.getSort() == null ? 0 : request.getSort());
        menu.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private void validateParent(Long parentId) {
        if (parentId != null && parentId != 0 && menuMapper.selectById(parentId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父菜单不存在: id=" + parentId);
        }
    }

    private List<MenuVO> buildTree(List<MenuVO> nodes) {
        Map<Long, MenuVO> byId = new HashMap<>();
        for (MenuVO node : nodes) {
            byId.put(node.getId(), node);
        }
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO node : nodes) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0) {
                roots.add(node);
            } else {
                MenuVO parent = byId.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    // 父节点被禁用/不存在：按根节点兜底，避免节点丢失
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜单不存在: id=" + id);
        }
        return menu;
    }
}
