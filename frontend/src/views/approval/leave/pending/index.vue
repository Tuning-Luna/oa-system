<template>
  <div class="leave-pending-page">
    <!-- 列表 -->
    <el-card shadow="never">
      <div class="toolbar">待我审批的请假申请</div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="单号" width="80" />
        <el-table-column prop="applicantName" label="申请人" min-width="100" />
        <el-table-column label="请假类型" width="100" align="center">
          <template #default="{ row }">
            {{ leaveTypeDict[row.leaveType] ?? row.leaveType }}
          </template>
        </el-table-column>
        <el-table-column label="起止日期" min-width="180">
          <template #default="{ row }">
            {{ row.startDate }} ~ {{ row.endDate }}
          </template>
        </el-table-column>
        <el-table-column prop="days" label="天数" width="70" align="center">
          <template #default="{ row }">{{ row.days }} 天</template>
        </el-table-column>
        <el-table-column prop="reason" label="事由" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="success" @click="openAction(row, 'approve')">通过</el-button>
            <el-button link type="danger" @click="openAction(row, 'reject')">拒绝</el-button>
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

    <!-- 审批弹窗（通过/拒绝共用） -->
    <el-dialog
      v-model="actionVisible"
      :title="actionMode === 'approve' ? '审批通过' : '审批拒绝'"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetAction"
    >
      <template v-if="currentRow">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="申请人">{{ currentRow.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="请假类型">{{ leaveTypeDict[currentRow.leaveType] ?? currentRow.leaveType }}</el-descriptions-item>
          <el-descriptions-item label="起止日期" :span="2">
            {{ currentRow.startDate }} ~ {{ currentRow.endDate }}（{{ currentRow.days }} 天）
          </el-descriptions-item>
          <el-descriptions-item label="请假事由" :span="2">{{ currentRow.reason }}</el-descriptions-item>
        </el-descriptions>

        <el-form ref="actionFormRef" :model="actionForm" :rules="actionRules" label-width="80px" class="action-form">
          <el-form-item label="审批意见" prop="comment">
            <el-input
              v-model="actionForm.comment"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              :placeholder="actionMode === 'approve' ? '选填' : '拒绝时请填写审批意见（必填）'"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button
          :type="actionMode === 'approve' ? 'success' : 'danger'"
          :loading="actionLoading"
          @click="handleAction"
        >
          {{ actionMode === 'approve' ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <LeaveDetailDrawer v-model="detailVisible" :leave-id="detailId" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { approveLeave, pendingLeaves, rejectLeave } from '@/api/approval'
import type { LeaveVO } from '@/types'
import { leaveTypeDict } from '@/utils/dict'
import LeaveDetailDrawer from '../components/LeaveDetailDrawer.vue'

// ==================== 列表 ====================
const loading = ref(false)
const list = ref<LeaveVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
})

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const data = await pendingLeaves({ pageNum: query.pageNum, pageSize: query.pageSize })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSizeChange(): void {
  query.pageNum = 1
  fetchList()
}

// ==================== 通过 / 拒绝 ====================
const actionVisible = ref(false)
const actionLoading = ref(false)
const actionMode = ref<'approve' | 'reject'>('approve')
const currentRow = ref<LeaveVO | null>(null)
const actionFormRef = ref<FormInstance>()
const actionForm = reactive({ comment: '' })

const actionRules = computed<FormRules>(() => ({
  comment: actionMode.value === 'reject'
    ? [{ required: true, message: '拒绝时请填写审批意见', trigger: 'blur' }]
    : [{ max: 500, message: '审批意见最长 500 字', trigger: 'blur' }],
}))

function openAction(row: LeaveVO, mode: 'approve' | 'reject'): void {
  currentRow.value = row
  actionMode.value = mode
  actionForm.comment = ''
  actionVisible.value = true
}

function resetAction(): void {
  actionFormRef.value?.clearValidate()
  currentRow.value = null
}

async function handleAction(): Promise<void> {
  const valid = await actionFormRef.value?.validate().catch(() => false)
  if (!valid || !currentRow.value) return
  actionLoading.value = true
  try {
    const comment = actionForm.comment || undefined
    if (actionMode.value === 'approve') {
      await approveLeave(currentRow.value.id, { comment })
      ElMessage.success('已通过')
    } else {
      await rejectLeave(currentRow.value.id, { comment })
      ElMessage.success('已拒绝')
    }
    actionVisible.value = false
    // 审批后单子不在待办中，回第 1 页刷新
    query.pageNum = 1
    fetchList()
  } catch {
    // 非法流转（如对已审批单再操作）由拦截器展示后端 400/403 提示
  } finally {
    actionLoading.value = false
  }
}

// ==================== 详情 ====================
const detailVisible = ref(false)
const detailId = ref(0)

function openDetail(row: LeaveVO): void {
  detailId.value = row.id
  detailVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.leave-pending-page {
  .toolbar {
    margin-bottom: 12px;
    color: #606266;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }

  .action-form {
    margin-top: 16px;
  }
}
</style>
