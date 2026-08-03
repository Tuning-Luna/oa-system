/**
 * 角色管理接口
 * 契约见 frontend-plan/02-接口文档.md 第三、四章
 */
import { http } from '@/utils/request'
import type { PageResult, RoleQuery, RoleRequest, RoleVO } from '@/types'

/** 角色分页查询 */
export function pageRoles(params: RoleQuery): Promise<PageResult<RoleVO>> {
  return http.get<PageResult<RoleVO>>('/roles', params)
}

/** 全部启用角色（供「给用户分配角色」下拉使用） */
export function listAllRoles(): Promise<RoleVO[]> {
  return http.get<RoleVO[]>('/roles/all')
}

/** 角色详情 */
export function getRole(id: number): Promise<RoleVO> {
  return http.get<RoleVO>(`/roles/${id}`)
}

/** 新增角色 */
export function createRole(data: RoleRequest): Promise<RoleVO> {
  return http.post<RoleVO>('/roles', data)
}

/** 修改角色 */
export function updateRole(id: number, data: RoleRequest): Promise<RoleVO> {
  return http.put<RoleVO>(`/roles/${id}`, data)
}

/** 删除角色（逻辑删除 + 清理关联） */
export function deleteRole(id: number): Promise<null> {
  return http.delete<null>(`/roles/${id}`)
}

/** 给角色分配菜单（全量替换） */
export function assignMenus(id: number, menuIds: number[]): Promise<null> {
  return http.put<null>(`/roles/${id}/menus`, { menuIds })
}

/** 角色当前菜单 ID 列表（分配菜单回显） */
export function getRoleMenuIds(id: number): Promise<number[]> {
  return http.get<number[]>(`/roles/${id}/menus`)
}
