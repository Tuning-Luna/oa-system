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

# ============ 阶段 5：审批流程模块（请假/报销） ============
# 任何登录用户可提交（需指定审批人）；仅指定审批人可审批；仅申请人可撤回。
# 状态机：DRAFT(0) → PENDING(1) → APPROVED(2)/REJECTED(3)；DRAFT/PENDING 可撤回 → CANCELLED(4)。非法流转返回 400。

# 请假
curl -X POST http://localhost:8080/api/approvals/leave -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"leaveType":2,"startDate":"2026-08-10","endDate":"2026-08-12","days":3,"reason":"家中有事","approverId":1}'
curl "http://localhost:8080/api/approvals/leave/my?status=" -H "Authorization: Bearer <token>"        # 我的请假（可按状态筛选）
curl http://localhost:8080/api/approvals/leave/pending -H "Authorization: Bearer <token>"             # 待我审批
curl http://localhost:8080/api/approvals/leave/{id} -H "Authorization: Bearer <token>"                # 详情（申请人/审批人）
curl -X PUT http://localhost:8080/api/approvals/leave/{id}/approve -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"comment":"同意"}'
curl -X PUT http://localhost:8080/api/approvals/leave/{id}/reject  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"comment":"不通过"}'
curl -X PUT http://localhost:8080/api/approvals/leave/{id}/cancel  -H "Authorization: Bearer <token>" # 撤回

# 报销
curl -X POST http://localhost:8080/api/approvals/reimburse -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"amount":500.00,"category":"交通费","reason":"出差打车","approverId":1}'
curl "http://localhost:8080/api/approvals/reimburse/my?status=" -H "Authorization: Bearer <token>"
curl http://localhost:8080/api/approvals/reimburse/pending -H "Authorization: Bearer <token>"
curl http://localhost:8080/api/approvals/reimburse/{id} -H "Authorization: Bearer <token>"
curl -X PUT http://localhost:8080/api/approvals/reimburse/{id}/approve -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"comment":"同意"}'
curl -X PUT http://localhost:8080/api/approvals/reimburse/{id}/reject  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{}'
curl -X PUT http://localhost:8080/api/approvals/reimburse/{id}/cancel  -H "Authorization: Bearer <token>"

# 审批记录（businessType：1请假 2报销）
curl "http://localhost:8080/api/approvals/records?businessType=1&businessId={id}" -H "Authorization: Bearer <token>"