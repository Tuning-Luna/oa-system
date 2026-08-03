/**
 * 系统通知状态
 * 顶栏铃铛角标（layout 轮询写入）、通知列表页标记已读后刷新，共用同一份未读数。
 */
import { defineStore } from 'pinia'
import { unreadMessageCount } from '@/api/message'

export const useMessageStore = defineStore('message', {
  state: () => ({
    /** 未读通知数量（0 时角标隐藏） */
    unreadCount: 0,
  }),

  actions: {
    /** 拉取未读数量（轮询/操作后刷新共用） */
    async fetchUnreadCount(): Promise<void> {
      try {
        this.unreadCount = await unreadMessageCount()
      } catch {
        // 轮询失败静默：401 已由拦截器统一跳登录；偶发 5xx 仅本次不更新
      }
    },

    setUnreadCount(count: number): void {
      this.unreadCount = count
    },
  },
})
