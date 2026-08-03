<template>
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
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { markAllMessagesRead, markMessageRead, pageMessages } from '@/api/message'
import { useMessageStore } from '@/stores/message'
import type { SysMessageVO } from '@/types'
import { messageTypeLabel, messageTypeTag } from '@/utils/dict'

const router = useRouter()
const messageStore = useMessageStore()

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

// 挂载后拉一次未读数，之后每 60s 轮询；卸载时清理定时器
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
</script>

<style scoped lang="scss">
.notify-badge {
  cursor: pointer;

  .bell-icon {
    font-size: 20px;
    color: #303133;
  }
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
