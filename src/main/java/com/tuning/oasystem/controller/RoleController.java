package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.dto.RoleMenuRequest;
import com.tuning.oasystem.dto.RoleQuery;
import com.tuning.oasystem.dto.RoleRequest;
import com.tuning.oasystem.service.RoleService;
import com.tuning.oasystem.vo.RoleVO;
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
 * 角色管理接口
 */
@Tag(name = "角色管理", description = "角色分页查询 / CRUD / 分配菜单")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色分页查询", description = "按角色名称模糊、状态筛选")
    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping
    public Result<PageResult<RoleVO>> page(RoleQuery query) {
        return Result.success(roleService.page(query));
    }

    @Operation(summary = "全部启用角色", description = "供「给用户分配角色」下拉选择")
    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/all")
    public Result<List<RoleVO>> listAll() {
        return Result.success(roleService.listAll());
    }

    @Operation(summary = "角色详情")
    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/{id}")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @Operation(summary = "新增角色", description = "角色编码全局唯一")
    @PreAuthorize("hasAuthority('system:role:add')")
    @PostMapping
    public Result<RoleVO> create(@Valid @RequestBody RoleRequest request) {
        return Result.success(roleService.create(request));
    }

    @Operation(summary = "修改角色")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @PutMapping("/{id}")
    public Result<RoleVO> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return Result.success(roleService.update(id, request));
    }

    @Operation(summary = "删除角色", description = "逻辑删除并清理角色-用户/角色-菜单关联")
    @PreAuthorize("hasAuthority('system:role:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "给角色分配菜单", description = "全量替换角色的菜单权限")
    @PreAuthorize("hasAuthority('system:role:assign')")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleMenuRequest request) {
        roleService.assignMenus(id, request.getMenuIds());
        return Result.success();
    }

    @Operation(summary = "角色当前菜单 ID 列表", description = "供分配菜单回显")
    @PreAuthorize("hasAuthority('system:role:assign')")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> roleMenuIds(@PathVariable Long id) {
        return Result.success(roleService.getMenuIds(id));
    }
}
