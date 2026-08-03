<template>
  <el-dialog
    v-model="visible"
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
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createMenu, updateMenu } from '@/api/menu'
import type { MenuRequest, MenuVO } from '@/types'

const props = defineProps<{
  modelValue: boolean
  /** 编辑的行数据；null 表示新增 */
  menu: MenuVO | null
  /** 新增时的上级菜单 ID */
  parentId: number
  /** 菜单树（用于上级菜单选择器） */
  menuTree: MenuVO[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const isEdit = computed(() => props.menu !== null)

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
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

// 打开时填充：编辑用行数据，新增用空值 + 指定父级
watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    formRef.value?.clearValidate()
    if (props.menu) {
      form.id = props.menu.id
      form.parentId = props.menu.parentId
      form.name = props.menu.name
      form.type = props.menu.type
      form.path = props.menu.path ?? ''
      form.component = props.menu.component ?? ''
      form.icon = props.menu.icon ?? ''
      form.perms = props.menu.perms ?? ''
      form.sort = props.menu.sort ?? 0
      form.status = props.menu.status
    } else {
      form.id = 0
      form.parentId = props.parentId
      form.name = ''
      form.type = 2
      form.path = ''
      form.component = ''
      form.icon = ''
      form.perms = ''
      form.sort = 0
      form.status = 1
    }
  },
)

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

/** 上级菜单选择器数据（根节点 0 + 树；按钮不可作为父级） */
const parentOptions = computed(() => {
  const nodes = isEdit.value ? excludeSubtree(props.menuTree, form.id) : props.menuTree
  return [
    {
      label: '根节点',
      value: 0,
      children: nodes.map((n) => toSelectNode(n)),
    },
  ]
})

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
    emit('success')
    visible.value = false
  } catch {
    // 失败提示（如父菜单是自身）由 request 拦截器统一处理
  } finally {
    submitLoading.value = false
  }
}
</script>
