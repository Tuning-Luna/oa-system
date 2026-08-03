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
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑菜单' : '新增菜单'"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            default-expand-all
            node-key="value"
            placeholder="选择上级菜单（根节点为 0）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="最长 50 字符" />
        </el-form-item>
        <el-form-item label="路由地址" prop="path">
          <el-input v-model="form.path" placeholder="如 /system/user（目录/菜单）" />
        </el-form-item>
        <el-form-item label="组件路径" prop="component">
          <el-input v-model="form.component" placeholder="如 system/user/index（目录/菜单）" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="图标名，如 user" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input v-model="form.perms" placeholder="如 system:user:list（按钮必填）" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createMenu, deleteMenu, getMenuTree, updateMenu } from '@/api/menu'
import type { MenuRequest, MenuVO } from '@/types'
import { commonStatusLabel, commonStatusTag, menuTypeLabel, menuTypeTag } from '@/utils/dict'

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
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  parentId: 0,
  name: '',
  type: 2,
  path: '',
  component: '',
  icon: '',
  perms: '',
  sort: 0,
  status: 1,
})

const rules = computed<FormRules>(() => ({
  parentId: [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  name: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' },
    { max: 50, message: '菜单名称最长 50 字符', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  // 按钮必填权限标识；目录/菜单可不填
  perms: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (form.type === 3 && !value) {
          return callback(new Error('按钮必须填写权限标识'))
        }
        if (value && value.length > 100) {
          return callback(new Error('权限标识最长 100 字符'))
        }
        return callback()
      },
      trigger: 'blur',
    },
  ],
  path: [{ max: 255, message: '路由地址最长 255 字符', trigger: 'blur' }],
  component: [{ max: 255, message: '组件路径最长 255 字符', trigger: 'blur' }],
  icon: [{ max: 50, message: '图标最长 50 字符', trigger: 'blur' }],
}))

function openCreate(parentId: number): void {
  isEdit.value = false
  form.id = 0
  form.parentId = parentId
  form.name = ''
  form.type = 2
  form.path = ''
  form.component = ''
  form.icon = ''
  form.perms = ''
  form.sort = 0
  form.status = 1
  dialogVisible.value = true
}

function openEdit(row: MenuVO): void {
  isEdit.value = true
  form.id = row.id
  form.parentId = row.parentId
  form.name = row.name
  form.type = row.type
  form.path = row.path ?? ''
  form.component = row.component ?? ''
  form.icon = row.icon ?? ''
  form.perms = row.perms ?? ''
  form.sort = row.sort ?? 0
  form.status = row.status
  dialogVisible.value = true
}

function resetForm(): void {
  formRef.value?.clearValidate()
}

/** 排除某节点及其整棵子树（编辑时防止选自身/后代为父，避免成环） */
function excludeSubtree(nodes: MenuVO[], targetId: number): MenuVO[] {
  const result: MenuVO[] = []
  for (const node of nodes) {
    if (node.id === targetId) continue
    result.push(node.children?.length ? { ...node, children: excludeSubtree(node.children, targetId) } : node)
  }
  return result
}

/** 上级菜单选择器数据（根节点 0 + 树；按钮不可作为父级） */
const parentOptions = computed(() => {
  const nodes = isEdit.value ? excludeSubtree(menuTree.value, form.id) : menuTree.value
  return [
    {
      label: '根节点',
      value: 0,
      children: nodes.map((n) => toSelectNode(n)),
    },
  ]
})

interface SelectNode {
  label: string
  value: number
  disabled?: boolean
  children?: SelectNode[]
}

function toSelectNode(node: MenuVO): SelectNode {
  return {
    label: node.name,
    value: node.id,
    disabled: node.type === 3, // 按钮不可作为父级
    children: node.children?.length ? node.children.map(toSelectNode) : undefined,
  }
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload: MenuRequest = {
      parentId: form.parentId,
      name: form.name,
      type: form.type,
      path: form.path || undefined,
      component: form.component || undefined,
      icon: form.icon || undefined,
      perms: form.perms || undefined,
      sort: form.sort,
      status: form.status,
    }
    if (isEdit.value) {
      await updateMenu(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createMenu(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchTree()
  } catch {
    // 失败提示（如父菜单是自身）由 request 拦截器统一处理
  } finally {
    submitLoading.value = false
  }
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
