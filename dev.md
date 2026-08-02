# 企业协同办公管理系统（OA System）开发计划

> 本文档依据 `prompt.md` 的要求制定，是后续所有开发阶段的执行依据。
> 开发规则（来自 prompt.md，必须遵守）：
> 1. 不一次性生成完整项目，按阶段拆分开发。
> 2. 每完成一个阶段，必须：编译项目 → 运行自动化测试 → 修复所有错误 → 确认功能正常 → 生成对应 API 接口文档 → **停止并等待用户确认**。
> 3. 遇到需求歧义 / 技术方案不确定 / 业务规则不确定时，先提问，不自行猜测。
> 4. 涉及框架版本、官方推荐方案等变化时，先查询官方文档确认。
> 5. 代码符合企业规范：清晰分层、高内聚低耦合、避免过度设计、不生成无意义代码。

---

## 1. 项目状态检查（阶段 0 已完成）

### 1.1 当前项目状态

| 检查项 | 结果 |
|---|---|
| 项目类型 | 空 Spring Boot 脚手架（Spring Initializr 生成） |
| Spring Boot 版本 | **4.1.0**（2025-11 发布的大版本） |
| Java 目标版本 | pom 中 `java.version = 17` |
| 本机 JDK | **21.0.2 LTS**（兼容：以 17 为目标编译，在 21 上运行） |
| 包名 | `com.tuning.oa_system`（含下划线，**已确认改为 `com.tuning.oasystem`**） |
| 已有依赖 | 仅 `spring-boot-starter-webmvc` + `spring-boot-starter-webmvc-test` |
| 已有代码 | `OaSystemApplication`、`OaSystemApplicationTests`（contextLoads） |
| 资源文件 | `application.properties`（仅 `spring.application.name=oa-system`），static/templates 为空 |
| 构建方式 | 项目自带 Maven Wrapper（`.\mvnw.cmd`），**开发统一用它** |
| 测试框架 | JUnit 5 + Spring Boot Test（`@SpringBootTest` 已可用） |
| 中间件 | 本机已配置 **MySQL + Redis**（未用 Docker）；**RabbitMQ、Elasticsearch 本机未安装** |

### 1.2 已确认的技术决策（用户已答复）

1. **保留 Spring Boot 4.1.0**，不降级。
2. **包名改为 `com.tuning.oasystem`**（去掉下划线），在阶段 1 一次性调整。
3. **中间件用本机安装**（MySQL + Redis 已有），暂不使用 Docker；RabbitMQ / ES 到对应阶段前再安装。

### 1.3 版本兼容性调研结论（官方文档核实）

> Spring Boot 4 为破坏性大版本（starter 名称、部分 API 变化），依赖选型需匹配 Spring Boot 4。

| 组件 | 选型 | 兼容说明 |
|---|---|---|
| ORM | `mybatis-plus-spring-boot4-starter:3.5.16` | MyBatis-Plus 自 3.5.13 起官方提供 Spring Boot 4 starter，BOM 从 3.5.14 支持 |
| 接口文档 | `springdoc-openapi-starter-webmvc-ui:3.0.3` | springdoc v3.0.0 起官方支持 Spring Boot 4 |
| 分页依赖 | `mybatis-plus-jsqlparser` | MyBatis-Plus 3.5.9+ 分页插件需单独引入（与 starter 同版本） |
| 数据库驱动 | `com.mysql:mysql-connector-j`（runtime） | 版本由 Spring Boot BOM 管理 |
| 安全 | `spring-boot-starter-security` | Spring Boot 官方 |
| JWT | `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.6` | 行业标准库 |
| 校验 | `spring-boot-starter-validation` | 官方 |
| 缓存 | `spring-boot-starter-data-redis` | 官方（阶段 4 引入） |
| 消息队列 | `spring-boot-starter-amqp` | 官方（阶段 6 引入） |
| 搜索 | `spring-boot-starter-data-elasticsearch` | 官方（阶段 8 引入，客户端版本需与 ES 服务端匹配，届时核实） |
| 开发效率 | `lombok` | 减少样板代码（版本由 BOM 管理） |

> 注意：Spring Boot 4 中 starter 名为 `spring-boot-starter-webmvc`（不再是 3.x 的 `spring-boot-starter-web`），本项目 pom 已是新命名，无需改动。

