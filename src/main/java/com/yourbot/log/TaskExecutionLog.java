package com.yourbot.log;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务执行日志实体类
 */
@Data
public class TaskExecutionLog {
    // 日志ID
    private String id;
    
    // 任务名称
    private String taskName;
    
    // 任务类型
    private String taskType;
    
    // 执行时间
    private LocalDateTime executionTime;
    
    // 执行结果（成功/失败）
    private boolean success;
    
    // 目标类型（群/私聊）
    private String targetType;
    
    // 目标ID（群号/QQ号）
    private List<Long> targetIds = new ArrayList<>();
    
    // 成员ID（如果有）
    private List<Long> memberIds = new ArrayList<>();
    
    // 执行详情
    private String details;
    
    // 错误信息（如果有）
    private String errorMessage;
    
    /**
     * 生成唯一ID
     */
    public static String generateId() {
        return java.util.UUID.randomUUID().toString();
    }
    
    // 兼容旧版本的setter/getter
    public void setTargetId(long targetId) {
        this.targetIds.clear();
        this.targetIds.add(targetId);
    }
    
    public long getTargetId() {
        return targetIds.isEmpty() ? 0 : targetIds.get(0);
    }
} 