/**
 * 用户管理接口
 * 契约见 frontend-plan/02-接口文档.md 第二、三章
 */
import { http } from '@/utils/request'
import type { PageResult, UserQuery, UserUpdateRequest, UserVO } from '@/types'

/** 用户分页查询 */
export function pageUsers(params: UserQuery): Promise<PageResult<UserVO>> {
  return http.get<PageResult<UserVO>>('/users', params)
}

/** 用户详情 */
export function getUser(id: number): Promise<UserVO> {
  return http.get<UserVO>(`/users/${id}`)
}

/** 修改用户 */
export function updateUser(id: number, data: UserUpdateRequest): Promise<UserVO> {
  return http.put<UserVO>(`/users/${id}`, data)
}

/** 删除用户（逻辑删除） */
export function deleteUser(id: number): Promise<null> {
  return http.delete<null>(`/users/${id}`)
}

/** 给用户分配角色（全量替换） */
export function assignRoles(id: number, roleIds: number[]): Promise<null> {
  return http.put<null>(`/users/${id}/roles`, { roleIds })
}

/** 用户当前角色 ID 列表（分配角色回显） */
export function getUserRoleIds(id: number): Promise<number[]> {
  return http.get<number[]>(`/users/${id}/roles`)
}
