package com.tuning.oasystem.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.mapper.SysUserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户 ID → 显示名（优先昵称，缺省用登录名）批量解析，供审批等需要展示姓名的场景复用。
 */
@Component
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class UserNameResolver {

    private final SysUserMapper sysUserMapper;

    public UserNameResolver(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    /** 批量解析（自动去重并忽略 null），返回 userId → 显示名 */
    public Map<Long, String> namesOf(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        java.util.List<Long> distinct = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, distinct))
                .stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername(),
                        (a, b) -> a));
    }

    /** 解析单个用户显示名；用户不存在返回 null */
    public String nameOf(Long userId) {
        Map<Long, String> map = namesOf(java.util.List.of(userId));
        return map.get(userId);
    }
}
