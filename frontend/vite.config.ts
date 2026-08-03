import { fileURLToPath, URL } from 'node:url'
import { loadEnv } from 'vite'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        // 路径别名 @ -> src
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    // 部署基础路径：默认 /（站根部署）；子路径部署时在 .env 设 VITE_BASE_PATH=/xxx/
    base: env.VITE_BASE_PATH || '/',
    server: {
      host: true,
      port: 5173,
      // 本地联调代理：/api 转发到后端 Spring Boot（8080）
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    build: {
      // Element Plus 全量引入使主 chunk 偏大，放宽告警阈值（无需拆包也满足内部使用）
      chunkSizeWarningLimit: 1024,
    },
  }
})
