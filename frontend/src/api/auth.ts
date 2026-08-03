/**
 * 认证模块接口
 * 契约见 frontend-plan/02-接口文档.md 第一、二章
 */
import { http } from '@/utils/request'
import type { LoginRequest, LoginResponse, RegisterRequest, UserInfoVO, UserVO } from '@/types'

/** 注册（公开接口；注册后需管理员分配角色） */
export function register(data: RegisterRequest): Promise<UserVO> {
  return http.post<UserVO>('/auth/register', data)
}

/** 登录 */
export function login(data: LoginRequest): Promise<LoginResponse> {
  return http.post<LoginResponse>('/auth/login', data)
}

/** 登出（删除 Redis 登录态缓存） */
export function logout(): Promise<null> {
  return http.post<null>('/auth/logout')
}

/** 当前用户信息（含角色编码与权限标识） */
export function getMe(): Promise<UserInfoVO> {
  return http.get<UserInfoVO>('/auth/me')
}
