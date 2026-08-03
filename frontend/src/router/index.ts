/**
 * 路由配置 + 全局登录守卫
 *
 * F1 阶段：仅常量路由（登录/注册/404/布局占位）。
 * 动态路由注入在 F2 阶段实现（业务路由 + 按权限过滤的系统管理路由）。
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

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
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页' },
      },
    ],
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
        // 用户信息拉取失败（token 失效等）→ 清登录态回登录页
        userStore.reset()
        return { path: '/login', query: { redirect: to.fullPath } }
      }
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
