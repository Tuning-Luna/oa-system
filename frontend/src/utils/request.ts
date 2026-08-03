/**
 * Axios 统一封装
 *
 * - 请求拦截：自动注入 `Authorization: Bearer <token>`
 * - 响应拦截：统一解析后端 `{ code, message, data }`；
 *   - code === 200 直接返回 data（调用方拿到的是解包后的业务数据）
 *   - code !== 200 用 ElMessage 提示并 reject
 *   - HTTP 401 清除登录态并跳转登录页
 *   - responseType === 'blob'（文件下载）原样返回，不做解包
 */
import axios from 'axios'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'

/** token 在 localStorage 中的存储 key */
export const TOKEN_KEY = 'oa_token'

const TOKEN_PREFIX = 'Bearer '

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

// ---------- 请求拦截器：注入 token ----------
service.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = TOKEN_PREFIX + token
  }
  return config
})

// ---------- 响应拦截器：统一解包与错误处理 ----------
service.interceptors.response.use(
  (response) => {
    // 文件下载：返回 Blob，不做解包
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const envelope = response.data as Result
    if (envelope.code === 200) {
      return envelope.data
    }
    if (envelope.code === 401) {
      handleUnauthorized()
    }
    ElMessage.error(envelope.message || '请求失败')
    return Promise.reject(new Error(envelope.message || '请求失败'))
  },
  (error: AxiosError) => {
    const status = error.response?.status
    let message = '网络异常，请稍后重试'
    if (error.response) {
      const data = error.response.data as { code?: number; message?: string } | undefined
      if (data?.message) {
        message = data.message
      } else if (status === 401) {
        message = '未认证或登录已过期'
      } else if (status === 403) {
        message = '无访问权限'
      } else if (status === 404) {
        message = '资源不存在'
      } else if (status === 500) {
        message = '系统内部错误'
      }
    }
    if (status === 401) {
      handleUnauthorized()
    }
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

/** 登录态失效：清 token 并跳登录页（避免在登录页自身重复跳转） */
function handleUnauthorized(): void {
  localStorage.removeItem(TOKEN_KEY)
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

/**
 * 类型化请求：响应拦截器已把 `{code,message,data}` 解包为 data，
 * 因此 `request<T>` 直接返回业务数据 T。
 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  // 响应拦截器已把 AxiosResponse 解包为业务数据，运行时实际 resolve 的是 T
  return service.request(config) as Promise<T>
}

/** 便捷方法对象 */
export const http = {
  get<T = unknown>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request<T>({ url, method: 'get', params })
  },
  post<T = unknown>(url: string, data?: unknown): Promise<T> {
    return request<T>({ url, method: 'post', data })
  },
  put<T = unknown>(url: string, data?: unknown): Promise<T> {
    return request<T>({ url, method: 'put', data })
  },
  delete<T = unknown>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request<T>({ url, method: 'delete', params })
  },
  /** 文件下载：返回 Blob */
  download<T = Blob>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request<T>({ url, method: 'get', params, responseType: 'blob' })
  },
}

export default service