### 1.4 阶段 1 实测发现的 Spring Boot 4 差异（后续阶段注意）

1. **`@WebMvcTest` 包路径变化**：Boot 4 中位于 `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`（新模块 `spring-boot-webmvc-test`），不再是 3.x 的 `org.springframework.boot.test.autoconfigure.web.servlet`。
2. **内置 Jackson 3**：Spring Boot 4 默认使用 Jackson 3（`tools.jackson.*` 包，非 `com.fasterxml.*`）。`spring.jackson.serialization.write-dates-as-timestamps` 在 Jackson 3 中无此枚举，勿配置；测试如需对齐运行时序列化，用 `tools.jackson.databind.ObjectMapper`。
3. **`@WebMvcTest` 不注册嵌套内部类控制器**：测试用 `@RestController` 必须是顶层类（本项目的 `TestExceptionController` 即因此为顶层类）。

---

## 2. 整体架构与代码规范

### 2.1 包结构（统一为 `com.tuning.oasystem`）

```
com.tuning.oasystem
├── OaSystemApplication.java
├── common/          # 统一返回、错误码、分页对象
├── config/          # 全局配置（OpenAPI、MyBatis-Plus、Redis、Security、MQ...）
├── controller/      # 接口层，只做参数接收与响应，不写业务逻辑
├── service/         # 业务接口
│   └── impl/        # 业务实现
├── mapper/          # MyBatis-Plus Mapper，数据访问
├── entity/          # 数据库实体
├── dto/             # 接收前端参数
├── vo/              # 返回给前端数据
├── security/        # JWT、认证过滤器、登录用户上下文
├── exception/       # 业务异常 + 全局异常处理
├── enums/           # 状态机、业务类型等枚举
├── utils/           # 通用工具
└── producer/ consumer/  # MQ 消息生产/消费（阶段 6）
```

### 2.2 统一返回格式

