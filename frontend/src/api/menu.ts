/**
 * 菜单管理接口
 * 契约见 frontend-plan/02-接口文档.md 第四、五章
 */
import { http } from '@/utils/request'
import type { MenuVO } from '@/types'

/** 菜单树（全部启用菜单，含子菜单） */
export function getMenuTree(): Promise<MenuVO[]> {
  return http.get<MenuVO[]>('/menus/tree')
}
