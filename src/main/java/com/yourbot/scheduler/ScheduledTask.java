package com.yourbot.scheduler;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class ScheduledTask {
    private String name;           // 任务名称
    private TaskType type;         // 任务类型
    private String targetType;     // 目标类型：GROUP或PRIVATE
    private List<Long> targetIds = new ArrayList<>();  // 目标ID列表（群号或QQ号）
    private String cronExpression; // Cron表达式
    private String content;        // 消息内容（用于SEND_MESSAGE类型）
    private boolean enable;        // 是否启用（用于GROUP_BAN_ALL类型）
    private List<Long> memberIds = new ArrayList<>();  // 成员ID列表（用于GROUP_BAN_MEMBER类型）
    private int duration;          // 禁言时长（秒）（用于GROUP_BAN_MEMBER类型）
    private boolean sendNotice;    // 是否发送通知消息
    private String noticeContent;  // 通知消息内容
    
    // 兼容单个目标ID的setter
    public void setTargetId(long targetId) {
        this.targetIds.clear();
        this.targetIds.add(targetId);
    }
    
    // 兼容单个目标ID的getter
    public long getTargetId() {
        return targetIds.isEmpty() ? 0 : targetIds.get(0);
    }
    
    // 兼容单个成员ID的setter
    public void setMemberId(long memberId) {
        this.memberIds.clear();
        this.memberIds.add(memberId);
    }
    
    // 兼容单个成员ID的getter
    public long getMemberId() {
        return memberIds.isEmpty() ? 0 : memberIds.get(0);
    }
} 