<template>
  <div class="file-upload">
    <el-upload
      :show-file-list="false"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :disabled="uploading"
      accept=".jpg,.jpeg,.png,.gif,.pdf,.doc,.docx,.xls,.xlsx,.txt,.csv"
    >
      <el-button type="primary" :loading="uploading">上传文件</el-button>
    </el-upload>
    <el-progress
      v-if="uploading"
      class="upload-progress"
      :percentage="uploadProgress"
      :stroke-width="6"
    />
    <span class="upload-tip">支持 jpg/jpeg/png/gif/pdf/doc/docx/xls/xlsx/txt/csv，最大 10MB</span>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { uploadFile } from '@/api/file'

const emit = defineEmits<{
  (e: 'success'): void
}>()

/** 与后端一致：后缀白名单 + 大小上限 10MB */
const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt', 'csv']
const MAX_SIZE = 10 * 1024 * 1024

const uploading = ref(false)
const uploadProgress = ref(0)

/** 前端预校验：与后端提示保持一致 */
function beforeUpload(file: File): boolean {
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    ElMessage.error('不支持的文件类型，仅支持 jpg/jpeg/png/gif/pdf/doc/docx/xls/xlsx/txt/csv')
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

async function handleUpload(options: UploadRequestOptions): Promise<void> {
  uploading.value = true
  uploadProgress.value = 0
  try {
    await uploadFile(options.file, (p) => {
      uploadProgress.value = p
    })
    ElMessage.success('上传成功')
    emit('success')
  } catch {
    // 后端校验失败提示由拦截器统一处理
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}
</script>

<style scoped lang="scss">
.file-upload {
  display: flex;
  align-items: center;
  gap: 12px;

  .upload-progress {
    width: 160px;
  }

  .upload-tip {
    font-size: 12px;
    color: #909399;
  }
}
</style>
