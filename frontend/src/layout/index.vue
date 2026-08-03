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
          <!-- 通知铃铛 -->
          <el-popover
            v-model:visible="notifyVisible"
            placement="bottom-end"
            :width="380"
            trigger="click"
            popper-class="notify-popover"
            @show="loadRecent"
          >
            <template #reference>
              <el-badge
                :value="messageStore.unreadCount"
                :hidden="messageStore.unreadCount === 0"
                :max="99"
                class="notify-badge"
              >
                <el-icon class="bell-icon"><Bell /></el-icon>
              </el-badge>
            </template>

            <div class="notify-panel" v-loading="notifyLoading">
              <div class="notify-header">
                <span class="notify-title">未读通知</span>
                <el-button link type="primary" size="small" @click="handleNotifyReadAll">全部已读</el-button>
              </div>
              <div class="notify-list">
                <el-empty v-if="recent.length === 0" description="暂无未读通知" :image-size="50" />
                <div v-for="msg in recent" :key="msg.id" class="notify-item" @click="handleNotifyItem(msg)">
                  <el-tag :type="messageTypeTag(msg.type)" size="small">{{ messageTypeLabel(msg.type) }}</el-tag>
                  <div class="notify-item-body">
                    <div class="notify-item-title">{{ msg.title }}</div>
                    <div class="notify-item-content">{{ msg.content }}</div>
                  </div>
                  <span class="notify-item-time">{{ shortTime(msg.createTime) }}</span>
                </div>
              </div>
              <div class="notify-footer">
                <el-button link type="primary" size="small" @click="goMessage">查看全部</el-button>
              </div>
            </div>
          </el-popover>

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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Expand, Fold, UserFilled } from '@element-plus/icons-vue'
import { markAllMessagesRead, markMessageRead, pageMessages } from '@/api/message'
import { useAppStore } from '@/stores/app'
import { useMessageStore } from '@/stores/message'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'
import type { SysMessageVO } from '@/types'
import { messageTypeLabel, messageTypeTag } from '@/utils/dict'
import { resolveIcon } from '@/utils/icons'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const permissionStore = usePermissionStore()
const userStore = useUserStore()
const messageStore = useMessageStore()

// ==================== 通知铃铛 ====================
const notifyVisible = ref(false)
const notifyLoading = ref(false)
const recent = ref<SysMessageVO[]>([])
let notifyTimer: ReturnType<typeof setInterval> | undefined

/** 拉取最近 10 条未读（popover 每次打开时刷新） */
async function loadRecent(): Promise<void> {
  notifyLoading.value = true
  try {
    const data = await pageMessages({ pageNum: 1, pageSize: 10, readFlag: 0 })
    recent.value = data.records
  } catch {
    // 拦截器已提示
  } finally {
    notifyLoading.value = false
  }
}

/** 点击某条未读：标记已读 + 刷新角标与列表 */
async function handleNotifyItem(msg: SysMessageVO): Promise<void> {
  try {
    await markMessageRead(msg.id)
    messageStore.fetchUnreadCount()
    loadRecent()
  } catch {
    // 拦截器已提示
  }
}

/** 全部已读 */
async function handleNotifyReadAll(): Promise<void> {
  try {
    await markAllMessagesRead()
    messageStore.fetchUnreadCount()
    recent.value = []
  } catch {
    // 拦截器已提示
  }
}

function goMessage(): void {
  notifyVisible.value = false
  router.push('/message')
}

/** 通知时间短格式：MM-dd HH:mm */
function shortTime(time: string): string {
  return time ? time.replace('T', ' ').slice(5, 16) : ''
}

// 登录后（Layout 挂载）拉一次未读数，之后每 60s 轮询；登出时（Layout 卸载）清理定时器
onMounted(() => {
  messageStore.fetchUnreadCount()
  notifyTimer = setInterval(() => messageStore.fetchUnreadCount(), 60000)
})

onBeforeUnmount(() => {
  if (notifyTimer) {
    clearInterval(notifyTimer)
    notifyTimer = undefined
  }
})

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

    .notify-badge {
      cursor: pointer;

      .bell-icon {
        font-size: 20px;
        color: #303133;
      }
    }

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

<!-- 通知 popover 内容被 teleport 到 body，样式需全局 -->
<style lang="scss">
.notify-popover {
  padding: 0 !important;

  .notify-panel {
    .notify-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 12px;
      border-bottom: 1px solid #ebeef5;

      .notify-title {
        font-weight: 600;
        color: #303133;
      }
    }

    .notify-list {
      max-height: 360px;
      overflow-y: auto;

      .notify-item {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        padding: 10px 12px;
        cursor: pointer;
        border-bottom: 1px solid #f0f2f5;

        &:hover {
          background-color: #f5f7fa;
        }

        .notify-item-body {
          flex: 1;
          min-width: 0;

          .notify-item-title {
            font-size: 13px;
            color: #303133;
            font-weight: 600;
          }

          .notify-item-content {
            font-size: 12px;
            color: #909399;
            margin-top: 2px;
            line-height: 1.4;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            line-clamp: 2;
            -webkit-box-orient: vertical;
          }
        }

        .notify-item-time {
          font-size: 12px;
          color: #c0c4cc;
          white-space: nowrap;
          flex-shrink: 0;
        }
      }
    }

    .notify-footer {
      padding: 8px 12px;
      text-align: center;
      border-top: 1px solid #ebeef5;
    }
  }
}
</style>
