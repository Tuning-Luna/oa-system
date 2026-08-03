/**
 * 角色管理类型
 * 与后端契约见 frontend-plan/02-接口文档.md 第三章。
 */

/** 角色信息（RoleVO） */
export interface RoleVO {
  id: number
  name: string
  code: string
  description: string | null
  status: number
  createTime: string
  updateTime: string
}

/** 角色分页查询参数 */
export interface RoleQuery {
  pageNum: number
  pageSize: number
  name?: string
  status?: number
}

/** 角色新增/修改请求 */
export interface RoleRequest {
  name: string
  code: string
  description?: string
  status?: number
}

/** 给角色分配菜单请求（全量替换） */
export interface RoleMenuRequest {
  menuIds: number[]
}
