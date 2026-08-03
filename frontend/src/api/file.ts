/**
 * 文件管理接口
 * 契约见 frontend-plan/02-接口文档.md 第八章
 */
import { http, request } from '@/utils/request'
import type { FileQuery, FileVO, PageResult } from '@/types'

/** 文件分页列表（登录用户共享可见） */
export function pageFiles(params: FileQuery): Promise<PageResult<FileVO>> {
  return http.get<PageResult<FileVO>>('/files', params)
}

/** 删除文件（仅上传者或 admin） */
export function deleteFile(id: number): Promise<null> {
  return http.delete<null>(`/files/${id}`)
}

/** 上传文件（multipart 字段 file；onProgress 回传 0-100） */
export function uploadFile(file: File, onProgress?: (percent: number) => void): Promise<FileVO> {
  const formData = new FormData()
  formData.append('file', file)
  return request<FileVO>({
    url: '/files/upload',
    method: 'post',
    data: formData,
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100))
      }
    },
  })
}

/** 下载文件（blob）；FileVO.url 形如 /api/files/{id}/download，去重 /api 前缀避免与 baseURL 双拼 */
export function downloadFile(file: FileVO): Promise<Blob> {
  const url = file.url.startsWith('/api') ? file.url.slice(4) : file.url
  return http.download<Blob>(url)
}