所有接口返回 `Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- `code`：业务状态码（200 成功；其余见 `ErrorCode` 枚举）。
- 分页接口返回 `Result<PageResult<T>>`，`PageResult` 含 `total / records / pageNum / pageSize`。

### 2.3 统一异常处理

- `BusinessException`：业务异常（携带错误码 + 消息），Controller/Service 直接抛出。
- `GlobalExceptionHandler`（`@RestControllerAdvice`）统一捕获：业务异常、参数校验异常、认证/鉴权异常、兜底异常，输出统一格式。
- 认证失败返回 401，无权限返回 403，参数错误返回 400。

### 2.4 数据库设计规范

- 每张表含公共字段：`id`（BIGINT 自增主键）、`create_time`、`update_time`、`deleted`（逻辑删除，MyBatis-Plus `@TableLogic`）。
- `create_time / update_time` 用 MyBatis-Plus `MetaObjectHandler` 自动填充。
- 布尔/状态用 `TINYINT`，金额用 `DECIMAL`，编码/枚举字段存代码值。
- 初始化脚本统一放 `src/main/resources/db/`（按阶段编号，如 `V2__sys_user.sql`）。

### 2.5 接口文档规范

- 使用 springdoc-openapi 自动生成 Swagger UI。
- 每个 Controller 类加 `@Tag`，每个接口加 `@Operation`（描述）、`@Parameter`（参数说明）、`@ApiResponse`（返回说明）。
- 安全接口在 OpenAPI 中声明 JWT Bearer 认证（阶段 2 起）。

---

## 3. 推荐执行顺序

| 执行序号 | 阶段 | 内容 | 前置条件 |
|---|---|---|---|
| 1 | 阶段 0 | 项目检查（已完成） | — |
| 2 | 阶段 1 | 项目基础架构 | 阶段 0 |
| 3 | 阶段 2 | 用户模块（登录/注册/用户查询） | 阶段 1 + **MySQL 启动** |
| 4 | 阶段 3 | 权限模块（RBAC） | 阶段 2 |
| 5 | 阶段 5 | 审批流程模块（请假/报销，状态机） | 阶段 2/3 |
| 6 | 阶段 4 | Redis 接入（缓存） | 阶段 2/3 + **Redis 启动** |
| 7 | 阶段 6 | RabbitMQ 消息模块 | 阶段 5 + **安装 RabbitMQ** |
| 8 | 阶段 7 | 文件管理模块 | 阶段 2 |
| 9 | 阶段 8 | Elasticsearch 搜索模块 | 阶段 7 + **安装 ES** |
| 10 | 阶段 9 | 微服务拆分（远期，当前不拆） | 单体稳定后 |

> 说明：按 prompt.md 末尾建议，先跑通业务（登录/RBAC → 审批），再接入 Redis/MQ 等中间件，最后考虑微服务，降低前期调试成本。

---

## 4. 分阶段详细计划

### 阶段 1：项目基础架构

**目标**：搭好工程地基，后续业务模块在此之上直接叠加。

**前置**：无。

**步骤**：

1. **调整包名**：`com.tuning.oa_system` → `com.tuning.oasystem`（移动 `OaSystemApplication`、测试类，更新 `@SpringBootApplication` 扫描）。
2. **整理 Maven 依赖**（修改 `pom.xml`）：
   - 新增：`springdoc-openapi-starter-webmvc-ui:3.0.3`、`spring-boot-starter-validation`、`lombok`。
   - 数据库 / 安全 / Redis / MQ / ES 依赖**按各自阶段引入**，本阶段不加入（保持测试无需外部服务即可运行）。
3. **配置 `application.yml`**（替换 `application.properties`）：
   - 服务端口、应用名、Jackson 时区与 `LocalDateTime` 序列化格式。
   - 引入 `application-dev.yml` profile，预留 MySQL / Redis 连接配置（值待用户提供，本阶段不激活使用）。
   - springdoc 基础配置（`springdoc.api-docs.enabled`、默认分组）。
4. **实现基础类**：
   - `common/Result`（统一返回）、`common/ResultCode` 或 `common/ErrorCode`（统一错误码枚举）、`common/PageResult`（分页返回）。
   - `exception/BusinessException`、`exception/GlobalExceptionHandler`（`@RestControllerAdvice`）。
   - `config/OpenApiConfig`：OpenAPI 元信息（标题/版本/描述），预置 JWT `SecurityScheme`（阶段 2 生效）。
   - `controller/HealthController`：`GET /api/health` 返回 `Result`，作为冒烟接口与文档示例。
5. **基础测试**：
   - `contextLoads`（已有）。
   - `HealthControllerTest`：`@WebMvcTest` 验证 `GET /api/health` 返回 `{code:200,message:success}`。
   - `GlobalExceptionHandlerTest`：抛出 `BusinessException`，断言统一错误响应结构。
   - `ResultTest`：断言 JSON 序列化结构 `{code,message,data}`。

**完成标准（Checkpoint）**：
- [ ] `.\mvnw.cmd clean test` 全部通过（不依赖任何外部服务）。
- [ ] `.\mvnw.cmd spring-boot:run` 启动成功。
- [ ] 访问 `http://localhost:8080/swagger-ui/index.html` 能看到接口文档（至少含 Health 接口）；`/v3/api-docs` 返回 JSON。
- [ ] 输出阶段报告（完成内容/修改文件/测试方法/接口调用方式/下一步），**停止等待确认**。

---

### 阶段 2：用户模块

**目标**：用户登录、注册、用户查询（含密码加密、参数校验、JWT 签发）。

**前置**：阶段 1 完成；**启动本机 MySQL 并提供连接信息**（host/port/账号/密码，填入 `application-dev.yml`）。

**步骤**：

1. **添加依赖**：`mybatis-plus-spring-boot4-starter:3.5.16`、`mybatis-plus-jsqlparser`（同版本）、`com.mysql:mysql-connector-j`（runtime）、`spring-boot-starter-security`、`jjwt-api/impl/jackson:0.12.6`。
2. **数据库设计**（`db/V2__sys_user.sql`）：

   ```sql
   CREATE TABLE sys_user (
     id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
     username    VARCHAR(50)  NOT NULL COMMENT '登录名',
     password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
     nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
     email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
     phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
     avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
     status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
     create_time DATETIME     DEFAULT NULL,
     update_time DATETIME     DEFAULT NULL,
     deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
     PRIMARY KEY (id),
     UNIQUE KEY uk_username (username)
   ) COMMENT '用户表';
   ```

