<template>
  <el-dialog v-model="visible" title="编辑用户" width="520px" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="选填，最长 50 字符" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="选填" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="选填" />
      </el-form-item>
      <el-form-item label="头像" prop="avatar">
        <el-input v-model="form.avatar" placeholder="选填，头像 URL" />
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
import { updateUser } from '@/api/user'
import type { UserVO } from '@/types'
import { emailRules, nicknameRules, phoneRules } from '@/utils/validators'

const props = defineProps<{
  modelValue: boolean
  /** 当前编辑的行数据 */
  user: UserVO | null
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
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  status: 1,
})

const rules: FormRules = {
  nickname: nicknameRules,
  email: emailRules,
  phone: phoneRules,
}

// 打开时用当前行数据填充
watch(
  () => props.modelValue,
  (open) => {
    if (open && props.user) {
      form.id = props.user.id
      form.nickname = props.user.nickname ?? ''
      form.email = props.user.email ?? ''
      form.phone = props.user.phone ?? ''
      form.avatar = props.user.avatar ?? ''
      form.status = props.user.status
      formRef.value?.clearValidate()
    }
  },
)

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await updateUser(form.id, {
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined,
      avatar: form.avatar || undefined,
      status: form.status,
    })
    ElMessage.success('修改成功')
    emit('success')
    visible.value = false
  } catch {
    // 拦截器已提示
  } finally {
    submitLoading.value = false
  }
}
</script>
