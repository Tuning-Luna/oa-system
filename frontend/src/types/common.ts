/**
 * 通用类型：统一响应与分页
 * 与后端统一响应契约保持一致（见 frontend-plan/02-接口文档.md 0.x 章）。
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
