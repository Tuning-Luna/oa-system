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
# 用户管理
curl "http://localhost:8080/api/users?username=ad&status=1" -H "Authorization: Bearer <token>"
curl -X PUT http://localhost:8080/api/users/{id} -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"nickname":"新昵称","status":0}'
curl -X DELETE http://localhost:8080/api/users/{id} -H "Authorization: Bearer <token>"