3. **配置**：`application-dev.yml` 数据源（MySQL）；`config/MybatisPlusConfig`（`PaginationInnerInterceptor` 分页插件 + `MetaObjectHandler` 自动填充 create_time/update_time）；mybatis-plus 逻辑删除、驼峰映射配置。
4. **实体与 Mapper**：`entity/SysUser`（`@TableName("sys_user")`、`@TableLogic`、`@TableField(fill=...)`）、`mapper/SysUserMapper extends BaseMapper<SysUser>`。
5. **安全与 JWT**：
   - `config/SecurityConfig`：`SecurityFilterChain`，放行 `/api/auth/**`、swagger 路径、`/api/health`；其余需认证；`SessionCreationPolicy.STATELESS`。
   - `security/JwtTokenProvider`：生成 / 解析 / 校验 JWT（含过期时间）。
   - `security/JwtAuthenticationFilter`：`OncePerRequestFilter`，解析 token 并写入 `SecurityContext`。
   - `PasswordEncoder` Bean：`BCryptPasswordEncoder`（密码加密）。
6. **业务与接口**：
   - `AuthController`：`POST /api/auth/register`（参数校验：用户名非空/长度、密码强度、手机/邮箱格式，用户名重复校验）、`POST /api/auth/login`（校验密码 → 签发 JWT → 返回 `LoginResponse`：token + 用户信息）。
   - `UserController`：`GET /api/users`（分页 + 用户名/状态筛选）、`GET /api/users/{id}`（详情）、`PUT /api/users/{id}`（更新资料）、`DELETE /api/users/{id}`（禁用/逻辑删除）。
   - `dto/RegisterRequest`、`LoginRequest`、`UserQuery`；`vo/LoginResponse`、`UserVO`（不回传 password）。
7. **测试**（需 MySQL 运行）：
   - `AuthServiceTest`：注册密码密文非明文、重复用户名异常；登录成功签发 token、密码错误抛异常。
   - `AuthControllerTest`：`@SpringBootTest + MockMvc`，注册→登录→带 token 访问受保护接口 200、无 token 401。
   - `UserServiceTest`：分页查询、状态/关键字筛选。

**完成标准（Checkpoint）**：
- [x] 编译 + 全部测试通过（MySQL 启动状态下）。
- [x] Swagger 中可见登录/注册/用户查询接口（带描述、参数、返回说明）。
- [x] 输出阶段报告，**停止等待确认**。

---

### 阶段 3：权限模块（RBAC）

**目标**：用户 / 角色 / 权限 / 菜单四类资源，实现登录用户权限查询与接口鉴权。

**前置**：阶段 2。

**步骤**：

1. **数据库设计**（`db/V3__rbac.sql`）：

   ```sql
   CREATE TABLE sys_role (
     id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(50), code VARCHAR(50) UNIQUE,
     description VARCHAR(255), status TINYINT DEFAULT 1,
     create_time DATETIME, update_time DATETIME, deleted TINYINT DEFAULT 0, PRIMARY KEY (id)
   ) COMMENT '角色表';

   CREATE TABLE sys_menu (
     id BIGINT NOT NULL AUTO_INCREMENT, parent_id BIGINT DEFAULT 0,
     name VARCHAR(50), path VARCHAR(255), component VARCHAR(255), icon VARCHAR(50),
     type TINYINT COMMENT '1目录 2菜单 3按钮',
     perms VARCHAR(100) COMMENT '权限标识，如 system:user:list',
     sort INT DEFAULT 0, status TINYINT DEFAULT 1,
     create_time DATETIME, update_time DATETIME, deleted TINYINT DEFAULT 0, PRIMARY KEY (id)
   ) COMMENT '菜单/权限表';

   CREATE TABLE sys_user_role (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, role_id BIGINT);
   CREATE TABLE sys_role_menu (id BIGINT AUTO_INCREMENT PRIMARY KEY, role_id BIGINT, menu_id BIGINT);
   ```

2. **实体与 Mapper**：`SysRole`、`SysMenu`、`SysUserRole`、`SysRoleMenu` 及对应 Mapper。
3. **服务层**：
   - `RoleService`：角色 CRUD + 分配菜单（`role_menu` 关联维护）。
   - `MenuService`：菜单树查询（`parentId` 组装）、CRUD。
