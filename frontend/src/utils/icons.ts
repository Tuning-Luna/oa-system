/**
 * 路由 meta.icon 字符串 → Element Plus 图标组件 映射
 * （按需映射本系统用到的图标，避免全量注册图标导致的体积膨胀）
 */
import {
  Bell,
  Document,
  Folder,
  HomeFilled,
  Menu,
  Setting,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

export const iconMap: Record<string, Component> = {
  home: HomeFilled,
  document: Document,
  bell: Bell,
  folder: Folder,
  setting: Setting,
  user: User,
  role: UserFilled,
  menu: Menu,
}

/** 解析图标；未配置或未知图标时回退到首页图标 */
export function resolveIcon(name?: string): Component {
  return (name && iconMap[name]) || HomeFilled
}
