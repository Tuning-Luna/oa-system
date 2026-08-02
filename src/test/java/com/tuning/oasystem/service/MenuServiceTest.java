package com.tuning.oasystem.service;

import com.tuning.oasystem.dto.MenuRequest;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.SysMenuMapper;
import com.tuning.oasystem.vo.MenuVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜单服务测试（需 MySQL 运行）：树形组装、父菜单校验、删除子菜单拦截、逻辑删除。
 */
@SpringBootTest
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
class MenuServiceTest {

    @Autowired
    private MenuService menuService;

    @Autowired
    private SysMenuMapper menuMapper;

    private final List<Long> createdMenuIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        // 先删子再删父，避免“存在子菜单无法删除”拦截
        Collections.reverse(createdMenuIds);
        for (Long id : createdMenuIds) {
            try {
                menuService.delete(id);
            } catch (BusinessException ignore) {
                // 测试内已删除
            }
        }
        createdMenuIds.clear();
    }

    private String uniqueName(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private MenuRequest request(Long parentId, String name, Integer type, String perms) {
        MenuRequest r = new MenuRequest();
        r.setParentId(parentId);
        r.setName(name);
        r.setType(type);
        r.setPerms(perms);
        r.setSort(1);
        r.setStatus(1);
        return r;
    }

    @Test
    void createShouldPersistAndReturn() {
        MenuVO vo = menuService.create(request(0L, uniqueName("menu"), 2, null));
        createdMenuIds.add(vo.getId());

        assertNotNull(vo.getId());
        assertEquals(2, vo.getType());
        MenuVO db = menuService.getById(vo.getId());
        assertNotNull(db);
        assertEquals(vo.getName(), db.getName());
    }

    @Test
    void treeShouldAssembleParentChildren() {
        String prefix = uniqueName("tree");
        MenuVO dir = menuService.create(request(0L, prefix + "目录", 1, null));
        MenuVO menu = menuService.create(request(dir.getId(), prefix + "菜单", 2, null));
        MenuVO btn = menuService.create(request(menu.getId(), prefix + "按钮", 3, prefix + ":action"));
        createdMenuIds.addAll(List.of(dir.getId(), menu.getId(), btn.getId()));

        List<MenuVO> tree = menuService.tree();
        MenuVO dirNode = findNode(tree, dir.getId());
        assertNotNull(dirNode, "目录应出现在树中");
        assertEquals(1, dirNode.getChildren().size());
        assertEquals(menu.getId(), dirNode.getChildren().get(0).getId());
        assertEquals(btn.getId(), dirNode.getChildren().get(0).getChildren().get(0).getId());
    }

    @Test
    void createWithInvalidParentShouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> menuService.create(request(999999L, uniqueName("bad"), 2, null)));
        assertTrue(ex.getMessage().contains("父菜单不存在"));
    }

    @Test
    void deleteMenuWithChildrenShouldThrow() {
        MenuVO dir = menuService.create(request(0L, uniqueName("dir"), 1, null));
        MenuVO child = menuService.create(request(dir.getId(), uniqueName("child"), 2, null));
        createdMenuIds.addAll(List.of(dir.getId(), child.getId()));

        assertThrows(BusinessException.class, () -> menuService.delete(dir.getId()),
                "存在子菜单时父菜单不可删除");

        // 先删子菜单成功
        menuService.delete(child.getId());
        createdMenuIds.remove(child.getId());
        assertNull(menuMapper.selectById(child.getId()), "子菜单逻辑删除后查不到");
    }

    private MenuVO findNode(List<MenuVO> nodes, Long id) {
        for (MenuVO node : nodes) {
            if (node.getId().equals(id)) {
                return node;
            }
            MenuVO found = findNode(node.getChildren(), id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