4. **登录用户权限查询**：`GET /api/auth/me`（或 `GET /api/user/info`）→ 返回 `UserInfoVO`：用户信息 + `roles`（角色编码）+ `permissions`（权限标识集合，由 `user → role → menu(perms)` 聚合）。
5. **接口鉴权**：启用 `@EnableMethodSecurity`，在接口上使用 `@PreAuthorize("hasAuthority('system:user:list')")`；`UserDetailsService` 从 DB 加载用户与权限，JWT 过滤器把权限写入认证上下文。
6. **初始化数据**：`db/V3__init_admin.sql` —— 内置 `admin` 角色、管理员账号（BCrypt 密码）、基础菜单权限。
7. **测试**：
   - `RoleServiceTest`：角色增删改、分配菜单。
   - 权限测试：无权限角色访问受保护接口返回 403；管理员可访问。
   - `GET /api/auth/me` 返回权限集合正确。

**完成标准（Checkpoint）**：
- [x] 编译 + 测试通过（35 个用例，含 RBAC 权限测试）。
- [x] Swagger 覆盖角色管理、菜单管理、当前用户信息接口。
- [x] 输出阶段报告，**停止等待确认**。

---

### 阶段 5：审批流程模块（请假 / 报销）

> 说明：按推荐顺序在 Redis（阶段 4）之前执行，先跑通核心业务。

**目标**：请假、报销两类审批；含申请、审批记录、状态流转（合理状态机）。

**前置**：阶段 2/3 完成（需登录用户身份）。

**步骤**：

1. **确认审批深度**（**Checkpoint 问题**）：默认**单级审批**（申请人提交 → 指定审批人通过/拒绝）；多级审批预留扩展点，不提前实现。
2. **状态机设计**：

   ```
   状态：DRAFT(草稿) → PENDING(待审批) → APPROVED(通过)
                          ↘ REJECTED(拒绝)
   DRAFT / PENDING 可执行：撤回 → CANCELLED(已撤回)

   合法流转表（动作 × 当前状态 → 新状态）：
   提交   submit    : DRAFT → PENDING
   通过   approve   : PENDING → APPROVED
   拒绝   reject    : PENDING → REJECTED
   撤回   cancel    : DRAFT/PENDING → CANCELLED
   ```

   - 状态用枚举 `ApprovalStatus`，流转规则用不可变映射表集中定义，Service 层守卫：**非法流转直接抛 `BusinessException`**（如对已通过的单再次通过）。

3. **数据库设计**（`db/V5__approval.sql`）：

   ```sql
   CREATE TABLE leave_request (
     id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
     leave_type TINYINT COMMENT '1年假 2事假 3病假', start_date DATE, end_date DATE,
     days INT, reason VARCHAR(500), status TINYINT DEFAULT 0 COMMENT '状态机值',
     approver_id BIGINT COMMENT '审批人',
     create_time DATETIME, update_time DATETIME, deleted TINYINT DEFAULT 0
   ) COMMENT '请假申请';

   CREATE TABLE reimbursement_request (
     id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
     amount DECIMAL(10,2), category VARCHAR(50), reason VARCHAR(500),
     status TINYINT DEFAULT 0, approver_id BIGINT,
     create_time DATETIME, update_time DATETIME, deleted TINYINT DEFAULT 0
   ) COMMENT '报销申请';

   CREATE TABLE approval_record (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     business_type TINYINT COMMENT '1请假 2报销',
     business_id BIGINT COMMENT '业务单号',
     approver_id BIGINT, action TINYINT COMMENT '1提交 2通过 3拒绝 4撤回',
     comment VARCHAR(500), create_time DATETIME
   ) COMMENT '审批记录';
   ```

4. **实体 / Mapper / 枚举**：`LeaveRequest`、`ReimbursementRequest`、`ApprovalRecord`；`ApprovalStatus`、`BusinessType`、`ApprovalAction`。
5. **服务与接口**：
   - `LeaveService` / `ReimbursementService`：
     - 提交申请（DRAFT→PENDING，落一条"提交"审批记录）。
     - 审批通过 / 拒绝（校验审批人身份、状态机校验，落审批记录）。
     - 撤回（校验状态）。
   - `ApprovalRecordService`：审批记录查询（按业务单）。
   - Controller：`/api/approvals/leave/**`、`/api/approvals/reimburse/**`：申请提交、我的申请列表（分页）、待我审批列表、审批操作、撤回、审批记录。
