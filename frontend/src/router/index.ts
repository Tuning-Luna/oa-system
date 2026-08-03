/**
 * 路由配置 + 全局登录/权限守卫
 *
 * 动态路由方案：
 * 1. 常量路由：登录/注册/404 + Layout 壳（无子路由）
 * 2. 登录后拉取 /api/auth/me → 根据 permissions 过滤出可访问路由（业务路由全量 +
 *    系统管理按权限过滤）→ addRoute 注入为 Layout 子路由 → 重新导航避免刷新 404
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { dashboardRoute } from './routes'

const Layout = () => import('@/layout/index.vue')

/** 常量路由（无需权限） */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', public: true },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', public: true },
  },
  {
    // Layout 壳路由：不设 meta.title，避免面包屑出现重复的「首页」
    // children 仅含首页（常量子路由）；其余业务/系统动态路由通过 addRoute('Layout', route) 注入
    path: '/',
    name: 'Layout',
    component: Layout,
    redirect: '/dashboard',
    children: [dashboardRoute],
  },
  {
    // 兜底：未匹配路由 → 404
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
})

/** 无需登录即可访问的白名单 */
const WHITE_LIST = ['/login', '/register', '/404']

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  // 已登录
  if (userStore.isLoggedIn) {
    // 已登录访问登录页 → 回首页
    if (to.path === '/login') {
      return { path: '/' }
    }
    // 有 token 但无用户信息（刷新/首次）→ 拉取 /api/auth/me
    if (!userStore.userInfo) {
      try {
        await userStore.fetchMe()
      } catch {
        userStore.reset()
        permissionStore.resetRoutes()
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
    // 动态路由尚未注入 → 过滤并注入，然后重新导航（防刷新 404）
    if (!permissionStore.isRoutesReady) {
      const routes = permissionStore.generateRoutes(userStore.permissions)
      for (const route of routes) {
        router.addRoute('Layout', route)
      }
      permissionStore.setReady(true)
      return { ...to, replace: true }
    }
    return true
  }

  // 未登录
  if (WHITE_LIST.includes(to.path)) {
    return true
  }
  return { path: '/login', query: { redirect: to.fullPath } }
})

export default router
