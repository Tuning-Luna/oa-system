/**
 * 全局类型定义
 * 与后端接口契约保持一致（见 frontend-plan/02-接口文档.md）。
 * 后端 Long 序列化为 JSON number，TS 统一用 number。
 */

/** 统一返回结果 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页返回对象 */
export interface PageResult<T = unknown> {
  total: number
  records: T[]
  pageNum: number
  pageSize: number
}

/** 用户信息（UserVO，不回传密码） */
export interface UserVO {
  id: number
  username: string
  nickname: string | null
  email: string | null
  phone: string | null
  avatar: string | null
  status: number
  createTime: string
  updateTime: string
}

/** 当前用户信息（含角色编码与权限标识） */
export interface UserInfoVO {
  user: UserVO
  roles: string[]
  permissions: string[]
}

/** 登录响应 */
export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: UserVO
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
}

/** 用户分页查询参数 */
export interface UserQuery {
  pageNum: number
  pageSize: number
  username?: string
  status?: number
}

/** 修改用户请求 */
export interface UserUpdateRequest {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
}

/** 给用户分配角色请求（全量替换） */
export interface AssignRoleRequest {
  roleIds: number[]
}

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
