# 贡献指南

感谢你对 Camera Rental 项目的关注！欢迎提交 Issue 和 Pull Request。

## 开发流程

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m 'feat: 添加xxx功能'`
4. 推送分支：`git push origin feature/your-feature`
5. 创建 Pull Request

## Commit 规范

采用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 代码重构 |
| `test` | 测试相关 |
| `chore` | 构建/工具变更 |

示例：
```
feat: 添加设备搜索功能
fix: 修复 token 刷新失败问题
docs: 更新 README 安装说明
```

## 分支命名

- `feature/xxx` - 新功能
- `fix/xxx` - Bug 修复
- `docs/xxx` - 文档
- `refactor/xxx` - 重构

## 代码规范

### 后端
- 遵循阿里巴巴 Java 编码规范
- Controller 方法建议添加 Swagger 注解
- 异常统一使用 `BusinessException(ErrorCode.*)` 风格
- 管理员接口添加 `@AdminOnly` 注解

### 前端
- 组件使用 `<script setup>` 语法
- 使用 Element Plus 组件库
- 样式使用 `scoped`

## Issue 规范

提交 Issue 请包含：
- 问题描述
- 复现步骤
- 期望行为 vs 实际行为
- 环境信息（OS、JDK 版本、浏览器等）
- 截图或日志（如有）

## 本地开发

参考 [README.md](README.md) 的快速开始部分搭建开发环境。

