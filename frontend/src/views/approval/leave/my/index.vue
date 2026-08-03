<template>
  <div class="leave-my-page">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="opt in approvalStatusOptions" :key="opt.value ?? 'all'" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="router.push('/approval/leave/create')">提交请假</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="单号" width="80" />
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
        <el-table-column prop="approverName" label="审批人" min-width="100" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              @click="handleCancel(row)"
            >
              撤回
            </el-button>
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

    <LeaveDetailDrawer v-model="detailVisible" :leave-id="detailId" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelLeave, myLeaves } from '@/api/approval'
import type { LeaveVO } from '@/types'
import { approvalStatusDict, approvalStatusOptions, leaveTypeDict } from '@/utils/dict'
import LeaveDetailDrawer from '../components/LeaveDetailDrawer.vue'

const router = useRouter()

// ==================== 列表 ====================
const loading = ref(false)
const list = ref<LeaveVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined as number | undefined,
})

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const data = await myLeaves({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status,
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
  query.status = undefined
  handleSearch()
}

function handleSizeChange(): void {
  query.pageNum = 1
  fetchList()
}

function statusTag(status: number): string {
  return approvalStatusDict[status]?.tagType ?? 'info'
}

// ==================== 撤回 ====================
async function handleCancel(row: LeaveVO): Promise<void> {
  try {
    await ElMessageBox.confirm('撤回后审批人将无法处理该申请，确定撤回吗？', '撤回确认', {
      type: 'warning',
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
    })
  } catch {
    return // 用户取消
  }
  try {
    await cancelLeave(row.id)
    ElMessage.success('撤回成功')
    fetchList()
  } catch {
    // 非草稿/待审批等非法流转由拦截器展示后端 400 提示
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
.leave-my-page {
  .search-card {
    margin-bottom: 16px;
  }

  .toolbar {
    margin-bottom: 12px;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
