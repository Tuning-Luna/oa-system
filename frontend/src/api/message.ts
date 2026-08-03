/**
 * 系统通知接口
 * 契约见 frontend-plan/02-接口文档.md 第九章
 */
import { http } from '@/utils/request'
import type { MessageQuery, PageResult, SysMessageVO } from '@/types'

/** 我的通知分页查询（readFlag：0 未读 / 1 已读，可选） */
export function pageMessages(params: MessageQuery): Promise<PageResult<SysMessageVO>> {
  return http.get<PageResult<SysMessageVO>>('/messages', params)
}

/** 未读通知数量（顶栏铃铛角标轮询） */
export function unreadMessageCount(): Promise<number> {
  return http.get<number>('/messages/unread-count')
}

/** 标记单条已读 */
export function markMessageRead(id: number): Promise<null> {
  return http.put<null>(`/messages/${id}/read`)
}

/** 全部标记已读 */
export function markAllMessagesRead(): Promise<null> {
  return http.put<null>('/messages/read-all')
}
