<template>
  <el-dialog v-model="visible" title="分配角色" width="480px" :close-on-click-modal="false">
    <p class="role-tip">为用户「{{ username }}」分配角色（全量替换）</p>
    <el-select v-model="selectedRoles" multiple placeholder="选择角色" style="width: 100%">
      <el-option v-for="role in roleOptions" :key="role.id" :label="`${role.name}（${role.code}）`" :value="role.id" />
    </el-select>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { assignRoles, getUserRoleIds } from '@/api/user'
import type { RoleVO } from '@/types'

const props = defineProps<{
  modelValue: boolean
  userId: number
  username: string
  roleOptions: RoleVO[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const selectedRoles = ref<number[]>([])
const submitLoading = ref(false)

// 打开时回显当前角色
watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    selectedRoles.value = []
    try {
      selectedRoles.value = await getUserRoleIds(props.userId)
    } catch {
      // 拦截器已提示
    }
  },
)

async function handleSubmit(): Promise<void> {
  submitLoading.value = true
  try {
    await assignRoles(props.userId, selectedRoles.value)
    ElMessage.success('分配角色成功')
    emit('success')
    visible.value = false
  } catch {
    // 拦截器已提示
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.role-tip {
  margin: 0 0 12px;
  color: #606266;
}
</style>
