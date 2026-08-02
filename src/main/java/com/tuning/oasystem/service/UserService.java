package com.tuning.oasystem.service;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.dto.UserQuery;
import com.tuning.oasystem.dto.UserUpdateRequest;
import com.tuning.oasystem.vo.UserVO;

import java.util.List;

/**
 * 用户管理服务
 */
public interface UserService {

    /** 分页查询（用户名模糊 + 状态筛选） */
    PageResult<UserVO> page(UserQuery query);

    /** 用户详情 */
    UserVO getById(Long id);

    /** 修改用户资料/状态 */
    UserVO update(Long id, UserUpdateRequest request);

    /** 删除用户（逻辑删除） */
    void delete(Long id);

    /** 给用户分配角色（全量替换） */
    void assignRoles(Long userId, List<Long> roleIds);

    /** 用户当前角色 ID 列表 */
    List<Long> getRoleIds(Long userId);
}
