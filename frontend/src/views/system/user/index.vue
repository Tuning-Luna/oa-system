<template>
  <div class="user-page">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form inline>
        <el-form-item label="用户名">
          <el-input
            v-model="query.username"
            placeholder="用户名模糊查询"
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
        <el-button type="primary" v-permission="'system:user:add'" @click="addVisible = true">
          新增用户
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="commonStatusTag(row.status)">{{ commonStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-permission="'system:user:edit'" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button link type="primary" v-permission="'system:user:edit'" @click="openAssignRole(row)">
              分配角色
            </el-button>
            <el-button link type="danger" v-permission="'system:user:remove'" @click="handleDelete(row)">
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

    <!-- 弹窗组件：新增 / 编辑 / 分配角色 -->
    <AddUserDialog v-model="addVisible" :role-options="roleOptions" @success="fetchList" />
    <EditUserDialog v-model="editVisible" :user="editingUser" @success="fetchList" />
    <AssignRoleDialog
      v-model="roleVisible"
      :user-id="currentUserId"
      :username="currentUsername"
      :role-options="roleOptions"
      @success="fetchList"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteUser, pageUsers } from '@/api/user'
import { listAllRoles } from '@/api/role'
import type { RoleVO, UserVO } from '@/types'
import { commonStatusLabel, commonStatusTag } from '@/utils/dict'
import AddUserDialog from './components/AddUserDialog.vue'
import EditUserDialog from './components/EditUserDialog.vue'
import AssignRoleDialog from './components/AssignRoleDialog.vue'

// ==================== 列表 ====================
const loading = ref(false)
const list = ref<UserVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  status: undefined as number | undefined,
})

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const data = await pageUsers({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      username: query.username || undefined,
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
  query.username = ''
  query.status = undefined
  handleSearch()
}

function handleSizeChange(): void {
  query.pageNum = 1
  fetchList()
}

// ==================== 角色选项（新增/分配角色共用） ====================
const roleOptions = ref<RoleVO[]>([])

// ==================== 弹窗状态 ====================
const addVisible = ref(false)
const editVisible = ref(false)
const roleVisible = ref(false)
const editingUser = ref<UserVO | null>(null)
const currentUserId = ref(0)
const currentUsername = ref('')

function openEdit(row: UserVO): void {
  editingUser.value = row
  editVisible.value = true
}

function openAssignRole(row: UserVO): void {
  currentUserId.value = row.id
  currentUsername.value = row.username
  roleVisible.value = true
}

// ==================== 删除用户 ====================
async function handleDelete(row: UserVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return // 用户取消
  }
  try {
    await deleteUser(row.id)
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
  // 加载角色下拉选项（需 system:role:list；无权限时由拦截器提示，不阻塞列表）
  listAllRoles()
    .then((roles) => {
      roleOptions.value = roles
    })
    .catch(() => {})
})
</script>

<style scoped lang="scss">
.user-page {
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
