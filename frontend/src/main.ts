import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { permission } from './directive/permission'
import '@/styles/index.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// Element Plus 全量引入 + 中文语言包
app.use(ElementPlus, { locale: zhCn })
// 按钮级权限指令
app.directive('permission', permission)

app.mount('#app')
