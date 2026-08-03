/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** API 基础路径（dev: /api 走 vite 代理） */
  readonly VITE_API_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
