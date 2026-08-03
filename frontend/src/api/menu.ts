/**
 * 菜单管理接口
 * 契约见 frontend-plan/02-接口文档.md 第四、五章
 */
import { http } from '@/utils/request'
import type { MenuRequest, MenuVO } from '@/types'

/** 菜单树（全部启用菜单，含子菜单） */
export function getMenuTree(): Promise<MenuVO[]> {
  return http.get<MenuVO[]>('/menus/tree')
}

/** 菜单详情 */
export function getMenu(id: number): Promise<MenuVO> {
  return http.get<MenuVO>(`/menus/${id}`)
}

/** 新增菜单 */
export function createMenu(data: MenuRequest): Promise<MenuVO> {
  return http.post<MenuVO>('/menus', data)
}

/** 修改菜单 */
export function updateMenu(id: number, data: MenuRequest): Promise<MenuVO> {
  return http.put<MenuVO>(`/menus/${id}`, data)
}

/** 删除菜单（存在子菜单时后端拒绝） */
export function deleteMenu(id: number): Promise<null> {
  return http.delete<null>(`/menus/${id}`)
}
