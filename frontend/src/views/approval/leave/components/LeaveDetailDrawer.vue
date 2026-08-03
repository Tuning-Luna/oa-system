<template>
  <el-drawer
    :model-value="modelValue"
    title="请假详情"
    size="480px"
    :destroy-on-close="false"
    @update:model-value="(val: boolean) => emit('update:modelValue', val)"
    @open="loadDetail"
  >
    <div v-loading="loading">
      <template v-if="leave">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请单号">{{ leave.id }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ leave.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="请假类型">{{ leaveTypeName(leave.leaveType) }}</el-descriptions-item>
          <el-descriptions-item label="起止日期">{{ leave.startDate }} ~ {{ leave.endDate }}</el-descriptions-item>
          <el-descriptions-item label="请假天数">{{ leave.days }} 天</el-descriptions-item>
          <el-descriptions-item label="请假事由">{{ leave.reason }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ leave.approverName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusTag(leave.status)">{{ leave.statusDesc }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ leave.createTime }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">审批记录</el-divider>
        <el-empty v-if="records.length === 0" description="暂无审批记录" :image-size="60" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="record in records"
            :key="record.id"
            :timestamp="record.createTime"
            placement="top"
            :type="actionTimelineType(record.action)"
            :hollow="record.action !== 2"
          >
            <div class="record-line">
              <el-tag :type="actionTag(record.action)" size="small">{{ record.actionDesc }}</el-tag>
              <span class="record-approver">{{ record.approverName }}</span>
            </div>
            <div v-if="record.comment" class="record-comment">{{ record.comment }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getApprovalRecords, getLeave } from '@/api/approval'
import type { ApprovalRecordVO, LeaveVO } from '@/types'
import { approvalActionDict, approvalStatusDict, leaveTypeDict } from '@/utils/dict'

const props = defineProps<{
  modelValue: boolean
  /** 申请单 ID，打开抽屉时传 */
  leaveId: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const loading = ref(false)
const leave = ref<LeaveVO | null>(null)
const records = ref<ApprovalRecordVO[]>([])

function leaveTypeName(type: number): string {
  return leaveTypeDict[type] ?? String(type)
}

function statusTag(status: number): string {
  return approvalStatusDict[status]?.tagType ?? 'info'
}

function actionTag(action: number): string {
  return approvalActionDict[action]?.tagType ?? 'info'
}

/** 时间线圆点类型：通过=实心绿，其余=空心 */
function actionTimelineType(action: number): string {
  return approvalActionDict[action]?.tagType ?? 'info'
}

/** 打开抽屉时重新拉取详情 + 审批记录 */
async function loadDetail(): Promise<void> {
  if (!props.leaveId) return
  loading.value = true
  leave.value = null
  records.value = []
  try {
    leave.value = await getLeave(props.leaveId)
    records.value = await getApprovalRecords(1, props.leaveId)
  } finally {
    loading.value = false
  }
}

// leaveId 变化（抽屉内重新打开另一单）时也刷新
watch(
  () => props.leaveId,
  () => {
    if (props.modelValue) loadDetail()
  },
)
</script>

<style scoped lang="scss">
.record-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;

  .record-approver {
    font-size: 13px;
    color: #606266;
  }
}

.record-comment {
  font-size: 13px;
  color: #303133;
  line-height: 1.5;
  word-break: break-all;
}
</style>
