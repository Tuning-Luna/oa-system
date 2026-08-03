/**
 * 权限/路由状态
 * - 根据当前用户 permissions 过滤系统管理路由（业务路由登录即可见）
 * - 生成侧边栏菜单数据（menuRoutes）
 * - 维护 isRoutesReady 标记，避免重复注入动态路由
 */
import { defineStore } from 'pinia'
import type { RouteRecordRaw } from 'vue-router'
import { businessRoutes, dashboardRoute, sysRoutes } from '@/router/routes'

/** 侧边栏菜单节点（与 RouteRecordRaw 解耦，避免联合类型在模板中的推断问题） */
export interface MenuRoute {
  path: string
  meta?: {
    title?: string
    icon?: string
  }
  children?: MenuRoute[]
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    /** 过滤后用于侧边栏菜单的路由表 */
    menuRoutes: [] as MenuRoute[],
    /** 动态路由是否已注入 */
    isRoutesReady: false,
  }),

  actions: {
    /**
     * 生成可访问路由
     * 首页为常量子路由（不参与动态注入），这里把它补进菜单首位
     * @returns 需要 addRoute 注入的 RouteRecordRaw 列表
     */
    generateRoutes(permissions: string[]): RouteRecordRaw[] {
      const accessible = filterRoutes(sysRoutes, permissions)
      const toAdd = [...businessRoutes, ...accessible]
      this.menuRoutes = [...toMenuTree([dashboardRoute]), ...toMenuTree(toAdd)]
      return toAdd
    },

    /** 清空路由状态（登出/路由重建时调用） */
    resetRoutes(): void {
      this.menuRoutes = []
      this.isRoutesReady = false
    },

    setReady(ready: boolean): void {
      this.isRoutesReady = ready
    },
  },
})

/** RouteRecordRaw → MenuRoute（仅保留菜单所需的 path/title/icon/children） */
function toMenuTree(routes: RouteRecordRaw[]): MenuRoute[] {
  return routes.map((route) => {
    const meta = route.meta as { title?: string; icon?: string } | undefined
    return {
      path: route.path,
      meta: { title: meta?.title, icon: meta?.icon },
      children: route.children ? toMenuTree(route.children) : undefined,
    }
  })
}

/**
 * 递归过滤路由：
 * - 路由 meta.permissions 存在且用户一个都不满足 → 剔除
 * - 目录（有 children）：子项全部被剔除 → 整组剔除；否则保留过滤后的子项，并把 redirect 指向首个可见子项
 * - 无 permissions 约束 → 保留
 */
function filterRoutes(routes: RouteRecordRaw[], permissions: string[]): RouteRecordRaw[] {
  const result: RouteRecordRaw[] = []
  for (const route of routes) {
    const need = route.meta?.permissions as string[] | undefined
    if (need && need.length > 0 && !need.some((p) => permissions.includes(p))) {
      continue
    }
    if (route.children && route.children.length > 0) {
      const children = filterRoutes(route.children, permissions)
      if (children.length === 0) {
        continue
      }
      result.push({
        ...route,
        // 目录默认重定向到首个可见子项，避免指向无权限页
        redirect: `/${route.path}/${children[0].path}`,
        children,
      } as RouteRecordRaw)
    } else {
      result.push(route)
    }
  }
  return result
}
