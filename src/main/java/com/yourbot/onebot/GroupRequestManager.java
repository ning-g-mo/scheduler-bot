package com.yourbot.onebot;

import com.yourbot.util.ConsoleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 进群申请管理器
 * 提供对挂起和待处理的进群申请的管理功能
 */
public class GroupRequestManager {
    private static final Logger logger = LoggerFactory.getLogger(GroupRequestManager.class);
    
    private static GroupRequestManager instance;
    
    // 挂起的进群请求，键为请求标识(flag)，值为请求信息
    private final Map<String, PendingGroupRequest> suspendedRequests = new ConcurrentHashMap<>();
    
    // OneBotClient实例
    private final OneBotClient client = OneBotClient.getInstance();
    
    private GroupRequestManager() {}
    
    public static synchronized GroupRequestManager getInstance() {
        if (instance == null) {
            instance = new GroupRequestManager();
        }
        return instance;
    }
    
    /**
     * 添加挂起的请求
     */
    public void addSuspendedRequest(long groupId, long userId, String flag, String comment, String reason) {
        PendingGroupRequest request = new PendingGroupRequest();
        request.setGroupId(groupId);
        request.setUserId(userId);
        request.setFlag(flag);
        request.setComment(comment);
        request.setReason(reason);
        request.setTimestamp(System.currentTimeMillis());
        request.setStatus(RequestStatus.SUSPENDED);
        
        suspendedRequests.put(flag, request);
        
        logger.info("进群申请已挂起: 群 {}, 用户 {}, 原因: {}", groupId, userId, reason);
        ConsoleUtil.info("进群申请已挂起: 群 " + groupId + ", 用户 " + userId + ", 原因: " + reason);
    }
    
    /**
     * 手动同意挂起的请求
     */
    public boolean approveRequest(String flag) {
        PendingGroupRequest request = suspendedRequests.get(flag);
        if (request == null) {
            logger.warn("未找到标识为 {} 的挂起请求", flag);
            ConsoleUtil.warn("未找到标识为 " + flag + " 的挂起请求");
            return false;
        }
        
        boolean success = client.handleGroupRequest(flag, true, null);
        if (success) {
            request.setStatus(RequestStatus.APPROVED);
            suspendedRequests.remove(flag);
            
            logger.info("手动同意进群申请: 群 {}, 用户 {}", request.getGroupId(), request.getUserId());
            ConsoleUtil.success("手动同意进群申请: 群 " + request.getGroupId() + ", 用户 " + request.getUserId());
        } else {
            logger.error("同意进群申请失败: 群 {}, 用户 {}", request.getGroupId(), request.getUserId());
            ConsoleUtil.error("同意进群申请失败: 群 " + request.getGroupId() + ", 用户 " + request.getUserId());
        }
        
        return success;
    }
    
    /**
     * 手动拒绝挂起的请求
     */
    public boolean rejectRequest(String flag, String reason) {
        PendingGroupRequest request = suspendedRequests.get(flag);
        if (request == null) {
            logger.warn("未找到标识为 {} 的挂起请求", flag);
            ConsoleUtil.warn("未找到标识为 " + flag + " 的挂起请求");
            return false;
        }
        
        String rejectMessage = reason != null && !reason.trim().isEmpty() ? reason : "申请被拒绝";
        boolean success = client.handleGroupRequest(flag, false, rejectMessage);
        
        if (success) {
            request.setStatus(RequestStatus.REJECTED);
            suspendedRequests.remove(flag);
            
            logger.info("手动拒绝进群申请: 群 {}, 用户 {}, 原因: {}", request.getGroupId(), request.getUserId(), rejectMessage);
            ConsoleUtil.warn("手动拒绝进群申请: 群 " + request.getGroupId() + ", 用户 " + request.getUserId() + ", 原因: " + rejectMessage);
        } else {
            logger.error("拒绝进群申请失败: 群 {}, 用户 {}", request.getGroupId(), request.getUserId());
            ConsoleUtil.error("拒绝进群申请失败: 群 " + request.getGroupId() + ", 用户 " + request.getUserId());
        }
        
        return success;
    }
    
    /**
     * 获取所有挂起的请求
     */
    public List<PendingGroupRequest> getSuspendedRequests() {
        return suspendedRequests.values().stream()
                .filter(request -> request.getStatus() == RequestStatus.SUSPENDED)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定群的挂起请求
     */
    public List<PendingGroupRequest> getSuspendedRequestsByGroup(long groupId) {
        return suspendedRequests.values().stream()
                .filter(request -> request.getStatus() == RequestStatus.SUSPENDED && request.getGroupId() == groupId)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据flag获取请求信息
     */
    public PendingGroupRequest getRequestByFlag(String flag) {
        return suspendedRequests.get(flag);
    }
    
    /**
     * 清理过期的挂起请求（超过7天）
     */
    public void cleanupExpiredRequests() {
        long expireTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); // 7天
        
        List<String> expiredFlags = suspendedRequests.entrySet().stream()
                .filter(entry -> entry.getValue().getTimestamp() < expireTime)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        for (String flag : expiredFlags) {
            PendingGroupRequest request = suspendedRequests.remove(flag);
            logger.info("清理过期的挂起请求: 群 {}, 用户 {}", request.getGroupId(), request.getUserId());
        }
        
        if (!expiredFlags.isEmpty()) {
            ConsoleUtil.info("清理了 " + expiredFlags.size() + " 个过期的挂起请求");
        }
    }
    
    /**
     * 获取挂起请求数量
     */
    public int getSuspendedRequestCount() {
        return (int) suspendedRequests.values().stream()
                .filter(request -> request.getStatus() == RequestStatus.SUSPENDED)
                .count();
    }
    
    /**
     * 挂起的进群请求信息
     */
    public static class PendingGroupRequest {
        private long groupId;
        private long userId;
        private String flag;
        private String comment;
        private String reason;
        private long timestamp;
        private RequestStatus status;
        
        // Getters and Setters
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
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public RequestStatus getStatus() {
            return status;
        }
        
        public void setStatus(RequestStatus status) {
            this.status = status;
        }
        
        @Override
        public String toString() {
            return String.format("PendingGroupRequest{groupId=%d, userId=%d, flag='%s', comment='%s', reason='%s', status=%s}",
                    groupId, userId, flag, comment, reason, status);
        }
    }
    
    /**
     * 请求状态枚举
     */
    public enum RequestStatus {
        SUSPENDED,  // 挂起
        APPROVED,   // 已同意
        REJECTED    // 已拒绝
    }
}