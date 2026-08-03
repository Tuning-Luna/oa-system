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

/** 文件大小格式化：B / KB / MB / GB（>=100 不保留小数，否则 1 位） */
export function formatSize(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined || bytes < 0) {
    return '-'
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }
  const units = ['KB', 'MB', 'GB', 'TB']
  let value = bytes
  let index = -1
  do {
    value /= 1024
    index += 1
  } while (value >= 1024 && index < units.length - 1)
  const fixed = value >= 100 ? 0 : 1
  return `${value.toFixed(fixed)} ${units[index]}`
}
