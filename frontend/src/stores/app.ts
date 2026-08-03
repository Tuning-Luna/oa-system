/**
 * 应用级 UI 状态
 */
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    /** 侧边栏是否折叠 */
    sidebarCollapsed: false,
  }),

  actions: {
    toggleSidebar(): void {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
  },
})
