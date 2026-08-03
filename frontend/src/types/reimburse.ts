/**
 * 报销审批类型
 * 与后端契约见 frontend-plan/02-接口文档.md 第六章。
 */

/** 报销申请提交请求 */
export interface ReimburseSubmitRequest {
  amount: number
  category: string
  reason: string
  approverId: number
}

/** 报销申请信息（ReimburseVO） */
export interface ReimburseVO {
  id: number
  userId: number
  applicantName: string
  amount: number
  category: string
  reason: string
  status: number
  statusDesc: string
  approverId: number
  approverName: string
  createTime: string
  updateTime: string
}
