/**
 * 请假审批类型
 * 与后端契约见 frontend-plan/02-接口文档.md 第五章。
 */

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
