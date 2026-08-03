<template>
  <el-drawer
    :model-value="modelValue"
    :title="title"
    size="480px"
    @update:model-value="(val: boolean) => emit('update:modelValue', val)"
    @open="loadDetail"
  >
    <div v-loading="loading">
      <template v-if="item">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请单号">{{ item.id }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ item.applicantName }}</el-descriptions-item>

          <!-- 请假字段 -->
          <template v-if="businessType === 1 && leave">
            <el-descriptions-item label="请假类型">{{ leaveTypeDict[leave.leaveType] ?? leave.leaveType }}</el-descriptions-item>
            <el-descriptions-item label="起止日期">{{ leave.startDate }} ~ {{ leave.endDate }}</el-descriptions-item>
            <el-descriptions-item label="请假天数">{{ leave.days }} 天</el-descriptions-item>
            <el-descriptions-item label="请假事由">{{ leave.reason }}</el-descriptions-item>
          </template>

          <!-- 报销字段 -->
          <template v-else-if="businessType === 2 && reimburse">
            <el-descriptions-item label="报销金额">¥ {{ formatMoney(reimburse.amount) }}</el-descriptions-item>
            <el-descriptions-item label="报销类别">{{ reimburse.category }}</el-descriptions-item>
            <el-descriptions-item label="报销事由">{{ reimburse.reason }}</el-descriptions-item>
          </template>

          <el-descriptions-item label="审批人">{{ item.approverName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusTag(item.status)">{{ item.statusDesc }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ item.createTime }}</el-descriptions-item>
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
import { computed, ref, watch } from 'vue'
import { getApprovalRecords, getLeave, getReimburse } from '@/api/approval'
import type { ApprovalRecordVO, LeaveVO, ReimburseVO } from '@/types'
import { approvalActionDict, approvalStatusDict, leaveTypeDict } from '@/utils/dict'
import { formatMoney } from '@/utils/format'

const props = defineProps<{
  modelValue: boolean
  /** 业务类型：1 请假 / 2 报销 */
  businessType: number
  /** 申请单 ID */
  businessId: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const loading = ref(false)
const leave = ref<LeaveVO | null>(null)
const reimburse = ref<ReimburseVO | null>(null)
const records = ref<ApprovalRecordVO[]>([])

/** 当前业务对象（请假或报销） */
const item = computed(() => (props.businessType === 1 ? leave.value : reimburse.value))

const title = computed(() => (props.businessType === 1 ? '请假详情' : '报销详情'))

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
  if (!props.businessId) return
  loading.value = true
  leave.value = null
  reimburse.value = null
  records.value = []
  try {
    if (props.businessType === 1) {
      leave.value = await getLeave(props.businessId)
    } else {
      reimburse.value = await getReimburse(props.businessId)
    }
    records.value = await getApprovalRecords(props.businessType, props.businessId)
  } finally {
    loading.value = false
  }
}

// businessId 变化（抽屉内重新打开另一单）时也刷新
watch(
  () => props.businessId,
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
