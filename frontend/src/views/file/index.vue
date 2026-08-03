<template>
  <div class="file-page">
    <!-- 搜索 + 上传 -->
    <el-card class="search-card" shadow="never">
      <div class="search-row">
        <el-form inline @submit.prevent>
          <el-form-item label="文件名">
            <el-input
              v-model="query.name"
              placeholder="文件名模糊查询"
              clearable
              style="width: 220px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="upload-area">
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
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="大小" width="110" align="right">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tooltip :content="row.contentType" placement="top">
              <el-tag :type="fileTypeTag(row.originalName)">{{ fileTypeLabel(row.originalName) }}</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" min-width="100" />
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
            <el-button v-if="canDelete(row)" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="fetchList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { deleteFile, downloadFile, pageFiles, uploadFile } from '@/api/file'
import { useUserStore } from '@/stores/user'
import type { FileVO } from '@/types'
import { fileTypeLabel, fileTypeTag } from '@/utils/dict'
import { formatSize } from '@/utils/format'

const userStore = useUserStore()

/** 与后端一致：后缀白名单 + 大小上限 10MB */
const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt', 'csv']
const MAX_SIZE = 10 * 1024 * 1024

// ==================== 列表 ====================
const loading = ref(false)
const list = ref<FileVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
})

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const data = await pageFiles({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name || undefined,
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  fetchList()
}

function handleReset(): void {
  query.name = ''
  handleSearch()
}

function handleSizeChange(): void {
  query.pageNum = 1
  fetchList()
}

// ==================== 上传 ====================
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
    query.pageNum = 1
    fetchList()
  } catch {
    // 后端校验失败提示由拦截器统一处理
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

// ==================== 下载 ====================
async function handleDownload(row: FileVO): Promise<void> {
  try {
    const blob = await downloadFile(row)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.originalName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    // 下载失败提示由拦截器统一处理
  }
}

// ==================== 删除 ====================
/** 删除按钮：仅上传者本人 或 admin 角色可见 */
function canDelete(row: FileVO): boolean {
  return userStore.userInfo?.user?.id === row.uploaderId || userStore.roles.includes('admin')
}

async function handleDelete(row: FileVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除文件「${row.originalName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return // 用户取消
  }
  try {
    await deleteFile(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // 非上传者/非 admin 被拒提示由拦截器统一处理
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.file-page {
  .search-card {
    margin-bottom: 16px;
  }

  .search-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
  }

  .upload-area {
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

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
