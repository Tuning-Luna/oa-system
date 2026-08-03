<template>
  <div>
    <el-card>
      <h2>欢迎使用企业协同办公管理系统</h2>
      <p>当前登录用户：{{ userStore.nickname || '未登录' }}</p>
      <p class="tip">首页内容将在 F2 阶段完善（当前为静态占位，后端暂无统计接口）。</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped lang="scss">
.tip {
  color: #909399;
  font-size: 13px;
}
</style>
