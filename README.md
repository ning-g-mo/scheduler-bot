# 定时任务机器人

一个基于OneBot协议的QQ机器人，用于执行各种定时任务，如定时发送消息、群组管理等。

## 功能特点

- 支持多种定时任务类型：
  - 定时发送消息（群聊/私聊）
  - 定时全体禁言/解禁
  - 定时禁言/解禁特定成员
  - 进群申请智能验证
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

### 4. 进群验证 (GROUP_REQUEST_VERIFY)

自动处理进群申请，根据用户在申请时填写的验证答案和等级决定是否通过。

必要参数：
- `targetType`: 必须为 "GROUP"
- `targetIds`: 要启用验证的群号列表
- `verifyQuestion`: 验证问题
- `verifyAnswers`: 正确答案列表，支持多个正确答案

基本可选参数：
- `rejectMessage`: 拒绝消息，当验证失败时返回给用户的消息
- `caseSensitive`: 答案是否区分大小写，默认为 false

增强验证可选参数：
- `ignoreWhitespace`: 是否忽略答案中的空格，默认为 false
- `fuzzyMatch`: 是否启用模糊匹配，默认为 false
- `minLevel`: 最低等级要求，低于此等级将拒绝，默认为 0（不检查等级）
- `maxAutoAcceptLevel`: 达到指定等级自动通过验证，默认为 0（不自动通过）
- `verifyMode`: 验证模式，详见下方验证模式说明

#### 验证模式说明
- `IGNORE_ALL`: 忽略所有验证，直接同意所有申请
- `ANY_ONE_PASS`: 答案验证或等级验证通过任一个即可同意
- `BOTH_REQUIRED`: 必须同时通过答案验证和等级验证
- `ANSWER_ONLY`: 只检查答案正确性（默认模式）
- `LEVEL_ONLY`: 只检查用户等级
- `ANSWER_PASS_LEVEL_PENDING`: 答案通过但等级不符要求时挂起请求（不自动处理）
- `LEVEL_PASS_ANSWER_PENDING`: 等级通过但答案不正确时挂起请求（不自动处理）

#### 答案验证增强说明
- `ignoreWhitespace`选项：
  - 设为`true`时会忽略答案中的所有空格，适合对格式要求不严格的场景
  - 例如："面向 对象"与"面向对象"将被视为相同
  
- `fuzzyMatch`选项：
  - 设为`true`时启用模糊匹配，只要用户答案包含正确答案，或正确答案包含用户答案，就视为通过
  - 例如：正确答案为"面向对象"，用户回答"Java是面向对象的语言"或"面向对象编程"都会通过
  - 适合需要灵活验证的场景，但可能降低验证精确度

#### 等级验证说明
- `minLevel`: 设置群成员最低等级要求
  - 当用户等级低于此值时，拒绝入群请求
  - 设为0时不检查等级
  
- `maxAutoAcceptLevel`: 设置自动通过的等级阈值
  - 当用户等级高于或等于此值时，自动同意入群请求，无需答案验证
  - 设为0时禁用此功能
  - 适合对资深用户简化入群流程

#### 进群验证配置示例

基础验证示例：
```yaml
- name: "编程群进群验证"
  type: "GROUP_REQUEST_VERIFY"
  targetType: "GROUP"
  targetIds:
    - 123456789
  verifyQuestion: "请回答：Python和Java都是什么类型的编程语言？"
  verifyAnswers:
    - "面向对象编程语言"
    - "面向对象"
    - "OOP"
  rejectMessage: "回答错误，请重新申请并正确回答问题"
  caseSensitive: false
  verifyMode: "ANSWER_ONLY"
```

高级验证示例：
```yaml
- name: "VIP用户群验证"
  type: "GROUP_REQUEST_VERIFY"
  targetType: "GROUP"
  targetIds:
    - 123454321
  verifyQuestion: "请输入邀请码或者告知您的技术领域"
  verifyAnswers:
    - "VIP2023"
    - "前端"
    - "后端"
    - "全栈"
  rejectMessage: "验证未通过，请联系管理员获取邀请码"
  caseSensitive: true
  ignoreWhitespace: true
  fuzzyMatch: true
  minLevel: 20
  maxAutoAcceptLevel: 50
  verifyMode: "ANY_ONE_PASS"
```

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

### v1.2.3
- 修复进群验证功能中的等级验证问题
- 提升API调用稳定性和可靠性

