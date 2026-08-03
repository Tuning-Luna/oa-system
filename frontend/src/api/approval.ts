/**
 * 审批模块接口
 * 契约见 frontend-plan/02-接口文档.md 第五、六、七章
 */
import { http } from '@/utils/request'
import type {
  ApprovalCommentRequest,
  ApprovalQuery,
  ApprovalRecordVO,
  LeaveSubmitRequest,
  LeaveVO,
  PageResult,
  ReimburseSubmitRequest,
  ReimburseVO,
} from '@/types'

// ==================== 请假审批 ====================

/** 提交请假申请 */
export function submitLeave(data: LeaveSubmitRequest): Promise<LeaveVO> {
  return http.post<LeaveVO>('/approvals/leave', data)
}

/** 我的请假申请（分页，可按状态筛选） */
export function myLeaves(params: ApprovalQuery): Promise<PageResult<LeaveVO>> {
  return http.get<PageResult<LeaveVO>>('/approvals/leave/my', params)
}

/** 待我审批列表（当前登录用户为审批人的待审批申请） */
export function pendingLeaves(params: ApprovalQuery): Promise<PageResult<LeaveVO>> {
  return http.get<PageResult<LeaveVO>>('/approvals/leave/pending', params)
}

/** 请假申请详情（申请人/审批人可查看） */
export function getLeave(id: number): Promise<LeaveVO> {
  return http.get<LeaveVO>(`/approvals/leave/${id}`)
}

/** 审批通过（comment 可选，传空对象 {}） */
export function approveLeave(id: number, data?: ApprovalCommentRequest): Promise<null> {
  return http.put<null>(`/approvals/leave/${id}/approve`, data ?? {})
}

/** 审批拒绝 */
export function rejectLeave(id: number, data?: ApprovalCommentRequest): Promise<null> {
  return http.put<null>(`/approvals/leave/${id}/reject`, data ?? {})
}

/** 撤回申请（仅申请人本人，草稿/待审批可撤回） */
export function cancelLeave(id: number): Promise<null> {
  return http.put<null>(`/approvals/leave/${id}/cancel`, {})
}

// ==================== 报销审批 ====================

/** 提交报销申请 */
export function submitReimburse(data: ReimburseSubmitRequest): Promise<ReimburseVO> {
  return http.post<ReimburseVO>('/approvals/reimburse', data)
}

/** 我的报销申请（分页，可按状态筛选） */
export function myReimburses(params: ApprovalQuery): Promise<PageResult<ReimburseVO>> {
  return http.get<PageResult<ReimburseVO>>('/approvals/reimburse/my', params)
}

/** 待我审批（报销）列表 */
export function pendingReimburses(params: ApprovalQuery): Promise<PageResult<ReimburseVO>> {
  return http.get<PageResult<ReimburseVO>>('/approvals/reimburse/pending', params)
}

/** 报销申请详情（申请人/审批人可查看） */
export function getReimburse(id: number): Promise<ReimburseVO> {
  return http.get<ReimburseVO>(`/approvals/reimburse/${id}`)
}

/** 审批通过（报销） */
export function approveReimburse(id: number, data?: ApprovalCommentRequest): Promise<null> {
  return http.put<null>(`/approvals/reimburse/${id}/approve`, data ?? {})
}

/** 审批拒绝（报销） */
export function rejectReimburse(id: number, data?: ApprovalCommentRequest): Promise<null> {
  return http.put<null>(`/approvals/reimburse/${id}/reject`, data ?? {})
}

/** 撤回申请（报销） */
export function cancelReimburse(id: number): Promise<null> {
  return http.put<null>(`/approvals/reimburse/${id}/cancel`, {})
}

// ==================== 审批记录（请假/报销共用） ====================

/** 审批记录查询：businessType 1 请假 / 2 报销 */
export function getApprovalRecords(businessType: number, businessId: number): Promise<ApprovalRecordVO[]> {
  return http.get<ApprovalRecordVO[]>('/approvals/records', { businessType, businessId })
}
