package com.yourbot.scheduler;

import lombok.Data;

@Data
public class ScheduledTask {
    private String name;           // 任务名称
    private TaskType type;         // 任务类型
    private String targetType;     // 目标类型：GROUP或PRIVATE
    private long targetId;         // 目标ID（群号或QQ号）
    private String cronExpression; // Cron表达式
    private String content;        // 消息内容（用于SEND_MESSAGE类型）
    private boolean enable;        // 是否启用（用于GROUP_BAN_ALL类型）
    private long memberId;         // 成员ID（用于GROUP_BAN_MEMBER类型）
    private int duration;          // 禁言时长（秒）（用于GROUP_BAN_MEMBER类型）
} 