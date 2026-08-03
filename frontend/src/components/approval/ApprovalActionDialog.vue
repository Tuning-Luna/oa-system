<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'approve' ? '审批通过' : '审批拒绝'"
    width="520px"
    :close-on-click-modal="false"
    @closed="reset"
  >
    <div class="action-summary">
      <div class="summary-row">
        <span class="summary-label">申请人：</span>
        <span class="summary-value">{{ applicantName }}</span>
      </div>
      <div v-for="row in rows" :key="row.label" class="summary-row">
        <span class="summary-label">{{ row.label }}：</span>
        <span class="summary-value">{{ row.value }}</span>
      </div>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="action-form">
      <el-form-item label="审批意见" prop="comment">
        <el-input
          v-model="form.comment"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          :placeholder="mode === 'approve' ? '选填' : '拒绝时请填写审批意见（必填）'"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        :type="mode === 'approve' ? 'success' : 'danger'"
        :loading="loading"
        @click="handleConfirm"
      >
        {{ mode === 'approve' ? '通过' : '拒绝' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

/** 业务字段摘要行 */
export interface InfoRow {
  label: string
  value: string
}

const props = defineProps<{
  modelValue: boolean
  /** approve 通过 / reject 拒绝 */
  mode: 'approve' | 'reject'
  applicantName: string
  /** 业务字段摘要（类型/日期/金额等），不含申请人 */
  rows: InfoRow[]
  /** 提交中 loading（由父组件控制，提交成功后父组件关闭弹窗） */
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', comment?: string): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const formRef = ref<FormInstance>()
const form = reactive({ comment: '' })

const rules = computed<FormRules>(() => ({
  comment:
    props.mode === 'reject'
      ? [{ required: true, message: '拒绝时请填写审批意见', trigger: 'blur' }]
      : [{ max: 500, message: '审批意见最长 500 字', trigger: 'blur' }],
}))

/** 每次打开清空意见 */
watch(
  () => props.modelValue,
  (open) => {
    if (open) form.comment = ''
  },
)

function reset(): void {
  formRef.value?.clearValidate()
}

async function handleConfirm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('confirm', form.comment || undefined)
}
</script>

<style scoped lang="scss">
.action-summary {
  padding: 8px 0 4px;

  .summary-row {
    font-size: 14px;
    line-height: 1.8;
    word-break: break-all;

    .summary-label {
      color: #606266;
    }

    .summary-value {
      color: #303133;
    }
  }
}

.action-form {
  margin-top: 8px;
}
</style>
