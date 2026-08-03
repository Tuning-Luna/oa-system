<template>
  <div class="approver-select">
    <!-- 有用户列表权限：下拉选择；无权限/获取失败：手动输入用户 ID 兜底 -->
    <el-select v-if="mode === 'select'" v-model="value" filterable placeholder="选择审批人" style="width: 100%">
      <el-option v-for="opt in options" :key="opt.id" :label="opt.label" :value="opt.id" />
    </el-select>
    <div v-else class="approver-manual">
      <el-input-number v-model="value" :min="1" :controls="false" placeholder="审批人用户 ID" style="width: 200px" />
      <span class="approver-tip">无用户列表权限，请直接填写审批人用户 ID（admin=1）</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { pageUsers } from '@/api/user'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  /** 审批人用户 ID */
  modelValue: number | undefined
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | undefined): void
}>()

const userStore = useUserStore()
const mode = ref<'select' | 'manual'>('select')
const options = ref<{ id: number; label: string }[]>([])

/** 双向绑定代理：props 只读，经 emit 回写 */
const value = computed({
  get: () => props.modelValue,
  set: (v: number | undefined) => emit('update:modelValue', v),
})

async function init(): Promise<void> {
  // 普通用户无 system:user:list 权限：直接走手动输入 ID，避免触发「无访问权限」错误提示
  if (!userStore.permissions.includes('system:user:list')) {
    mode.value = 'manual'
    return
  }
  try {
    const data = await pageUsers({ pageNum: 1, pageSize: 100 })
    options.value = data.records.map((u) => ({
      id: u.id,
      label: `${u.nickname || u.username}（ID:${u.id}）`,
    }))
    mode.value = 'select'
  } catch {
    // 获取用户列表失败 → 兜底手动输入
    mode.value = 'manual'
  }
}

onMounted(init)
</script>

<style scoped lang="scss">
.approver-manual {
  display: flex;
  align-items: center;
  gap: 10px;

  .approver-tip {
    font-size: 12px;
    color: #909399;
  }
}
</style>
