# 贡献指南

感谢您考虑为定时任务机器人项目做出贡献！以下是一些指导原则，帮助您更好地参与项目开发。

## 报告问题

如果您发现了bug或有新功能建议，请先检查是否已有相关的issue。如果没有，请创建一个新的issue，并尽可能详细地描述问题或建议。

## 提交代码

1. Fork本仓库
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建一个Pull Request

## 代码风格

- 请遵循项目现有的代码风格
- 为新功能添加适当的注释和文档
- 确保您的代码通过所有测试

## 开发环境设置

1. 克隆仓库
2. 安装Java 21或更高版本
3. 使用Maven构建项目：`mvn clean package`

## 添加新的任务类型

如果您想添加新的任务类型，需要：

1. 在`TaskType`枚举中添加新类型
2. 在`SchedulerManager.TaskJob`类的`execute`方法中添加对应的处理逻辑
3. 更新README文档，说明新任务类型的用法
4. 添加相关的测试用例

## 许可证

通过贡献您的代码，您同意您的贡献将根据项目的MIT许可证进行许可。