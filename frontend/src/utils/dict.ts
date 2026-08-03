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
