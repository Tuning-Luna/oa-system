<template>
  <div class="menu-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" v-permission="'system:menu:add'" @click="openCreate(0)">
          新增菜单
        </el-button>
      </div>

      <!-- 菜单树表格 -->
      <el-table
        v-loading="loading"
        :data="menuTree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        border
      >
        <el-table-column prop="name" label="菜单名称" min-width="200" />
        <el-table-column prop="icon" label="图标" width="90" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.type)">{{ menuTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="160" show-overflow-tooltip />
        <el-table-column prop="perms" label="权限标识" min-width="150" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="commonStatusTag(row.status)">{{ commonStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-permission="'system:menu:add'" @click="openCreate(row.id)">
              新增
            </el-button>
            <el-button link type="primary" v-permission="'system:menu:edit'" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" v-permission="'system:menu:remove'" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑菜单弹窗 -->
    <MenuFormDialog
      v-model="formVisible"
      :menu="editingMenu"
      :parent-id="createParentId"
      :menu-tree="menuTree"
      @success="fetchTree"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteMenu, getMenuTree } from '@/api/menu'
import type { MenuVO } from '@/types'
import { commonStatusLabel, commonStatusTag, menuTypeLabel, menuTypeTag } from '@/utils/dict'
import MenuFormDialog from './components/MenuFormDialog.vue'

// ==================== 菜单树 ====================
const loading = ref(false)
const menuTree = ref<MenuVO[]>([])

async function fetchTree(): Promise<void> {
  loading.value = true
  try {
    menuTree.value = await getMenuTree()
  } finally {
    loading.value = false
  }
}

// ==================== 新增/编辑 ====================
const formVisible = ref(false)
const editingMenu = ref<MenuVO | null>(null)
const createParentId = ref(0)

function openCreate(parentId: number): void {
  editingMenu.value = null
  createParentId.value = parentId
  formVisible.value = true
}

function openEdit(row: MenuVO): void {
  editingMenu.value = row
  formVisible.value = true
}

// ==================== 删除 ====================
async function handleDelete(row: MenuVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除菜单「${row.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return // 用户取消
  }
  try {
    await deleteMenu(row.id)
    ElMessage.success('删除成功')
    fetchTree()
  } catch {
    // 存在子菜单被后端拒绝时，拦截器已提示
  }
}

onMounted(() => {
  fetchTree()
})
</script>

<style scoped lang="scss">
.menu-page {
  .toolbar {
    margin-bottom: 12px;
  }
}
</style>
