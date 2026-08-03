<template>
  <el-dialog v-model="visible" title="新增用户" width="520px" :close-on-click-modal="false" @closed="reset">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="3-20 位字母、数字、下划线" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="8-20 位，需含字母和数字" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="选填，最长 50 字符" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="选填" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="选填" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.roleIds" multiple placeholder="可选，注册后立即分配" style="width: 100%">
          <el-option v-for="role in roleOptions" :key="role.id" :label="`${role.name}（${role.code}）`" :value="role.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { register } from '@/api/auth'
import { assignRoles } from '@/api/user'
import type { RoleVO } from '@/types'
import { emailRules, nicknameRules, passwordRules, phoneRules, usernameRules } from '@/utils/validators'

const props = defineProps<{
  modelValue: boolean
  roleOptions: RoleVO[]
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
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  roleIds: [] as number[],
})

const rules: FormRules = {
  username: usernameRules,
  password: passwordRules,
  nickname: nicknameRules,
  email: emailRules,
  phone: phoneRules,
}

function reset(): void {
  formRef.value?.clearValidate()
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.roleIds = []
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    // 后端无管理员建号接口，新增复用公开注册接口 + 分配角色
    const user = await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined,
    })
    if (form.roleIds.length > 0) {
      await assignRoles(user.id, form.roleIds)
    }
    ElMessage.success('新增用户成功')
    emit('success')
    visible.value = false
  } catch {
    // 失败提示由 request 拦截器统一处理
  } finally {
    submitLoading.value = false
  }
}
</script>
