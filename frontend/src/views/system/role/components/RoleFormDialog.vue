<template>
  <el-dialog v-model="visible" :title="role ? '编辑角色' : '新增角色'" width="520px" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="角色名称" prop="name">
        <el-input v-model="form.name" placeholder="最长 50 字符" />
      </el-form-item>
      <el-form-item label="角色编码" prop="code">
        <el-input v-model="form.code" placeholder="字母开头，仅字母/数字/下划线，全局唯一" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填，最长 255 字符" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createRole, updateRole } from '@/api/role'
import type { RoleVO } from '@/types'

const props = defineProps<{
  modelValue: boolean
  /** 编辑的行数据；null 表示新增 */
  role: RoleVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const form = reactive({
  id: 0,
  name: '',
  code: '',
  description: '',
  status: 1,
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 50, message: '角色名称最长 50 字符', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{0,49}$/, message: '字母开头，仅含字母/数字/下划线，最长 50', trigger: 'blur' },
  ],
  description: [{ max: 255, message: '角色描述最长 255 字符', trigger: 'blur' }],
}

// 打开时填充（编辑用行数据，新增用空值）
watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    formRef.value?.clearValidate()
    form.id = props.role?.id ?? 0
    form.name = props.role?.name ?? ''
    form.code = props.role?.code ?? ''
    form.description = props.role?.description ?? ''
    form.status = props.role?.status ?? 1
  },
)

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload = {
      name: form.name,
      code: form.code,
      description: form.description || undefined,
      status: form.status,
    }
    if (form.id) {
      await updateRole(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createRole(payload)
      ElMessage.success('新增成功')
    }
    emit('success')
    visible.value = false
  } catch {
    // 失败提示（如编码重复 400）由 request 拦截器统一处理
  } finally {
    submitLoading.value = false
  }
}
</script>
