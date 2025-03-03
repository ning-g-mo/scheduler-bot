# 定时任务机器人

一个基于OneBot协议的QQ机器人，用于执行各种定时任务，如定时发送消息、群组管理等。

## 功能特点

- 支持多种定时任务类型：
  - 定时发送消息（群聊/私聊）
  - 定时全体禁言/解禁
  - 定时禁言/解禁特定成员
- 使用Cron表达式灵活配置任务执行时间
- 彩色日志输出，方便监控任务执行情况
- 任务执行日志记录和查询功能
- 配置文件热重载，无需重启即可更新任务

## 系统要求

- Java 21 或更高版本
- 支持OneBot v11协议的QQ机器人框架（如go-cqhttp）

## 快速开始

### 1. 下载并安装

1. 从[Releases](https://github.com/yourusername/scheduler-bot/releases)页面下载最新版本的JAR文件
2. 确保您已安装Java 21或更高版本

### 2. 配置机器人

首次运行时，程序会自动创建一个默认的`config.yml`配置文件。您需要编辑此文件，设置正确的机器人连接信息和任务配置。
```yaml
# 机器人基本配置
bot:
  websocket: "ws://127.0.0.1:6700"  # OneBot WebSocket地址
  accessToken: ""  # 访问令牌（如果有）

# 定时任务配置
scheduledTasks:
  - name: "早安问候"  # 任务名称
    type: "SEND_MESSAGE"  # 任务类型
    targetType: "GROUP"  # 目标类型：GROUP或PRIVATE
    targetId: 123456789  # 群号或QQ号
    cronExpression: "0 30 7 * * ?"  # 每天早上7:30执行
    content: "早上好，今天也要元气满满哦！"  # 发送的消息内容
    
  - name: "晚间提醒"
    type: "SEND_MESSAGE"
    targetType: "PRIVATE"
    targetId: 987654321
    cronExpression: "0 0 22 * * ?"  # 每天晚上10点执行
    content: "该休息了，记得早点睡觉哦~"
    
  - name: "周末全体禁言"
    type: "GROUP_BAN_ALL"
    targetType: "GROUP"
    targetId: 123456789
    cronExpression: "0 0 23 ? * FRI"  # 每周五晚上11点执行
    enable: true  # true表示开启全体禁言，false表示关闭
    sendNotice: true  # 是否发送通知消息
    noticeContent: "周末愉快！全体禁言开启，请各位周一见~"  # 通知消息内容
    
  - name: "周一解除全体禁言"
    type: "GROUP_BAN_ALL"
    targetType: "GROUP"
    targetId: 123456789
    cronExpression: "0 0 8 ? * MON"  # 每周一早上8点执行
    enable: false  # false表示关闭全体禁言
    sendNotice: true  # 是否发送通知消息
    noticeContent: "早上好！新的一周开始了，全体禁言已解除~"  # 通知消息内容
    
  - name: "特定用户禁言"
    type: "GROUP_BAN_MEMBER"
    targetType: "GROUP"
    targetId: 123456789
    memberId: 111222333  # 要禁言的成员QQ号
    cronExpression: "0 0 12 * * ?"  # 每天中午12点执行
    duration: 3600  # 禁言时长（秒），0表示解除禁言
    sendNotice: true  # 是否发送通知消息
    noticeContent: "成员 {memberId} 已被禁言 {duration}，请遵守群规则。"  # 通知消息内容，支持变量替换
    
  - name: "特定用户解禁"
    type: "GROUP_BAN_MEMBER"
    targetType: "GROUP"
    targetId: 123456789
    memberId: 111222333  # 要解禁的成员QQ号
    cronExpression: "0 0 13 * * ?"  # 每天下午1点执行
    duration: 0  # 禁言时长为0表示解除禁言
    sendNotice: true  # 是否发送通知消息
    noticeContent: "成员 {memberId} 的禁言已解除，希望遵守群规则。"  # 通知消息内容，支持变量替换
```


### 3. 启动机器人

Windows:
```bash
java -jar scheduler-bot-1.0.jar
```

Linux/Mac:
```bash
./start.sh
```


### 4. 使用命令

机器人启动后，您可以在控制台使用以下命令：

- `help` - 显示帮助信息
- `reload` - 重新加载配置文件
- `logs` - 显示任务执行日志
  - `logs recent [数量]` - 显示最近的日志
  - `logs task [任务名]` - 显示特定任务的日志
  - `logs export [任务名]` - 导出特定任务的日志到文件
- `exit` - 退出程序

## 任务类型说明

### 1. 发送消息 (SEND_MESSAGE)

定时发送消息到群聊或私聊。

必要参数：
- `targetType`: 目标类型，可选值为 "GROUP"(群聊) 或 "PRIVATE"(私聊)
- `targetId`: 目标ID，群号或QQ号
- `content`: 消息内容

### 2. 全体禁言 (GROUP_BAN_ALL)

定时开启或关闭群聊的全体禁言功能。

必要参数：
- `targetType`: 必须为 "GROUP"
- `targetId`: 群号
- `enable`: 是否启用全体禁言，true 表示开启，false 表示关闭

可选参数：
- `sendNotice`: 是否发送通知消息，默认为 false
- `noticeContent`: 通知消息内容

### 3. 成员禁言 (GROUP_BAN_MEMBER)

定时禁言或解除禁言群成员。

必要参数：
- `targetType`: 必须为 "GROUP"
- `targetId`: 群号
- `memberId`: 要禁言的成员QQ号
- `duration`: 禁言时长（秒），0 表示解除禁言

可选参数：
- `sendNotice`: 是否发送通知消息，默认为 false
- `noticeContent`: 通知消息内容，支持变量 {memberId} 和 {duration}

## Cron表达式说明

Cron表达式由6个或7个由空格分隔的时间字段组成：
```
秒 分 时 日 月 周 [年]
```

示例：
- `0 0 8 * * ?` - 每天早上8点
- `0 30 7 * * ?` - 每天早上7点30分
- `0 0 12 ? * MON-FRI` - 每周一至周五中午12点
- `0 0 20 ? * FRI` - 每周五晚上8点
- `0 0/30 * * * ?` - 每30分钟

## 日志文件

- `logs/scheduler-bot.log` - 主日志文件
- `logs/debug.log` - 调试日志文件
- `logs/tasks/[任务名]/` - 任务执行日志（JSON格式）
- `exports/` - 导出的任务日志文件（文本格式）

## 构建项目

如果您想自己构建项目，可以使用Maven：

```bash
mvn clean package
```


构建完成后，JAR文件将位于 `target/` 目录下。

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 更新说明

### v1.1.0
- 支持在一个任务中设置多个目标
- 优化消息发送机制
- 添加更多安全保护措施

[查看完整更新日志](CHANGELOG.md)

## 配置示例

```yaml
scheduledTasks:
  - name: "群发消息"
    type: "SEND_MESSAGE"
    targetType: "GROUP"
    targetIds:  # 支持多个群
      - 123456789
      - 987654321
    cronExpression: "0 30 7 * * ?"
    content: "早上好！"

  - name: "批量禁言"
    type: "GROUP_BAN_MEMBER"
    targetType: "GROUP"
    targetIds:  # 要操作的群
      - 123456789
      - 987654321
    memberIds:  # 要禁言的成员
      - 111222333
      - 444555666
    cronExpression: "0 0 12 * * ?"
    duration: 3600
    sendNotice: true
    noticeContent: "成员 {memberId} 已被禁言 {duration}"
```


