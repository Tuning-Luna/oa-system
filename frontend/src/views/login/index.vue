<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="login-title">企业协同办公管理系统</h2>
      <el-alert type="info" :closable="false" title="登录功能将在 F2 阶段实现" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
// F2 阶段实现登录表单与逻辑
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
    margin: 0 0 24px;
    text-align: center;
    font-size: 20px;
  }
}
</style>
