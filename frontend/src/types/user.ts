/**
 * 用户管理类型
 * 与后端契约见 frontend-plan/02-接口文档.md 第二章。
 */

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
