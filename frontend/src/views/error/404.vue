<template>
  <div class="not-found">
    <h1 class="code">404</h1>
    <p class="text">页面不存在或无权访问</p>
    <el-button type="primary" @click="router.push('/')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background-color: #f5f7fa;

  .code {
    margin: 0;
    font-size: 80px;
    color: #409eff;
  }

  .text {
    color: #909399;
  }
}
</style>
