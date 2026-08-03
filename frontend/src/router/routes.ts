/**
 * 动态路由表（作为 Layout 的子路由，path 为相对路径）
 *
 * - businessRoutes：业务路由，登录即可见（审批 / 通知 / 文件）
 * - sysRoutes：系统管理路由，按 meta.permissions 过滤
 *
 * 各页面组件在后续阶段逐步实现，此处先指向占位视图。
 */
import type { RouteRecordRaw } from 'vue-router'

/**
 * 首页（默认落地页）：作为 Layout 的常量子路由。
 * 保证登录/未登录时 '/dashboard' 始终可解析——避免 Layout 的 redirect 指向
 * 尚未注入的动态路由而落到 404 兜底。
 */
export const dashboardRoute: RouteRecordRaw = {
  path: 'dashboard',
  name: 'Dashboard',
  component: () => import('@/views/dashboard/index.vue'),
  meta: { title: '首页', icon: 'home' },
}

/** 业务路由（登录即可见，无权限要求；首页除外，见 dashboardRoute） */
export const businessRoutes: RouteRecordRaw[] = [
  {
    path: 'approval',
    name: 'Approval',
    redirect: '/approval/leave/my',
    meta: { title: '审批管理', icon: 'document' },
    children: [
      {
        path: 'leave/my',
        name: 'LeaveMy',
        component: () => import('@/views/approval/leave/my/index.vue'),
        meta: { title: '我的请假', icon: 'document' },
      },
      {
        path: 'leave/create',
        name: 'LeaveCreate',
        component: () => import('@/views/approval/leave/create/index.vue'),
        meta: { title: '提交请假', icon: 'document' },
      },
      {
        path: 'leave/pending',
        name: 'LeavePending',
        component: () => import('@/views/approval/leave/pending/index.vue'),
        meta: { title: '请假审批', icon: 'document' },
      },
      {
        path: 'reimburse/my',
        name: 'ReimburseMy',
        component: () => import('@/views/approval/reimburse/my/index.vue'),
        meta: { title: '我的报销', icon: 'document' },
      },
      {
        path: 'reimburse/create',
        name: 'ReimburseCreate',
        component: () => import('@/views/approval/reimburse/create/index.vue'),
        meta: { title: '提交报销', icon: 'document' },
      },
      {
        path: 'reimburse/pending',
        name: 'ReimbursePending',
        component: () => import('@/views/approval/reimburse/pending/index.vue'),
        meta: { title: '报销审批', icon: 'document' },
      },
    ],
  },
  {
    path: 'message',
    name: 'Message',
    component: () => import('@/views/message/index.vue'),
    meta: { title: '通知中心', icon: 'bell' },
  },
  {
    path: 'file',
    name: 'File',
    component: () => import('@/views/file/index.vue'),
    meta: { title: '文件中心', icon: 'folder' },
  },
]

/** 系统管理路由（需权限，按 meta.permissions 过滤） */
export const sysRoutes: RouteRecordRaw[] = [
  {
    path: 'system',
    name: 'System',
    redirect: '/system/user',
    meta: { title: '系统管理', icon: 'setting' },
    children: [
      {
        path: 'user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'user', permissions: ['system:user:list'] },
      },
      {
        path: 'role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'role', permissions: ['system:role:list'] },
      },
      {
        path: 'menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'menu', permissions: ['system:menu:list'] },
      },
    ],
  },
]
