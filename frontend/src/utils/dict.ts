/**
 * 公共业务字典
 * 集中维护下拉/表格/标签的展示映射，避免各页面散落硬编码。
 * 后续阶段（F7/F10）继续扩展：报销类别、菜单类型、通知类型等。
 */

/** Element Plus tag 类型 */
export type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

/** 审批状态：0 草稿 / 1 待审批 / 2 通过 / 3 拒绝 / 4 撤回 */
export interface StatusDictItem {
  label: string
  tagType: TagType
}

export const approvalStatusDict: Record<number, StatusDictItem> = {
  0: { label: '草稿', tagType: 'info' },
  1: { label: '待审批', tagType: 'warning' },
  2: { label: '已通过', tagType: 'success' },
  3: { label: '已拒绝', tagType: 'danger' },
  4: { label: '已撤回', tagType: 'info' },
}

/** 请假类型：1 年假 / 2 事假 / 3 病假 */
export const leaveTypeDict: Record<number, string> = {
  1: '年假',
  2: '事假',
  3: '病假',
}

/** 审批动作：1 提交 / 2 通过 / 3 拒绝 / 4 撤回 */
export const approvalActionDict: Record<number, StatusDictItem> = {
  1: { label: '提交', tagType: 'info' },
  2: { label: '通过', tagType: 'success' },
  3: { label: '拒绝', tagType: 'danger' },
  4: { label: '撤回', tagType: 'info' },
}

/** 审批状态筛选下拉选项（全部 + 各状态） */
export const approvalStatusOptions = [
  { label: '全部', value: undefined as number | undefined },
  ...Object.entries(approvalStatusDict).map(([value, item]) => ({
    label: item.label,
    value: Number(value),
  })),
]

/** 通知类型 tag 映射（后端 type 为字符串，如 APPROVAL） */
export const messageTypeDict: Record<string, { label: string; tagType: TagType }> = {
  APPROVAL: { label: '审批通知', tagType: 'primary' },
}

/** 通知类型名称（未知类型原样返回） */
export function messageTypeLabel(type: string): string {
  return messageTypeDict[type]?.label ?? type
}

/** 通知类型 tag 颜色 */
export function messageTypeTag(type: string): TagType {
  return messageTypeDict[type]?.tagType ?? 'info'
}

/** 文件扩展名 → 友好类型名（按扩展名映射，未知为「其他」） */
const FILE_EXT_LABEL: Record<string, string> = {
  jpg: '图片', jpeg: '图片', png: '图片', gif: '图片',
  pdf: 'PDF',
  doc: '文档', docx: '文档',
  xls: '表格', xlsx: '表格', csv: '表格',
  txt: '文本',
}

/** 文件扩展名 → tag 颜色 */
const FILE_EXT_TAG: Record<string, TagType> = {
  jpg: 'success', jpeg: 'success', png: 'success', gif: 'success',
  pdf: 'danger',
  doc: 'primary', docx: 'primary',
  xls: 'warning', xlsx: 'warning', csv: 'warning',
  txt: 'info',
}

function fileExtension(name: string): string {
  const dot = name.lastIndexOf('.')
  return (dot < 0 ? name : name.slice(dot + 1)).toLowerCase()
}

/** 文件类型展示名 */
export function fileTypeLabel(name: string): string {
  return FILE_EXT_LABEL[fileExtension(name)] ?? '其他'
}

/** 文件类型 tag 颜色 */
export function fileTypeTag(name: string): TagType {
  return FILE_EXT_TAG[fileExtension(name)] ?? 'info'
}