6. **测试**：
   - 状态机单元测试：遍历合法/非法流转，非法流转必须抛异常。
   - 集成测试：提交→通过，状态 APPROVED 且产生审批记录；提交→拒绝；已通过再审批被拒。
   - Controller 测试：`MockMvc` 走完整流程。

**完成标准（Checkpoint）**：
- [x] 编译 + 测试通过（54 个用例，含状态机全路径 + 审批集成测试）。
- [x] Swagger 覆盖全部审批接口（请假 / 报销 / 审批记录）。
- [x] 输出阶段报告，**停止等待确认**。

---

### 阶段 4：Redis 接入

**目标**：Redis 配置 + 登录状态 / 用户信息 / 热点数据缓存。

**为什么使用 Redis（对应 prompt 要求说明，不为了用而用）**：
- 登录认证场景：JWT 无状态方案下，用户权限/登录态放在 Redis，可在不改签发逻辑的前提下**实时生效/失效**（如改权限、踢下线），比纯无状态更可控。
- 用户信息与热点数据（部门、常量、菜单）读取频繁、低频变更，缓存显著降低 DB 压力。
- 仅对**高频读 + 低频写**的数据使用，不滥用。

**前置**：阶段 2/3 完成；**启动本机 Redis**。

**步骤**：

1. **依赖**：`spring-boot-starter-data-redis`（+ `commons-pool2` 连接池）；`application-dev.yml` 配置 host/port/密码。
2. **配置**：`config/RedisConfig` —— `RedisTemplate<String,Object>`（Value 用 Jackson JSON 序列化，key 用 String）；`service/RedisService` 封装 `set/get/delete/expire/hasKey`（含 `ObjectMapper` 反序列化泛型辅助）。
3. **应用**：
   - 登录成功后将 `token → 用户Id`、`用户Id → UserInfoVO` 缓存并设 TTL；登出删除缓存。
   - JWT 过滤器校验阶段：token 有效后优先读 Redis 用户权限（miss 则查 DB 并回填）。
   - 用户查询 / 菜单查询等热点接口加缓存（`@Cacheable` 或 `RedisService` 手动缓存 + 失效），并实现**写后失效**。
4. **接口**：提供 `POST /api/cache/evict`（按 key 清除缓存，供运维/测试），在 Swagger 中说明。
5. **测试**（需 Redis 运行）：
   - `RedisServiceTest`：set/get 往返、TTL 过期、delete。
   - 缓存生效测试：第二次查询命中缓存（不触发 Mapper 查询，用 spy 断言调用次数）。

**完成标准（Checkpoint）**：
- [x] 编译 + 测试通过（Redis 启动状态）。
- [x] 说明文档（dev.md 本阶段）记录缓存 key 规范与失效策略。
- [x] 输出阶段报告，**停止等待确认**。

#### 阶段 4 缓存 key 规范与失效策略（已实施）

**序列化方案**：`RedisTemplate<String,Object>` key/value 均用 String 序列化，值存 JSON 字符串；对象↔JSON 由 `RedisService` 借助 Jackson 3 `ObjectMapper` 互转（规避 Spring Data Redis 4 对 Jackson 2/3 序列化器歧义）。

**Key 规范**（统一前缀 `oa:`，冒号分隔业务域）：

| Key | 值 | TTL | 用途 |
|---|---|---|---|
| `oa:auth:token:{token}` | 用户ID | JWT 有效期（30 天） | 登录态；登出/被踢删除后 token 立即失效 |
| `oa:user:info:{userId}` | UserInfoVO | 30 天 | 鉴权过滤器按用户还原主体 + `/me` 快路径 |
| `oa:user:detail:{userId}` | UserVO | 10 分钟 | 用户详情热点缓存 |
| `oa:menu:tree` | MenuVO[] | 30 分钟 | 菜单树热点缓存 |

**失效策略（写后失效）**：

