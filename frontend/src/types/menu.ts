/**
 * 菜单管理类型
 * 与后端契约见 frontend-plan/02-接口文档.md 第四章。
 */

/** 菜单新增/修改请求 */
export interface MenuRequest {
  parentId: number
  name: string
  path?: string
  component?: string
  icon?: string
  type: number
  perms?: string
  sort?: number
  status?: number
}

/** 菜单信息（MenuVO，含子菜单树） */
export interface MenuVO {
  id: number
  parentId: number
  name: string
  path: string | null
  component: string | null
  icon: string | null
  type: number
  perms: string | null
  sort: number
  status: number
  createTime: string
  updateTime: string
  children: MenuVO[]
}
