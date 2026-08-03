/**
 * 按钮级权限指令：`v-permission="'system:user:add'"` 或 `v-permission="['a','b']"`
 *
 * 用户命中任一权限标识则保留元素，否则从 DOM 移除。
 * 依赖 useUserStore.permissions（登录后由 /api/auth/me 加载）。
 */
import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const required = Array.isArray(binding.value) ? binding.value : [binding.value]
    const allowed = required.some((p) => userStore.permissions.includes(p))
    if (!allowed) {
      el.parentNode?.removeChild(el)
    }
  },
}
