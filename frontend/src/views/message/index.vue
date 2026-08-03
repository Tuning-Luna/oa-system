<template>
  <div class="message-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="radioFlag" @change="handleSearch">
          <el-radio-button :value="-1">全部</el-radio-button>
          <el-radio-button :value="0">未读</el-radio-button>
          <el-radio-button :value="1">已读</el-radio-button>
        </el-radio-group>
        <el-button type="primary" :disabled="hasUnread === false" @click="handleReadAll">
          全部已读
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe @row-click="handleRowClick">
        <el-table-column width="50" align="center">
          <template #default="{ row }">
            <span v-if="row.readFlag === 0" class="unread-dot" />
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <span :class="{ 'unread-title': row.readFlag === 0 }">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="messageTypeTag(row.type)" size="small">{{ messageTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { markAllMessagesRead, markMessageRead, pageMessages } from '@/api/message'
import { useMessageStore } from '@/stores/message'
import type { SysMessageVO } from '@/types'
import { messageTypeLabel, messageTypeTag } from '@/utils/dict'

const messageStore = useMessageStore()

// ==================== 列表 ====================
const loading = ref(false)
const list = ref<SysMessageVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  readFlag: undefined as number | undefined,
})
/** radio 展示值：-1 全部 / 0 未读 / 1 已读（-1 转 undefined 传给接口） */
const radioFlag = ref(-1)

/** 当前页是否有未读（用于控制「全部已读」按钮） */
const hasUnread = computed(() => list.value.some((m) => m.readFlag === 0))

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const data = await pageMessages({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      readFlag: query.readFlag,
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.readFlag = radioFlag.value === -1 ? undefined : radioFlag.value
  query.pageNum = 1
  fetchList()
}

function handleSizeChange(): void {
  query.pageNum = 1
  fetchList()
}

// ==================== 标记已读 ====================
async function handleRowClick(row: SysMessageVO): Promise<void> {
  if (row.readFlag === 1) return // 已读无需操作
  try {
    await markMessageRead(row.id)
    row.readFlag = 1
    messageStore.fetchUnreadCount()
    fetchList()
  } catch {
    // 拦截器已提示
  }
}

async function handleReadAll(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定将全部未读通知标记为已读吗？', '全部已读', {
      type: 'info',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return // 用户取消
  }
  try {
    await markAllMessagesRead()
    ElMessage.success('已全部标记为已读')
    messageStore.fetchUnreadCount()
    fetchList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.message-page {
  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  .unread-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: #f56c6c;
  }

  .unread-title {
    font-weight: 600;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
