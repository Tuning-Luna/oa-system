/**
 * 表单校验规则（与后端 DTO 校验保持一致，见 frontend-plan/02-接口文档.md）
 * 注册页与用户管理新增弹窗共用，避免重复定义。
 */
import type { FormItemRule } from 'element-plus'

/** 可选字段的格式校验：值为空则通过 */
export function optionalPattern(pattern: RegExp, message: string) {
  return {
    validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
      if (!value) return callback()
      return pattern.test(value) ? callback() : callback(new Error(message))
    },
    trigger: 'blur' as const,
  }
}

/** 用户名：3-20 位字母/数字/下划线 */
export const usernameRules: FormItemRule[] = [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { min: 3, max: 20, message: '用户名长度需在 3-20 之间', trigger: 'blur' },
  { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字、下划线', trigger: 'blur' },
]

/** 密码：8-20 位且同时包含字母和数字 */
export const passwordRules: FormItemRule[] = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 8, max: 20, message: '密码长度需在 8-20 之间', trigger: 'blur' },
  { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字', trigger: 'blur' },
]

/** 昵称：可选，最长 50 */
export const nicknameRules: FormItemRule[] = [
  { max: 50, message: '昵称最长 50 个字符', trigger: 'blur' },
]

/** 邮箱：可选 */
export const emailRules: FormItemRule[] = [
  optionalPattern(/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/, '邮箱格式不正确'),
]

/** 手机号：可选 */
export const phoneRules: FormItemRule[] = [
  optionalPattern(/^1[3-9]\d{9}$/, '手机号格式不正确'),
]
