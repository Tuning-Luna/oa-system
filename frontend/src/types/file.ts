/**
 * 文件管理类型
 * 与后端契约见 frontend-plan/02-接口文档.md 第八章。
 */

/** 文件分页查询参数 */
export interface FileQuery {
  pageNum: number
  pageSize: number
  name?: string
  contentType?: string
}

/** 文件信息（FileVO） */
export interface FileVO {
  id: number
  originalName: string
  size: number
  contentType: string
  url: string
  uploaderId: number
  uploaderName: string
  createTime: string
}
