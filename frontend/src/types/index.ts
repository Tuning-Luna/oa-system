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

/** 角色分页查询参数 */
export interface RoleQuery {
  pageNum: number
  pageSize: number
  name?: string
  status?: number
}

/** 角色新增/修改请求 */
export interface RoleRequest {
  name: string
  code: string
  description?: string
  status?: number
}

/** 给角色分配菜单请求（全量替换） */
export interface RoleMenuRequest {
  menuIds: number[]
}

/** 菜单新增/修改请求 */
export interface MenuRequest {
  parentId: number
  name: string
  path?: string
  component?: string
  icon?: string
  type: number
  perms?: string
  sort?: number
  status?: number
}

/** 菜单信息（MenuVO，含子菜单树） */
export interface MenuVO {
  id: number
  parentId: number
  name: string
  path: string | null
  component: string | null
  icon: string | null
  type: number
  perms: string | null
  sort: number
  status: number
  createTime: string
  updateTime: string
  children: MenuVO[]
}

/** 审批单分页查询参数（我的申请 / 待我审批共用） */
export interface ApprovalQuery {
  pageNum: number
  pageSize: number
  status?: number
}

/** 审批意见请求体（通过/拒绝共用，comment 可空） */
export interface ApprovalCommentRequest {
  comment?: string
}

/** 请假申请提交请求 */
export interface LeaveSubmitRequest {
  leaveType: number
  startDate: string
  endDate: string
  days: number
  reason: string
  approverId: number
}

/** 请假申请信息（LeaveVO） */
export interface LeaveVO {
  id: number
  userId: number
  applicantName: string
  leaveType: number
  startDate: string
  endDate: string
  days: number
  reason: string
  status: number
  statusDesc: string
  approverId: number
  approverName: string
  createTime: string
  updateTime: string
}

/** 审批记录（ApprovalRecordVO） */
export interface ApprovalRecordVO {
  id: number
  businessType: number
  businessId: number
  approverId: number
  approverName: string
  action: number
  actionDesc: string
  comment: string
  createTime: string
}
