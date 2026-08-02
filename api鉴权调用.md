4. API 如何调用

# 登录（admin/Admin@123456）
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123456"}'
# 注册
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"Passw0rd123","nickname":"张三"}'
# 带 token 访问受保护接口
curl http://localhost:8080/api/users -H "Authorization: Bearer <token>"
curl http://localhost:8080/api/auth/me  -H "Authorization: Bearer <token>"
# 用户管理（阶段 3 起需 system:user:* 权限）
curl "http://localhost:8080/api/users?username=ad&status=1" -H "Authorization: Bearer <token>"
curl -X PUT http://localhost:8080/api/users/{id} -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"nickname":"新昵称","status":0}'
curl -X DELETE http://localhost:8080/api/users/{id} -H "Authorization: Bearer <token>"

# ============ 阶段 3：权限模块（RBAC） ============
# 当前用户信息（含角色编码 + 权限标识）
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer <token>"

# 角色管理（需 system:role:* 权限）
curl "http://localhost:8080/api/roles?name=&status=" -H "Authorization: Bearer <token>"           # 角色分页
curl http://localhost:8080/api/roles/all -H "Authorization: Bearer <token>"                       # 全部启用角色（分配角色下拉）
curl -X POST http://localhost:8080/api/roles -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"name":"部门主管","code":"manager","description":"负责部门审批","status":1}'
curl -X PUT http://localhost:8080/api/roles/{id} -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"name":"部门主管","code":"manager","status":1}'
curl -X DELETE http://localhost:8080/api/roles/{id} -H "Authorization: Bearer <token>"
curl -X PUT http://localhost:8080/api/roles/{id}/menus -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"menuIds":[100,200,201,202,203,204]}'                                                    # 分配菜单（全量替换）
curl http://localhost:8080/api/roles/{id}/menus -H "Authorization: Bearer <token>"               # 角色当前菜单ID

# 菜单管理（需 system:menu:* 权限）
curl http://localhost:8080/api/menus/tree -H "Authorization: Bearer <token>"                     # 菜单树
curl -X POST http://localhost:8080/api/menus -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"parentId":200,"name":"用户重置密码","type":3,"perms":"system:user:reset","sort":5,"status":1}'
curl -X PUT http://localhost:8080/api/menus/{id} -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"parentId":200,"name":"用户重置密码","type":3,"perms":"system:user:reset","sort":5,"status":1}'
curl -X DELETE http://localhost:8080/api/menus/{id} -H "Authorization: Bearer <token>"

# 给用户分配角色（需 system:user:edit 权限）
curl -X PUT http://localhost:8080/api/users/{id}/roles -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"roleIds":[1]}'                                                                            # 全量替换
curl http://localhost:8080/api/users/{id}/roles -H "Authorization: Bearer <token>"                # 用户当前角色ID

# 权限说明：无角色用户访问受保护接口返回 403；未带 token 返回 401；admin 拥有全部基础权限。