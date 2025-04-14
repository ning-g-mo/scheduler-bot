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
                processVerification(verifyTask, groupId, userId, flag, comment);
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
     * 处理进群验证
     */
    private void processVerification(ScheduledTask verifyTask, long groupId, long userId, String flag, String comment) {
        // 提取用户提供的答案
        String answer = client.extractVerifyAnswer(comment);
        logger.debug("提取的验证答案: {}", answer);
        
        // 获取用户等级
        int userLevel = client.getGroupMemberLevel(groupId, userId);
        logger.debug("用户 {} 在群 {} 的等级: {}", userId, groupId, userLevel);
        
        // 验证结果
        boolean answerCorrect = verifyTask.checkAnswer(answer);
        boolean levelPassed = verifyTask.checkLevel(userLevel);
        boolean autoAcceptLevel = verifyTask.checkAutoAcceptLevel(userLevel);
        
        // 记录验证结果
        logger.debug("验证结果 - 答案: {}, 等级: {}, 自动通过等级: {}", 
                answerCorrect ? "通过" : "不通过", 
                levelPassed ? "通过" : "不通过",
                autoAcceptLevel ? "通过" : "不通过");
        
        // 根据验证模式决定处理结果
        boolean accept = false;
        boolean processingRequired = true;
        String rejectReason = null;
        
        // 如果达到自动通过等级，直接同意
        if (autoAcceptLevel) {
            logger.info("用户 {} 等级达到自动通过标准，直接同意", userId);
            accept = true;
        } else {
            // 根据不同验证模式处理
            switch (verifyTask.getVerifyMode()) {
                case IGNORE_ALL:
                    // 忽略所有验证，直接同意
                    logger.info("验证模式为忽略所有，直接同意用户 {} 的请求", userId);
                    accept = true;
                    break;
                    
                case ANY_ONE_PASS:
                    // 任意一个验证通过即可
                    accept = answerCorrect || levelPassed;
                    if (!accept) {
                        rejectReason = "答案验证和等级验证均未通过";
                    }
                    break;
                    
                case BOTH_REQUIRED:
                    // 必须两个验证都通过
                    accept = answerCorrect && levelPassed;
                    if (!accept) {
                        if (!answerCorrect && !levelPassed) {
                            rejectReason = "答案验证和等级验证均未通过";
                        } else if (!answerCorrect) {
                            rejectReason = "答案验证未通过";
                        } else {
                            rejectReason = "等级验证未通过";
                        }
                    }
                    break;
                    
                case ANSWER_ONLY:
                    // 只检查答案
                    accept = answerCorrect;
                    if (!accept) {
                        rejectReason = "答案验证未通过";
                    }
                    break;
                    
                case LEVEL_ONLY:
                    // 只检查等级
                    accept = levelPassed;
                    if (!accept) {
                        rejectReason = "等级验证未通过";
                    }
                    break;
                    
                case ANSWER_PASS_LEVEL_PENDING:
                    // 答案通过但等级未通过时挂起
                    if (answerCorrect && levelPassed) {
                        accept = true;
                    } else if (answerCorrect) {
                        // 答案通过但等级未通过，不处理
                        processingRequired = false;
                        logger.info("用户 {} 答案验证通过但等级未达标，请求挂起", userId);
                        ConsoleUtil.info("用户 " + userId + " 答案验证通过但等级未达标，请求挂起");
                    } else {
                        // 答案未通过，拒绝
                        accept = false;
                        rejectReason = "答案验证未通过";
                    }
                    break;
                    
                case LEVEL_PASS_ANSWER_PENDING:
                    // 等级通过但答案未通过时挂起
                    if (answerCorrect && levelPassed) {
                        accept = true;
                    } else if (levelPassed) {
                        // 等级通过但答案未通过，不处理
                        processingRequired = false;
                        logger.info("用户 {} 等级验证通过但答案未通过，请求挂起", userId);
                        ConsoleUtil.info("用户 " + userId + " 等级验证通过但答案未通过，请求挂起");
                    } else {
                        // 等级未通过，拒绝
                        accept = false;
                        rejectReason = "等级验证未通过";
                    }
                    break;
            }
        }
        
        // 处理请求
        if (processingRequired) {
            if (accept) {
                // 同意请求
                client.handleGroupRequest(flag, true, null);
                logger.info("验证通过，同意 {} 加入群 {}", userId, groupId);
                ConsoleUtil.success("验证通过，同意 " + userId + " 加入群 " + groupId);
            } else {
                // 拒绝请求
                String rejectMessage = verifyTask.getRejectMessage();
                if (rejectMessage == null || rejectMessage.isEmpty()) {
                    if (rejectReason != null) {
                        rejectMessage = "验证未通过，原因：" + rejectReason;
                    } else {
                        rejectMessage = "验证未通过，请重新申请并正确回答问题：" + verifyTask.getVerifyQuestion();
                    }
                }
                
                client.handleGroupRequest(flag, false, rejectMessage);
                logger.info("验证失败，拒绝 {} 加入群 {}, 原因: {}", userId, groupId, rejectReason);
                ConsoleUtil.warn("验证失败，拒绝 " + userId + " 加入群 " + groupId);
            }
            
            // 处理完毕，从缓存中移除
            pendingRequests.remove(flag);
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