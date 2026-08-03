/**
 * 按钮级权限指令
 *
 * `v-permission="'system:user:add'"`            单个权限，命中则保留
 * `v-permission="['a','b']"`                    数组，默认「任一」命中即保留
 * `v-permission:all="['a','b']"`                数组，「全部」命中才保留
 *
 * `v-role` 同构，基于用户角色编码判断：
 * `v-role="'admin'"` / `v-role="['admin','manager']"` / `v-role:all="['a','b']"`
 *
 * 依赖 useUserStore.permissions / roles（登录后由 /api/auth/me 加载，路由守卫已确保其先于页面渲染就绪）。
 */
import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

type PermissionValue = string | string[]

function checkAccess(required: PermissionValue, has: (key: string) => boolean, arg?: string): boolean {
  const keys = Array.isArray(required) ? required : [required]
  if (keys.length === 0) return true
  return arg === 'all' ? keys.every((k) => has(k)) : keys.some((k) => has(k))
}

function makeDirective<K extends 'permissions' | 'roles'>(
  source: K,
): Directive<HTMLElement, PermissionValue> {
  return {
    mounted(el, binding) {
      const userStore = useUserStore()
      const allowed = checkAccess(
        binding.value,
        (key: string) => userStore[source].includes(key),
        binding.arg,
      )
      if (!allowed) {
        el.parentNode?.removeChild(el)
      }
    },
  }
}

export const permission: Directive<HTMLElement, PermissionValue> = makeDirective('permissions')
export const role: Directive<HTMLElement, PermissionValue> = makeDirective('roles')
