/**
 * 审批模块通用类型（请假/报销共用）
 * 与后端契约见 frontend-plan/02-接口文档.md 第五、六、七章。
 */

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
