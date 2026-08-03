<template>
  <el-container class="app-layout">
    <!-- 侧边栏（F2 起由动态菜单驱动） -->
    <el-aside width="220px" class="app-aside">
      <div class="app-logo">OA 协同办公</div>
      <el-menu router :default-active="activeMenu" class="app-menu">
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="app-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="app-header-right">
          <el-dropdown @command="handleCommand">
            <span class="app-user">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.nickname || '未登录' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || '')

async function handleCommand(command: string): Promise<void> {
  if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100%;
}

.app-aside {
  background-color: #304156;
  color: #fff;

  .app-logo {
    height: 56px;
    line-height: 56px;
    text-align: center;
    font-size: 18px;
    font-weight: 600;
    color: #fff;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  .app-menu {
    border-right: none;
    background-color: transparent;
    --el-menu-text-color: #bfcbd9;
    --el-menu-hover-bg-color: #263445;
    --el-menu-active-color: #409eff;
  }
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.app-header-right {
  .app-user {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    color: #303133;
  }
}

.app-main {
  background-color: #f5f7fa;
}
</style>
