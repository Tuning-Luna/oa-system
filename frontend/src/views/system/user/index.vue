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
        <el-button type="primary" v-permission="'system:user:add'" @click="openAdd">
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

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="addVisible" title="新增用户" width="520px" :close-on-click-modal="false" @closed="resetAddForm">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="3-20 位字母、数字、下划线" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" show-password placeholder="8-20 位，需含字母和数字" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="addForm.nickname" placeholder="选填，最长 50 字符" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="addForm.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addForm.roleIds" multiple placeholder="可选，注册后立即分配" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="`${role.name}（${role.code}）`" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="520px" :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="选填，最长 50 字符" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-input v-model="editForm.avatar" placeholder="选填，头像 URL" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="roleVisible" title="分配角色" width="480px" :close-on-click-modal="false">
      <p class="role-tip">为用户「{{ currentUsername }}」分配角色（全量替换）</p>
      <el-select v-model="selectedRoles" multiple placeholder="选择角色" style="width: 100%">
        <el-option v-for="role in roleOptions" :key="role.id" :label="`${role.name}（${role.code}）`" :value="role.id" />
      </el-select>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleLoading" @click="handleAssignRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { register } from '@/api/auth'
import { assignRoles, deleteUser, getUserRoleIds, pageUsers, updateUser } from '@/api/user'
import { listAllRoles } from '@/api/role'
import type { RoleVO, UserVO } from '@/types'
import { commonStatusLabel, commonStatusTag } from '@/utils/dict'
import { emailRules, nicknameRules, passwordRules, phoneRules, usernameRules } from '@/utils/validators'

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

// ==================== 角色选项 ====================
const roleOptions = ref<RoleVO[]>([])

// ==================== 新增用户 ====================
const addVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  roleIds: [] as number[],
})

const addRules: FormRules = {
  username: usernameRules,
  password: passwordRules,
  nickname: nicknameRules,
  email: emailRules,
  phone: phoneRules,
}

function openAdd(): void {
  addVisible.value = true
}

async function handleAdd(): Promise<void> {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  addLoading.value = true
  try {
    // 后端无管理员建号接口，新增复用公开注册接口 + 分配角色
    const user = await register({
      username: addForm.username,
      password: addForm.password,
      nickname: addForm.nickname || undefined,
      email: addForm.email || undefined,
      phone: addForm.phone || undefined,
    })
    if (addForm.roleIds.length > 0) {
      await assignRoles(user.id, addForm.roleIds)
    }
    ElMessage.success('新增用户成功')
    addVisible.value = false
    fetchList()
  } catch {
    // 失败提示由 request 拦截器统一处理
  } finally {
    addLoading.value = false
  }
}

function resetAddForm(): void {
  addFormRef.value?.clearValidate()
  addForm.username = ''
  addForm.password = ''
  addForm.nickname = ''
  addForm.email = ''
  addForm.phone = ''
  addForm.roleIds = []
}

// ==================== 编辑用户 ====================
const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: 0,
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  status: 1,
})

const editRules: FormRules = {
  nickname: nicknameRules,
  email: emailRules,
  phone: phoneRules,
}

function openEdit(row: UserVO): void {
  editForm.id = row.id
  editForm.nickname = row.nickname ?? ''
  editForm.email = row.email ?? ''
  editForm.phone = row.phone ?? ''
  editForm.avatar = row.avatar ?? ''
  editForm.status = row.status
  editVisible.value = true
}

async function handleEdit(): Promise<void> {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  editLoading.value = true
  try {
    await updateUser(editForm.id, {
      nickname: editForm.nickname || undefined,
      email: editForm.email || undefined,
      phone: editForm.phone || undefined,
      avatar: editForm.avatar || undefined,
      status: editForm.status,
    })
    ElMessage.success('修改成功')
    editVisible.value = false
    fetchList()
  } catch {
    // 拦截器已提示
  } finally {
    editLoading.value = false
  }
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

// ==================== 分配角色 ====================
const roleVisible = ref(false)
const roleLoading = ref(false)
const currentUserId = ref(0)
const currentUsername = ref('')
const selectedRoles = ref<number[]>([])

async function openAssignRole(row: UserVO): Promise<void> {
  currentUserId.value = row.id
  currentUsername.value = row.username
  selectedRoles.value = []
  roleVisible.value = true
  try {
    selectedRoles.value = await getUserRoleIds(row.id)
  } catch {
    // 拦截器已提示
  }
}

async function handleAssignRole(): Promise<void> {
  roleLoading.value = true
  try {
    await assignRoles(currentUserId.value, selectedRoles.value)
    ElMessage.success('分配角色成功')
    roleVisible.value = false
  } catch {
    // 拦截器已提示
  } finally {
    roleLoading.value = false
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

  .role-tip {
    margin: 0 0 12px;
    color: #606266;
  }
}
</style>
