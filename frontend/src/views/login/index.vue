<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="login-title">企业协同办公管理系统</h2>
      <el-alert
        class="login-hint"
        type="info"
        :closable="false"
        show-icon
        title="默认管理员：admin / Admin@123456"
      />
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-links">
        <router-link to="/register">注册账号</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    await userStore.fetchMe()
    ElMessage.success('登录成功')
    // 守卫会把未登录访问的目标地址放到 redirect，登录后回跳
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch {
    // 失败提示已由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2b5876, #4e4376);
}

.login-card {
  width: 380px;

  .login-title {
    margin: 0 0 16px;
    text-align: center;
    font-size: 20px;
  }

  .login-hint {
    margin-bottom: 16px;
  }

  .login-btn {
    width: 100%;
  }

  .login-links {
    margin-top: 12px;
    text-align: center;
    font-size: 14px;
    color: #409eff;
  }
}
</style>
