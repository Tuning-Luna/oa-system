<template>
  <div class="reimburse-create-page">
    <el-card shadow="never" class="form-card">
      <template #header>提交报销申请</template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 560px">
        <el-form-item label="报销金额" prop="amount">
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :max="999999999.99"
            :precision="2"
            placeholder="大于 0，两位小数"
            style="width: 200px"
          />
          <span class="form-tip">元，保留两位小数</span>
        </el-form-item>

        <el-form-item label="报销类别" prop="category">
          <el-select
            v-model="form.category"
            filterable
            allow-create
            default-first-option
            placeholder="输入或选择报销类别"
            style="width: 100%"
          >
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item label="报销事由" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请填写报销事由（必填，最长 500 字）"
          />
        </el-form-item>

        <el-form-item label="审批人" prop="approverId">
          <ApproverSelect v-model="form.approverId" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
          <el-button @click="router.push('/approval/reimburse/my')">返回我的报销</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import ApproverSelect from '@/components/approval/ApproverSelect.vue'
import { submitReimburse } from '@/api/approval'

const router = useRouter()

/** 常用报销类别建议（可输入新类别） */
const categoryOptions = ['交通费', '餐饮费', '办公用品', '差旅费', '住宿费', '通讯费', '培训费', '其他']

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const form = reactive({
  amount: undefined as number | undefined,
  category: '',
  reason: '',
  approverId: undefined as number | undefined,
})

const rules: FormRules = {
  amount: [
    {
      validator: (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
        if (value === undefined || value === null) {
          return callback(new Error('请输入报销金额'))
        }
        if (value <= 0) {
          return callback(new Error('报销金额必须大于 0'))
        }
        return callback()
      },
      trigger: 'blur',
    },
  ],
  category: [
    { required: true, message: '请输入或选择报销类别', trigger: 'blur' },
    { max: 50, message: '报销类别最长 50 字', trigger: 'blur' },
  ],
  reason: [
    { required: true, message: '请填写报销事由', trigger: 'blur' },
    { max: 500, message: '报销事由最长 500 字', trigger: 'blur' },
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
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await submitReimburse({
      amount: form.amount as number,
      category: form.category,
      reason: form.reason,
      approverId: form.approverId as number,
    })
    ElMessage.success('提交成功，等待审批')
    router.push('/approval/reimburse/my')
  } catch {
    // 失败提示（如审批人无效 400）由 request 拦截器统一处理
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.reimburse-create-page {
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
