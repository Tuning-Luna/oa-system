<template>
  <div class="role-page">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form inline>
        <el-form-item label="角色名称">
          <el-input
            v-model="query.name"
            placeholder="名称模糊查询"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" v-permission="'system:role:add'" @click="openCreate">
          新增角色
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" min-width="120" />
        <el-table-column prop="code" label="角色编码" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="commonStatusTag(row.status)">{{ commonStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-permission="'system:role:edit'" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button link type="primary" v-permission="'system:role:assign'" @click="openAssignMenu(row)">
              分配菜单
            </el-button>
            <el-button link type="danger" v-permission="'system:role:remove'" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 弹窗组件：新增/编辑角色、分配菜单 -->
    <RoleFormDialog v-model="formVisible" :role="editingRole" @success="fetchList" />
    <AssignMenuDialog
      v-model="assignVisible"
      :role-id="currentRoleId"
      :role-name="currentRoleName"
      @success="fetchList"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteRole, pageRoles } from '@/api/role'
import type { RoleVO } from '@/types'
import { commonStatusLabel, commonStatusTag } from '@/utils/dict'
import RoleFormDialog from './components/RoleFormDialog.vue'
import AssignMenuDialog from './components/AssignMenuDialog.vue'

// ==================== 列表 ====================
const loading = ref(false)
const list = ref<RoleVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: undefined as number | undefined,
})

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const data = await pageRoles({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name || undefined,
      status: query.status,
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  fetchList()
}

function handleReset(): void {
  query.name = ''
  query.status = undefined
  handleSearch()
}

function handleSizeChange(): void {
  query.pageNum = 1
  fetchList()
}

// ==================== 弹窗状态 ====================
const formVisible = ref(false)
const editingRole = ref<RoleVO | null>(null)
const assignVisible = ref(false)
const currentRoleId = ref(0)
const currentRoleName = ref('')

function openCreate(): void {
  editingRole.value = null
  formVisible.value = true
}

function openEdit(row: RoleVO): void {
  editingRole.value = row
  formVisible.value = true
}

function openAssignMenu(row: RoleVO): void {
  currentRoleId.value = row.id
  currentRoleName.value = row.name
  assignVisible.value = true
}

// ==================== 删除角色 ====================
async function handleDelete(row: RoleVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除角色「${row.name}」吗？删除后该角色下用户的权限将实时失效。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return // 用户取消
  }
  try {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    fetchList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.role-page {
  .search-card {
    margin-bottom: 16px;
  }

  .toolbar {
    margin-bottom: 12px;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
