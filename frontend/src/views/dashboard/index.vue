<template>
  <el-card>
    <div class="welcome">
      <h2 class="welcome-title">你好，{{ userStore.nickname }}，欢迎使用企业协同办公管理系统</h2>
      <div class="role-tags">
        <el-tag v-for="role in userStore.roles" :key="role" type="success" class="role-tag">
          {{ role }}
        </el-tag>
        <el-tag v-if="userStore.roles.length === 0" type="warning">
          暂无角色（请联系管理员分配角色）
        </el-tag>
      </div>
      <el-descriptions :column="2" class="welcome-desc" border>
        <el-descriptions-item label="登录账号">{{ userStore.userInfo?.user.username }}</el-descriptions-item>
        <el-descriptions-item label="角色数">{{ userStore.roles.length }}</el-descriptions-item>
        <el-descriptions-item label="权限数">{{ userStore.permissions.length }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userStore.userInfo?.user.email || '未设置' }}</el-descriptions-item>
      </el-descriptions>
      <p class="tip">首页内容将在后续阶段完善（当前为静态占位，后端暂无数据统计接口）。</p>

      <!-- 仅管理员可见（v-role 示例） -->
      <el-card v-role="'admin'" shadow="never" class="admin-card">
        <template #header>管理快捷入口</template>
        <div class="admin-links">
          <el-button type="primary" plain @click="router.push('/system/user')">用户管理</el-button>
          <el-button type="primary" plain @click="router.push('/system/role')">角色管理</el-button>
          <el-button type="primary" plain @click="router.push('/system/menu')">菜单管理</el-button>
        </div>
      </el-card>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
</script>

<style scoped lang="scss">
.welcome {
  .welcome-title {
    margin: 0 0 16px;
  }

  .role-tags {
    margin-bottom: 16px;

    .role-tag {
      margin-right: 8px;
    }
  }

  .welcome-desc {
    margin-bottom: 16px;
  }

  .tip {
    color: #909399;
    font-size: 13px;
  }

  .admin-card {
    margin-top: 16px;

    .admin-links {
      display: flex;
      gap: 12px;
    }
  }
}
</style>
