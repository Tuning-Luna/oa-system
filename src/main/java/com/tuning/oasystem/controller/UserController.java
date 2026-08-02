package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.dto.AssignRoleRequest;
import com.tuning.oasystem.dto.UserQuery;
import com.tuning.oasystem.dto.UserUpdateRequest;
import com.tuning.oasystem.service.UserService;
import com.tuning.oasystem.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理接口
 */
@Tag(name = "用户管理", description = "用户分页查询 / 详情 / 修改 / 删除 / 分配角色")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户分页查询", description = "按用户名模糊、状态筛选")
    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping
    public Result<PageResult<UserVO>> page(UserQuery query) {
        return Result.success(userService.page(query));
    }

    @Operation(summary = "用户详情")
    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "修改用户", description = "可修改昵称/邮箱/手机号/头像/状态")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.success(userService.update(id, request));
    }

    @Operation(summary = "删除用户", description = "逻辑删除并清理用户-角色关联")
    @PreAuthorize("hasAuthority('system:user:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @Operation(summary = "给用户分配角色", description = "全量替换用户的角色")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest request) {
        userService.assignRoles(id, request.getRoleIds());
        return Result.success();
    }

    @Operation(summary = "用户当前角色 ID 列表", description = "供分配角色回显")
    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/{id}/roles")
    public Result<List<Long>> roleIds(@PathVariable Long id) {
        return Result.success(userService.getRoleIds(id));
    }
}
