<template>
  <div class="leave-create-page">
    <el-card shadow="never" class="form-card">
      <template #header>提交请假申请</template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 560px">
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" placeholder="请选择请假类型" style="width: 100%">
            <el-option v-for="(label, value) in leaveTypeDict" :key="value" :label="label" :value="Number(value)" />
          </el-select>
        </el-form-item>

        <el-form-item label="起止日期" prop="dateRange">
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="handleDateChange"
          />
        </el-form-item>

        <el-form-item label="请假天数" prop="days">
          <el-input-number v-model="form.days" :min="1" :max="365" style="width: 160px" />
          <span class="form-tip">选择日期后自动计算，可手动调整</span>
        </el-form-item>

        <el-form-item label="请假事由" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请填写请假事由（必填，最长 500 字）"
          />
        </el-form-item>

        <el-form-item label="审批人" prop="approverId">
          <ApproverSelect v-model="form.approverId" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
          <el-button @click="router.push('/approval/leave/my')">返回我的请假</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import ApproverSelect from '@/components/approval/ApproverSelect.vue'
import { submitLeave } from '@/api/approval'
import { leaveTypeDict } from '@/utils/dict'

const router = useRouter()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const form = reactive({
  leaveType: undefined as number | undefined,
  dateRange: null as [string, string] | null,
  days: 1,
  reason: '',
  approverId: undefined as number | undefined,
})

// ==================== 校验规则 ====================
const rules = computed<FormRules>(() => ({
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  dateRange: [
    {
      validator: (_rule: unknown, value: [string, string] | null, callback: (error?: Error) => void) => {
        if (!value || !value[0] || !value[1]) {
          return callback(new Error('请选择起止日期'))
        }
        return callback()
      },
      trigger: 'change',
    },
  ],
  days: [{ required: true, message: '请填写请假天数', trigger: 'blur' }],
  reason: [
    { required: true, message: '请填写请假事由', trigger: 'blur' },
    { max: 500, message: '请假事由最长 500 字', trigger: 'blur' },
  ],
  approverId: [
    {
      validator: (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
        if (!value || value < 1) {
          return callback(new Error('请选择审批人或填写审批人用户 ID'))
        }
        return callback()
      },
      trigger: 'change',
    },
  ],
}))

// ==================== 天数自动计算 ====================
function handleDateChange(): void {
  const [start, end] = form.dateRange ?? []
  if (start && end) {
    form.days = calcDays(start, end)
  }
}

/** 按起止日期计算天数（含首尾） */
function calcDays(start: string, end: string): number {
  const ms = new Date(end).getTime() - new Date(start).getTime()
  return Math.round(ms / 86400000) + 1
}

// ==================== 提交 ====================
async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.dateRange) return
  submitLoading.value = true
  try {
    await submitLeave({
      leaveType: form.leaveType as number,
      startDate: form.dateRange[0],
      endDate: form.dateRange[1],
      days: form.days,
      reason: form.reason,
      approverId: form.approverId as number,
    })
    ElMessage.success('提交成功，等待审批')
    router.push('/approval/leave/my')
  } catch {
    // 失败提示（如审批人无效 400）由 request 拦截器统一处理
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.leave-create-page {
  .form-card {
    max-width: 720px;
  }

  .form-tip {
    margin-left: 10px;
    font-size: 12px;
    color: #909399;
  }
}
</style>
