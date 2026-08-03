/**
 * 类型入口（barrel）：按业务域拆分到各子文件后统一重新导出。
 * 各 api/页面继续从 `@/types` 导入，无需改动调用方。
 *
 * - common.ts    统一响应/分页
 * - auth.ts      认证模块
 * - user.ts      用户管理
 * - role.ts      角色管理
 * - menu.ts      菜单管理
 * - approval.ts  审批通用（请假/报销共用）
 * - leave.ts     请假审批
 * - reimburse.ts 报销审批
 * - message.ts   系统通知
 * - file.ts      文件管理
 *
 * 说明：后端 Long 序列化为 JSON number，TS 统一用 number。
 */
export * from './common'
export * from './auth'
export * from './user'
export * from './role'
export * from './menu'
export * from './approval'
export * from './leave'
export * from './reimburse'
export * from './message'
export * from './file'
