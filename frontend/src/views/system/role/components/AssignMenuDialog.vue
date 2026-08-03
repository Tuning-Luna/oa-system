<template>
  <el-dialog v-model="visible" title="分配菜单" width="480px" :close-on-click-modal="false">
    <p class="role-tip">为角色「{{ roleName }}」分配菜单（全量替换，勾选目录将包含其子菜单）</p>
    <el-tree
      ref="menuTreeRef"
      :data="menuTree"
      show-checkbox
      node-key="id"
      :props="{ label: 'name', children: 'children' }"
      :default-expand-all="true"
      class="menu-tree"
    />
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ElTree } from 'element-plus'
import { assignMenus, getRoleMenuIds } from '@/api/role'
import { getMenuTree } from '@/api/menu'
import type { MenuVO } from '@/types'

const props = defineProps<{
  modelValue: boolean
  roleId: number
  roleName: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const menuTree = ref<MenuVO[]>([])
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const submitLoading = ref(false)

// 打开时加载菜单树（懒加载一次）并回显当前勾选
watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    try {
      if (menuTree.value.length === 0) {
        menuTree.value = await getMenuTree()
      }
      const ids = await getRoleMenuIds(props.roleId)
      nextTick(() => {
        menuTreeRef.value?.setCheckedKeys(ids)
      })
    } catch {
      // 拦截器已提示
    }
  },
)

async function handleSubmit(): Promise<void> {
  const tree = menuTreeRef.value
  if (!tree) return
  submitLoading.value = true
  try {
    // 仅提交勾选节点（含被勾选的目录/菜单，不含半选父节点）：
    // 目录(type=1)/菜单(type=2) 的 perms 为空、不参与权限聚合，仅按钮(type=3) 有权限标识；
    // 若把半选父节点一并提交，回显 setCheckedKeys 时父节点会级联误勾整棵子树。
    const checked = tree.getCheckedKeys() as number[]
    await assignMenus(props.roleId, checked)
    ElMessage.success('分配菜单成功')
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

.menu-tree {
  max-height: 400px;
  overflow-y: auto;
}
</style>
