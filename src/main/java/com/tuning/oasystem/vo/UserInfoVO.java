package com.tuning.oasystem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 当前用户信息返回：用户基本信息 + 角色编码 + 权限标识集合
 */
@Data
@Schema(description = "当前用户信息（含角色与权限）")
public class UserInfoVO {

    @Schema(description = "用户基本信息")
    private UserVO user;

    @Schema(description = "角色编码列表", example = "[\"admin\"]")
    private List<String> roles;

    @Schema(description = "权限标识集合", example = "[\"system:user:list\"]")
    private Set<String> permissions;

    public static UserInfoVO of(UserVO user, List<String> roles, Set<String> permissions) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUser(user);
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }
}
