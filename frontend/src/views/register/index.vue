<template>
  <div class="register-page">
    <el-card class="register-card">
      <h2 class="register-title">注册账号</h2>
      <el-alert type="info" :closable="false" title="注册功能将在 F2 阶段实现" />
      <div class="register-back">
        <router-link to="/login">返回登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// F2 阶段实现注册表单与逻辑
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
  width: 420px;

  .register-title {
    margin: 0 0 24px;
    text-align: center;
    font-size: 20px;
  }

  .register-back {
    margin-top: 16px;
    text-align: center;
  }
}
</style>
