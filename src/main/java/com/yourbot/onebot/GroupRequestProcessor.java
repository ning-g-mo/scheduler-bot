package com.yourbot.onebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.yourbot.config.ConfigManager;
import com.yourbot.scheduler.ScheduledTask;
import com.yourbot.scheduler.TaskType;
import com.yourbot.util.ConsoleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进群请求处理器
 */
public class GroupRequestProcessor {
    private static final Logger logger = LoggerFactory.getLogger(GroupRequestProcessor.class);
    
    // 缓存正在处理的进群请求，键为请求标识(flag)，值为请求信息
    private final Map<String, GroupJoinRequest> pendingRequests = new ConcurrentHashMap<>();
    
    // OneBotClient实例
    private final OneBotClient client = OneBotClient.getInstance();
    
    /**
     * 初始化处理器
     */
    public void init() {
        logger.info("初始化进群请求处理器");
        OneBotEventListener.registerListener(this::handleEvent);
    }
    
    /**
     * 处理OneBot事件
     */
    private void handleEvent(String event, JsonNode data) {
        if ("request.group.add".equals(event)) {
            handleGroupJoinRequest(data);
        }
    }
    
    /**
     * 处理进群请求事件
     */
    private void handleGroupJoinRequest(JsonNode data) {
        try {
            long groupId = data.get("group_id").asLong();
            long userId = data.get("user_id").asLong();
            String flag = data.get("flag").asText();
            String comment = data.has("comment") ? data.get("comment").asText() : "";
            
            GroupJoinRequest request = new GroupJoinRequest();
            request.setGroupId(groupId);
            request.setUserId(userId);
            request.setFlag(flag);
            request.setComment(comment);
            request.setTimestamp(System.currentTimeMillis());
            
            // 将请求加入缓存
            pendingRequests.put(flag, request);
            
            logger.info("收到进群请求: 群 {}, 用户 {}, 验证信息: {}", groupId, userId, comment);
            ConsoleUtil.info("收到进群请求: 群 " + groupId + ", 用户 " + userId + ", 验证信息: " + comment);
            
            // 获取此群配置的验证任务
            ScheduledTask verifyTask = getVerifyTaskForGroup(groupId);
            
            // 如果有为此群配置进群验证任务
            if (verifyTask != null) {
                // 提取用户提供的答案
                String answer = client.extractVerifyAnswer(comment);
                
                logger.debug("提取的验证答案: {}", answer);
                
                // 验证答案是否正确
                boolean isCorrect = verifyTask.checkAnswer(answer);
                
                if (isCorrect) {
                    // 答案正确，同意请求
                    client.handleGroupRequest(flag, true, null);
                    logger.info("验证通过，同意 {} 加入群 {}", userId, groupId);
                    ConsoleUtil.success("验证通过，同意 " + userId + " 加入群 " + groupId);
                } else {
                    // 答案错误，拒绝请求
                    String rejectMessage = verifyTask.getRejectMessage();
                    if (rejectMessage == null || rejectMessage.isEmpty()) {
                        rejectMessage = "验证答案错误，请重新申请并正确回答问题：" + verifyTask.getVerifyQuestion();
                    }
                    
                    client.handleGroupRequest(flag, false, rejectMessage);
                    logger.info("验证失败，拒绝 {} 加入群 {}, 原因: {}", userId, groupId, rejectMessage);
                    ConsoleUtil.warn("验证失败，拒绝 " + userId + " 加入群 " + groupId);
                }
                
                // 处理完毕，从缓存中移除
                pendingRequests.remove(flag);
            } else {
                logger.info("群 {} 没有配置进群验证任务，忽略此请求", groupId);
                ConsoleUtil.info("群 " + groupId + " 没有配置进群验证任务，忽略此请求");
            }
        } catch (Exception e) {
            logger.error("处理进群请求失败", e);
            ConsoleUtil.error("处理进群请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取群对应的验证任务
     */
    private ScheduledTask getVerifyTaskForGroup(long groupId) {
        List<ScheduledTask> tasks = ConfigManager.getInstance().getScheduledTasks();
        
        for (ScheduledTask task : tasks) {
            if (task.getType() == TaskType.GROUP_REQUEST_VERIFY &&
                    "GROUP".equals(task.getTargetType()) &&
                    task.getTargetIds().contains(groupId)) {
                return task;
            }
        }
        
        return null;
    }
    
    /**
     * 进群请求信息
     */
    private static class GroupJoinRequest {
        private long groupId;
        private long userId;
        private String flag;
        private String comment;
        private long timestamp;
        
        public long getGroupId() {
            return groupId;
        }
        
        public void setGroupId(long groupId) {
            this.groupId = groupId;
        }
        
        public long getUserId() {
            return userId;
        }
        
        public void setUserId(long userId) {
            this.userId = userId;
        }
        
        public String getFlag() {
            return flag;
        }
        
        public void setFlag(String flag) {
            this.flag = flag;
        }
        
        public String getComment() {
            return comment;
        }
        
        public void setComment(String comment) {
            this.comment = comment;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
} 