<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="app-aside">
      <div class="app-logo">
        {{ appStore.sidebarCollapsed ? 'OA' : 'OA 协同办公' }}
      </div>
      <el-scrollbar class="app-scrollbar">
        <el-menu
          router
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          class="app-menu"
        >
          <template v-for="menu in permissionStore.menuRoutes" :key="menu.path">
            <!-- 目录（有子菜单） -->
            <el-sub-menu v-if="menu.children && menu.children.length" :index="fullPath(menu.path)">
              <template #title>
                <el-icon><component :is="resolveIcon(menu.meta?.icon)" /></el-icon>
                <span>{{ menu.meta?.title }}</span>
              </template>
              <el-menu-item
                v-for="child in menu.children"
                :key="child.path"
                :index="fullPath(menu.path, child.path)"
              >
                <el-icon><component :is="resolveIcon(child.meta?.icon)" /></el-icon>
                <template #title>{{ child.meta?.title }}</template>
              </el-menu-item>
            </el-sub-menu>
            <!-- 叶子菜单 -->
            <el-menu-item v-else :index="fullPath(menu.path)">
              <el-icon><component :is="resolveIcon(menu.meta?.icon)" /></el-icon>
              <template #title>{{ menu.meta?.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="app-header">
        <div class="app-header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
            <Expand v-if="appStore.sidebarCollapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="app-header-right">
          <!-- 通知铃铛（自含未读角标/轮询/最近未读） -->
          <NotificationBell />
          <el-dropdown @command="handleCommand">
            <span class="app-user">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.nickname }}
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
import { Expand, Fold, UserFilled } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'
import { resolveIcon } from '@/utils/icons'
import NotificationBell from './components/NotificationBell.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const permissionStore = usePermissionStore()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

/** 面包屑：route.matched 中含 meta.title 的层级 */
const breadcrumbs = computed(() =>
  route.matched.filter((record) => record.meta?.title).map((record) => ({
    path: record.path,
    title: record.meta?.title as string,
  })),
)

/** 拼完整路径（动态路由以 Layout 为父，path 为相对路径） */
function fullPath(path: string, child?: string): string {
  return child ? `/${path}/${child}`.replace(/\/+/g, '/') : `/${path}`.replace(/\/+/g, '/')
}

async function handleCommand(command: string): Promise<void> {
  if (command === 'logout') {
    await userStore.logout()
    // 清理路由状态，下次登录重新按权限生成
    permissionStore.resetRoutes()
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
  transition: width 0.28s;
  overflow: hidden;

  .app-logo {
    height: 56px;
    line-height: 56px;
    text-align: center;
    font-size: 18px;
    font-weight: 600;
    color: #fff;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    white-space: nowrap;
    overflow: hidden;
  }

  .app-scrollbar {
    height: calc(100% - 56px);
  }

  .app-menu {
    border-right: none;
    background-color: transparent;
    --el-menu-text-color: #bfcbd9;
    --el-menu-hover-bg-color: #263445;
    --el-menu-active-color: #409eff;
    --el-menu-bg-color: transparent;
  }
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;

  .app-header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: #303133;
    }
  }

  .app-header-right {
    display: flex;
    align-items: center;
    gap: 20px;

    .app-user {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      cursor: pointer;
      color: #303133;
    }
  }
}

.app-main {
  background-color: #f5f7fa;
}
</style>
