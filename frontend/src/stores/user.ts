/**
 * 用户状态（登录态）
 * - token：localStorage 持久化（key: oa_token）
 * - userInfo：当前用户信息（含角色/权限），会话内内存缓存
 */
import { defineStore } from 'pinia'
import { getMe, login as apiLogin, logout as apiLogout } from '@/api/auth'
import { TOKEN_KEY } from '@/utils/request'
import type { LoginRequest, UserInfoVO } from '@/types'

interface UserState {
  token: string
  userInfo: UserInfoVO | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: null,
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    /** 显示名：昵称优先，缺省用登录名 */
    nickname: (state) => state.userInfo?.user?.nickname || state.userInfo?.user?.username || '',
    roles: (state) => state.userInfo?.roles ?? [],
    permissions: (state) => state.userInfo?.permissions ?? [],
  },

  actions: {
    /** 登录：落 token 并持久化 */
    async login(payload: LoginRequest): Promise<void> {
      const data = await apiLogin(payload)
      this.token = data.token
      localStorage.setItem(TOKEN_KEY, data.token)
    },

    /** 拉取当前用户信息（含角色/权限），路由守卫与刷新时调用 */
    async fetchMe(): Promise<UserInfoVO> {
      const data = await getMe()
      this.userInfo = data
      return data
    },

    /** 退出登录：通知后端删登录态缓存 + 本地清理（接口失败不阻断清理） */
    async logout(): Promise<void> {
      try {
        await apiLogout()
      } catch {
        // 忽略：本地清理照常执行
      } finally {
        this.reset()
      }
    },

    /** 本地清理登录态 */
    reset(): void {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
    },
  },
})
