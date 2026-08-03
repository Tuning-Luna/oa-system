/**
 * 展示格式化工具
 * 后续阶段（F9 文件大小、F10 统一）继续扩展。
 */

/** 金额千分位 + 两位小数：500 → "500.00"，12345.6 → "12,345.60" */
export function formatMoney(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '-'
  }
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}
