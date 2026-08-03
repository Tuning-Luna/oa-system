/**
 * 认证模块类型
 * 与后端契约见 frontend-plan/02-接口文档.md 第一章。
 */
import type { UserVO } from './user'

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