- 登出 → 删 `oa:auth:token:{token}`。
- 用户修改 / 删除 / 分配角色 → 删该用户 `oa:user:info:{id}` 与 `oa:user:detail:{id}`。
- 角色修改 / 删除 / 分配菜单 → 删该角色下所有用户的 `oa:user:info:{id}`（改权限实时生效）。
- 菜单修改 / 删除 → 删菜单树 + 全部 `oa:user:info:*`；菜单新增 → 仅删菜单树。
- 兜底：短 TTL（detail 10min / menu 30min）+ `POST /api/cache/evict` 运维手动清缓存。

---

### 阶段 6：RabbitMQ 消息模块

**目标**：审批完成后异步发送消息通知（服务 → MQ → 消费者 → 落库通知）。

**前置**：阶段 5 完成；**安装并启动 RabbitMQ**（本机未安装，需先行安装；如需可用 Docker，届时再确认）。

**步骤**：

1. **依赖**：`spring-boot-starter-amqp`；`application-dev.yml` 配置 MQ 连接。
2. **消息结构设计**：

   ```
   Exchange  : oa.approval.exchange  (direct)
   Queue     : oa.approval.notice.queue
   RoutingKey: approval.completed
   Message   : ApprovalNoticeMessage { businessType, businessId, applicantId, approverId, result, createTime }
   ```

3. **实现**：
   - `config/RabbitConfig`：声明 Exchange / Queue / Binding，配置 `Jackson2JsonMessageConverter`。
   - `dto/ApprovalNoticeMessage`。
   - `producer/ApprovalNoticeProducer`：审批通过 / 拒绝时发送消息。
   - `consumer/ApprovalNoticeConsumer`：消费消息 → 生成系统通知（写 `message` 表）。
   - 审批服务在状态流转成功处调用 producer。
4. **通知模块**：`entity/Message`（id, receiver_id, type, title, content, read_flag, create_time）+ `MessageService` + `MessageController`（分页查询我的通知、标记已读、未读计数）。
5. **测试**（需 RabbitMQ 运行）：
   - 发送测试：审批完成后断言队列收到消息（`RabbitTemplate` 取出校验内容）。
   - 消费测试：投递消息 → 消费者处理 → `message` 表出现对应通知记录。

**完成标准（Checkpoint）**：
- [ ] 编译 + 测试通过。
- [ ] 审批完成能产生异步通知。
- [ ] Swagger 覆盖通知查询/已读接口。
- [ ] 输出阶段报告，**停止等待确认**。

---

### 阶段 7：文件管理模块

**目标**：文件上传 / 下载 / 列表 / 删除。**先设计接口并等待确认**，再实现。

**前置**：阶段 2 完成（需登录用户身份）。

**步骤**：

1. **接口设计**（先行产出，供确认）：
   - `POST /api/files/upload`（multipart/form-data）→ 返回 `fileId`、`fileName`、`size`、`url`。
   - `GET /api/files/{id}/download` → 文件流下载。
   - `GET /api/files` → 分页列表（按名称/类型筛选）。
   - `DELETE /api/files/{id}` → 删除（逻辑删除 + 物理文件可选清理）。
2. **确认项**：存储策略 —— 默认**本地磁盘**（`upload.dir` 配置），预留 `StorageService` 接口，后续可选实现 MinIO（`MinioStorageServiceImpl`）。
3. **数据库设计**：`file_info`（id, original_name, store_name, path, url, size, content_type, uploader_id, create_time, deleted）。
4. **实现**：`entity/FileInfo`、`mapper`、`service/FileService`、`StorageService` 接口 + `LocalStorageServiceImpl`、`controller/FileController`。
5. **测试**：上传（校验大小/类型限制）、下载内容一致、列表分页、删除后不可下载。
6. **Swagger**：完整接口文档。

**完成标准（Checkpoint）**：
- [ ] 先提交接口设计文档，**等待确认后再编码**（prompt 要求）。
- [ ] 实现后编译 + 测试通过。
- [ ] 输出阶段报告，**停止等待确认**。

---

### 阶段 8：Elasticsearch 搜索模块

**目标**：员工搜索 + 文件搜索，支持关键词、分页、高亮。

**前置**：阶段 7 完成；**安装并启动 Elasticsearch**（客户端版本需与服务端大版本匹配，届时查询官方文档确认）。

**步骤**：

