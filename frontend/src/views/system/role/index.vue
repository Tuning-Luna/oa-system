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
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
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

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="最长 50 字符" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="字母开头，仅字母/数字/下划线，全局唯一" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填，最长 255 字符" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单弹窗 -->
    <el-dialog v-model="menuVisible" title="分配菜单" width="480px" :close-on-click-modal="false">
      <p class="role-tip">为角色「{{ currentRoleName }}」分配菜单（全量替换，勾选目录将包含其子菜单）</p>
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
        <el-button @click="menuVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuLoading" @click="handleAssignMenu">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import { assignMenus, createRole, deleteRole, getRoleMenuIds, pageRoles, updateRole } from '@/api/role'
import { getMenuTree } from '@/api/menu'
import type { MenuVO, RoleVO } from '@/types'

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

// ==================== 新增/编辑角色 ====================
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  code: '',
  description: '',
  status: 1,
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 50, message: '角色名称最长 50 字符', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{0,49}$/, message: '字母开头，仅含字母/数字/下划线，最长 50', trigger: 'blur' },
  ],
  description: [{ max: 255, message: '角色描述最长 255 字符', trigger: 'blur' }],
}

function openCreate(): void {
  isEdit.value = false
  dialogVisible.value = true
}

function openEdit(row: RoleVO): void {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.code = row.code
  form.description = row.description ?? ''
  form.status = row.status
  dialogVisible.value = true
}

function resetForm(): void {
  formRef.value?.clearValidate()
  form.id = 0
  form.name = ''
  form.code = ''
  form.description = ''
  form.status = 1
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload = {
      name: form.name,
      code: form.code,
      description: form.description || undefined,
      status: form.status,
    }
    if (isEdit.value) {
      await updateRole(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createRole(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch {
    // 失败提示（如编码重复 400）由 request 拦截器统一处理
  } finally {
    submitLoading.value = false
  }
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

// ==================== 分配菜单 ====================
const menuVisible = ref(false)
const menuLoading = ref(false)
const currentRoleId = ref(0)
const currentRoleName = ref('')
const menuTree = ref<MenuVO[]>([])
const menuTreeRef = ref<InstanceType<typeof ElTree>>()

async function openAssignMenu(row: RoleVO): Promise<void> {
  currentRoleId.value = row.id
  currentRoleName.value = row.name
  menuVisible.value = true
  try {
    // 树数据懒加载一次，后续复用
    if (menuTree.value.length === 0) {
      menuTree.value = await getMenuTree()
    }
    const ids = await getRoleMenuIds(row.id)
    // 等树渲染后回显勾选
    nextTick(() => {
      menuTreeRef.value?.setCheckedKeys(ids)
    })
  } catch {
    // 拦截器已提示
  }
}

async function handleAssignMenu(): Promise<void> {
  const tree = menuTreeRef.value
  if (!tree) return
  menuLoading.value = true
  try {
    // 仅提交勾选节点（含被勾选的目录/菜单，不含半选父节点）：
    // 目录(type=1)/菜单(type=2) 的 perms 为空、不参与权限聚合，仅按钮(type=3) 有权限标识；
    // 若把半选父节点一并提交，回显 setCheckedKeys 时父节点会级联误勾整棵子树。
    const checked = tree.getCheckedKeys() as number[]
    await assignMenus(currentRoleId.value, checked)
    ElMessage.success('分配菜单成功')
    menuVisible.value = false
  } catch {
    // 拦截器已提示
  } finally {
    menuLoading.value = false
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

  .role-tip {
    margin: 0 0 12px;
    color: #606266;
  }

  .menu-tree {
    max-height: 400px;
    overflow-y: auto;
  }
}
</style>