### v1.3.1
- 添加进群验证增强功能
  - 答案验证增强：忽略空格、模糊匹配
  - 等级验证：最低等级要求、自动通过等级
  - 多种验证模式组合策略

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
    
  - name: "进群验证-基础"
    type: "GROUP_REQUEST_VERIFY"
    targetType: "GROUP"
    targetIds:
      - 123456789  # 群号
    verifyQuestion: "请回答：Python和Java都是什么类型的编程语言？"
    verifyAnswers:  # 多个正确答案
      - "面向对象编程语言"
      - "面向对象"
      - "OOP"
    rejectMessage: "回答错误，请重新申请并正确回答问题"
    caseSensitive: false  # 答案是否区分大小写
    
  - name: "进群验证-高级"
    type: "GROUP_REQUEST_VERIFY"
    targetType: "GROUP"
    targetIds:
      - 987654321  # 群号
    verifyQuestion: "请回答：本群的主题是什么？（提示：与编程相关）"
    verifyAnswers:
      - "Java编程"
      - "Java"
    rejectMessage: "回答错误，请查看群介绍后重新申请"
    caseSensitive: false
    ignoreWhitespace: true
    fuzzyMatch: true
    minLevel: 15
    maxAutoAcceptLevel: 40
    verifyMode: "BOTH_REQUIRED"

bot:
  log:
    enableMessageLog: false     # 是否记录收到的消息
    enableDebugLog: false      # 是否启用调试日志
    includeInfoInNormal: true  # 是否在普通日志中包含INFO和MIXIN日志
    maxMessageLogs: 1000       # 最大消息日志数量
    maxDays: 30               # 日志保留天数
```

## 日志配置说明

```yaml
bot:
  log:
    enableMessageLog: false     # 是否记录收到的消息
    enableDebugLog: false      # 是否启用调试日志
    includeInfoInNormal: true  # 是否在普通日志中包含INFO和MIXIN日志
    maxMessageLogs: 1000       # 最大消息日志数量
    maxDays: 30               # 日志保留天数
```

- `includeInfoInNormal`: 设置为 true 时，普通日志文件将包含 INFO 和 MIXIN 级别的日志；设置为 false 时，只包含 WARN 及以上级别的日志

## 图形化界面

程序默认以图形界面模式启动，界面包括：

- 左侧：日志显示区域，实时展示程序运行日志
- 右上：系统信息区域，显示内存使用、CPU负载、任务状态等
- 右下：任务列表区域，显示所有定时任务及其下次执行时间
- 底部：命令输入区域，可以输入控制台命令

如需以无界面模式启动（仅命令行），可使用以下命令：

```bash
# Windows
start.bat nogui

# Linux/macOS
./start.sh nogui
```

Docker中默认以无界面模式运行。

## 最佳实践

### 进群验证场景推荐

1. **普通社交群**：
   ```yaml
   verifyQuestion: "请回答：2+3=?"
   verifyAnswers:
     - "5"
     - "五"
   caseSensitive: false
   ignoreWhitespace: true
   fuzzyMatch: false
   verifyMode: "ANSWER_ONLY"
   ```
   简单的数学问题防止广告机器人

2. **技术交流群**：
   ```yaml
   verifyQuestion: "请简述您的技术背景和加群目的"
   verifyAnswers:
     - "程序员"
     - "开发"
     - "学习"
     - "技术"
   caseSensitive: false
   ignoreWhitespace: true
   fuzzyMatch: true
   minLevel: 20
   verifyMode: "BOTH_REQUIRED"
   ```
   要求一定的活跃度和技术背景

3. **VIP会员群**：
   ```yaml
   verifyQuestion: "请输入您的会员邀请码"
   verifyAnswers:
     - "VIP2023"
     - "MEMBER2023"
   caseSensitive: true
   ignoreWhitespace: true
   fuzzyMatch: false
   maxAutoAcceptLevel: 100
   verifyMode: "ANSWER_ONLY"
   ```
   严格邀请码验证，资深用户可以自动通过

4. **粉丝群**：
   ```yaml
   verifyQuestion: "请回答：博主的名字是？"
   verifyAnswers:
     - "张三"
   caseSensitive: false
   ignoreWhitespace: true
   fuzzyMatch: false
   verifyMode: "ANY_ONE_PASS"
   minLevel: 10
   ```
   简单的粉丝问题，同时要求一定活跃度


