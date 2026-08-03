/**
 * 系统通知类型
 * 与后端契约见 frontend-plan/02-接口文档.md 第九章。
 */

/** 通知分页查询参数 */
export interface MessageQuery {
  pageNum: number
  pageSize: number
  readFlag?: number
}

/** 系统通知（SysMessageVO） */
export interface SysMessageVO {
  id: number
  receiverId: number
  type: string
  title: string
  content: string
  readFlag: number
  createTime: string
}