1. **依赖**：`spring-boot-starter-data-elasticsearch`；配置连接与版本核对。
2. **索引设计**：
   - `employee`：姓名、昵称、邮箱、部门（来源：`sys_user` + 部门）。
   - `file`：文件名、文件类型（来源：`file_info`）。
3. **数据同步**：业务写入后同步索引（简单方案：服务内同步；或定时全量/增量同步任务）。
4. **搜索接口**：
   - `GET /api/search/employees?keyword=&pageNum=&pageSize=` → 分页结果 + **高亮**字段。
   - `GET /api/search/files?keyword=&pageNum=&pageSize=`。
   - 高亮用 ES `highlight`，命中片段返回前端展示。
5. **测试**：索引 CRUD、关键词搜索命中、分页正确、高亮片段返回。
6. **Swagger**：搜索接口文档。

**完成标准（Checkpoint）**：
- [ ] 编译 + 测试通过（ES 启动状态）。
- [ ] 搜索 / 分页 / 高亮功能验证通过。
- [ ] 输出阶段报告，**停止等待确认**。

---

### 阶段 9：微服务拆分（远期）

> 当前**不实施**。单体版本稳定后，按 `user-service` / `approval-service` / `file-service` / `message-service` 拆分，引入 Nacos / Gateway / OpenFeign。届时重新制定拆分计划并逐阶段推进，不提前拆。

---

## 5. 每阶段完成后的汇报格式（prompt 要求）

每个阶段 checkpoint 输出固定五部分：

1. **完成了什么** —— 本阶段交付内容与验证结果。
2. **修改了哪些文件** —— 新增/修改文件清单。
3. **如何运行测试** —— 具体命令与依赖的中间件。
4. **API 如何调用** —— 示例 curl 或接口说明。
5. **下一步计划** —— 下一阶段内容与前置条件。

---

## 6. 开发命令速查（Windows，使用项目自带 mvnw.cmd）

| 命令 | 用途 |
|---|---|
| `.\mvnw.cmd clean test` | 编译 + 运行全部测试 |
| `.\mvnw.cmd spring-boot:run` | 启动应用 |
| `.\mvnw.cmd package -DskipTests` | 打包 jar |
| `.\mvnw.cmd clean` | 清理 target |

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- 默认端口：8080（可在 `application.yml` 修改）

---

## 7. 待确认问题清单（后续 Checkpoint 前需要用户答复）

| # | 问题 | 默认建议 | 需要答复的时点 |
|---|---|---|---|
| 1 | 本机 MySQL 连接信息（host/port/账号/密码） | 用户提供后填入 `application-dev.yml` | 阶段 2 前 |
| 2 | 审批深度：单级审批还是多级审批 | 单级审批，多级留扩展点 | 阶段 5 前 |
| 3 | 文件存储：本地磁盘还是 MinIO | 本地磁盘，预留 `StorageService` 接口 | 阶段 7 前 |
| 4 | RabbitMQ 安装方式（本机安装 / 届时再定） | 本机安装（不用 Docker） | 阶段 6 前 |
| 5 | Elasticsearch 安装方式（本机安装 / 届时再定） | 本机安装（不用 Docker） | 阶段 8 前 |
| 6 | Java 目标版本：pom 目前为 17，本机 JDK 21 | 保持 17（兼容）；如需升级到 21 可在阶段 1 顺带调整 | 阶段 1 前 |

---

## 8. 第一阶段计划（阶段 1 详细行动清单）

按 `阶段 1` 章节执行，首轮动作如下（完成后输出汇报并等待确认）：

1. `com.tuning.oa_system` → `com.tuning.oasystem` 包迁移。
2. `pom.xml` 增加 springdoc / validation / lombok 依赖。
3. 配置 `application.yml` + `application-dev.yml`（含 springdoc、Jackson 时区配置）。
4. 实现 `common`（Result / ErrorCode / PageResult）、`exception`（BusinessException / GlobalExceptionHandler）、`config/OpenApiConfig`、`controller/HealthController`。
5. 编写并运行基础测试（Health 冒烟、全局异常、Result 结构）。
6. `.\mvnw.cmd clean test` 全绿 → 启动 → 确认 Swagger UI 可用。
7. 输出阶段 1 汇报，等待确认后进入阶段 2。
