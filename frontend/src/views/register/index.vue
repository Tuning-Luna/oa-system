<template>
  <div class="register-page">
    <el-card class="register-card">
      <h2 class="register-title">注册账号</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20 位字母、数字、下划线" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="8-20 位，需含字母和数字" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" />
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
        <el-form-item>
          <el-button type="primary" class="register-btn" :loading="loading" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-back">
        <router-link to="/login">已有账号？返回登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  phone: '',
})

/** 可选字段的格式校验：为空则通过 */
function optionalPattern(pattern: RegExp, message: string) {
  return {
    validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
      if (!value) return callback()
      return pattern.test(value) ? callback() : callback(new Error(message))
    },
    trigger: 'blur' as const,
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需在 3-20 之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字、下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度需在 8-20 之间', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (!value) return callback()
        return value === form.password ? callback() : callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur',
    },
  ],
  nickname: [{ max: 50, message: '昵称最长 50 个字符', trigger: 'blur' }],
  email: [optionalPattern(/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/, '邮箱格式不正确')],
  phone: [optionalPattern(/^1[3-9]\d{9}$/, '手机号格式不正确')],
}

async function handleRegister(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined,
    })
    ElMessage.success('注册成功，请等待管理员分配角色后登录')
    router.push('/login')
  } catch {
    // 失败提示已由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2b5876, #4e4376);
}

.register-card {
  width: 440px;

  .register-title {
    margin: 0 0 24px;
    text-align: center;
    font-size: 20px;
  }

  .register-btn {
    width: 100%;
  }

  .register-back {
    margin-top: 12px;
    text-align: center;
    font-size: 14px;
    color: #409eff;
  }
}
</style>
