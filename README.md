# 企业协同办公管理系统（OA System）

一个前后端分离的企业协同办公管理系统，覆盖用户认证、RBAC 权限、请假/报销审批、异步通知与文件管理，可作为中小型企业内部办公协作平台或学习参考项目。

## 技术栈

### 后端

| 分类 | 技术 |
|---|---|
| 语言 | Java 17（兼容 JDK 21 运行） |
| 框架 | Spring Boot 4.1.0（Spring MVC） |
| 安全 | Spring Security 7 + JWT（jjwt 0.12.6），BCrypt 密码加密 |
| ORM | MyBatis-Plus 3.5.16（Spring Boot 4 专用 starter）+ 分页插件 |
| 数据库 | MySQL 8 |
| 缓存 | Redis（Spring Data Redis + commons-pool2 连接池） |
| 消息队列 | RabbitMQ（Spring AMQP） |
| 接口文档 | springdoc-openapi 3.0.3（Swagger UI） |
| 校验 / 工具 | Spring Validation、Lombok |
| 测试 | JUnit 5 + Spring Boot Test |

### 前端

| 分类 | 技术 |
|---|---|
| 框架 | Vue 3.5 + TypeScript 6.0 |
| 构建 | Vite 8 + vue-tsc |
| 路由 / 状态 | Vue Router 4、Pinia 4 |
| UI | Element Plus 2.14 + @element-plus/icons-vue |
| HTTP | Axios |
| 样式 | Sass |

## 已实现功能

### 认证与授权
- 用户注册 / 登录，JWT 无状态认证（Token 有效期 30 天），BCrypt 密码加密
- 接口级鉴权：`@PreAuthorize` 权限标识控制，401 / 403 统一返回
- 前端动态路由（按权限注入菜单）+ `v-permission` 按钮级权限指令

### RBAC 权限管理
- **用户管理**：分页查询、新增 / 编辑 / 删除（逻辑删除）、分配角色
- **角色管理**：CRUD、角色分配菜单（关联 `sys_role_menu`）
- **菜单管理**：树形菜单 CRUD（目录 / 菜单 / 按钮三种类型，绑定权限标识）
- 当前用户信息：聚合用户 → 角色 → 权限返回 `roles` + `permissions`

### 审批流程（请假 / 报销）
- 请假、报销两类业务：提交申请、我的申请列表、待我审批列表、审批操作、撤回、审批记录
- 状态机流转：`DRAFT → PENDING → APPROVED / REJECTED`，草稿 / 待审批可撤回 → `CANCELLED`，非法流转抛业务异常
- 单级审批（申请人指定审批人），多级审批预留扩展点

### 通知中心（RabbitMQ 异步消息）
- 审批通过 / 拒绝后通过 MQ 异步发送消息，消费者落库生成系统通知
- 通知分页查询、标记已读、未读计数

### Redis 缓存
- 登录态（token → 用户）缓存，登出即失效
- 用户信息、菜单树热点数据缓存，写后失效策略

### 文件管理
- 文件上传 / 下载 / 分页列表 / 删除
- 本地磁盘存储（按日期分目录），`StorageService` 接口预留 MinIO 扩展

### 公共能力
- 统一返回结构 `Result<T>` 与分页对象 `PageResult<T>`
- 统一异常处理（业务异常、参数校验、认证 / 鉴权、兜底）
- 全局字段自动填充（`create_time` / `update_time`）、逻辑删除
- Swagger UI 在线接口文档

## 规划中（未实现）
- Elasticsearch 员工 / 文件全文搜索（阶段 8）
- 微服务拆分（远期，当前保持单体）

## 快速开始

```bash
# 后端（需本机 MySQL、Redis、RabbitMQ 已启动，连接信息见 application-dev.yml）
.\mvnw.cmd spring-boot:run

# 前端
cd frontend
npm install
npm run dev
```

- 后端地址：`http://localhost:8080`，Swagger：`http://localhost:8080/swagger-ui.html`
- 前端地址：`http://localhost:5173`
- 测试账号：`admin` / `Admin@123456`（管理员，详见 `测试账号.md`）
