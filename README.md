
## 主要功能

### 聊天室
- 创建私密聊天室
- 加入已有聊天室
- 实时消息发送和接收
- 在线用户列表实时更新

### 安全性
- SM4 加密算法保护消息安全
- 支持请求和响应数据加密
- 防重放攻击保护

### 性能优化
- 长轮询机制减少服务器负载
- Redis 缓存提升响应速度
- 连接池优化数据库访问

## API 文档

### 聊天室接口
- `POST /api/room/create` - 创建聊天室
- `POST /api/room/join` - 加入聊天室
- `POST /api/room/leave` - 离开聊天室
- `POST /api/room/message` - 发送消息
- `POST /api/room/messages` - 获取消息列表
- `POST /api/room/users` - 获取在线用户列表

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

项目维护者 - [@YourGithubUsername](https://github.com/YourGithubUsername)

项目链接: [https://github.com/YourGithubUsername/secret-chat](https://github.com/YourGithubUsername/secret-chat)