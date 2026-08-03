/**
 * 角色管理接口
 * 契约见 frontend-plan/02-接口文档.md 第三、四章
 */
import { http } from '@/utils/request'
import type { RoleVO } from '@/types'

/** 全部启用角色（供「给用户分配角色」下拉使用） */
export function listAllRoles(): Promise<RoleVO[]> {
  return http.get<RoleVO[]>('/roles/all')
}
