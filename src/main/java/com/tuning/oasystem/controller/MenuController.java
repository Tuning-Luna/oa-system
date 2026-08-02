package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.dto.MenuRequest;
import com.tuning.oasystem.service.MenuService;
import com.tuning.oasystem.vo.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理接口
 */
@Tag(name = "菜单管理", description = "菜单树 / CRUD")
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "菜单树", description = "全部启用菜单的树形结构（目录/菜单/按钮）")
    @PreAuthorize("hasAuthority('system:menu:list')")
    @GetMapping("/tree")
    public Result<List<MenuVO>> tree() {
        return Result.success(menuService.tree());
    }

    @Operation(summary = "菜单详情")
    @PreAuthorize("hasAuthority('system:menu:list')")
    @GetMapping("/{id}")
    public Result<MenuVO> detail(@PathVariable Long id) {
        return Result.success(menuService.getById(id));
    }

    @Operation(summary = "新增菜单", description = "type 1目录 2菜单 3按钮；按钮需填 perms")
    @PreAuthorize("hasAuthority('system:menu:add')")
    @PostMapping
    public Result<MenuVO> create(@Valid @RequestBody MenuRequest request) {
        return Result.success(menuService.create(request));
    }

    @Operation(summary = "修改菜单")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @PutMapping("/{id}")
    public Result<MenuVO> update(@PathVariable Long id, @Valid @RequestBody MenuRequest request) {
        return Result.success(menuService.update(id, request));
    }

    @Operation(summary = "删除菜单", description = "存在子菜单时拒绝删除；并清理角色-菜单关联")
    @PreAuthorize("hasAuthority('system:menu:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.success();
    }
}
