package com.yourbot.command;

import com.yourbot.onebot.GroupRequestManager;
import com.yourbot.onebot.GroupRequestManager.PendingGroupRequest;
import com.yourbot.util.ConsoleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * 进群申请管理命令
 */
public class GroupRequestCommand {
    private static final Logger logger = LoggerFactory.getLogger(GroupRequestCommand.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private final GroupRequestManager requestManager = GroupRequestManager.getInstance();
    private final Scanner scanner = new Scanner(System.in);
    
    /**
     * 显示进群申请管理菜单
     */
    public void showMenu() {
        while (true) {
            ConsoleUtil.info("\n=== 进群申请管理 ===");
            ConsoleUtil.info("1. 查看所有挂起的申请");
            ConsoleUtil.info("2. 按群号查看挂起的申请");
            ConsoleUtil.info("3. 同意申请");
            ConsoleUtil.info("4. 拒绝申请");
            ConsoleUtil.info("5. 清理过期申请");
            ConsoleUtil.info("6. 查看申请统计");
            ConsoleUtil.info("0. 返回主菜单");
            ConsoleUtil.info("请选择操作: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    showAllSuspendedRequests();
                    break;
                case "2":
                    showRequestsByGroup();
                    break;
                case "3":
                    approveRequest();
                    break;
                case "4":
                    rejectRequest();
                    break;
                case "5":
                    cleanupExpiredRequests();
                    break;
                case "6":
                    showStatistics();
                    break;
                case "0":
                    return;
                default:
                    ConsoleUtil.warn("无效的选择，请重新输入");
            }
        }
    }
    
    /**
     * 显示所有挂起的申请
     */
    private void showAllSuspendedRequests() {
        List<PendingGroupRequest> requests = requestManager.getSuspendedRequests();
        
        if (requests.isEmpty()) {
            ConsoleUtil.info("当前没有挂起的申请");
            return;
        }
        
        ConsoleUtil.info("\n=== 所有挂起的申请 ===");
        for (int i = 0; i < requests.size(); i++) {
            PendingGroupRequest request = requests.get(i);
            ConsoleUtil.info(String.format("%d. 群号: %d, 用户: %d, 标识: %s", 
                    i + 1, request.getGroupId(), request.getUserId(), request.getFlag()));
            ConsoleUtil.info(String.format("   申请信息: %s", request.getComment()));
            ConsoleUtil.info(String.format("   挂起原因: %s", request.getReason()));
            ConsoleUtil.info(String.format("   申请时间: %s", dateFormat.format(new Date(request.getTimestamp()))));
            ConsoleUtil.info("");
        }
    }
    
    /**
     * 按群号查看挂起的申请
     */
    private void showRequestsByGroup() {
        ConsoleUtil.info("请输入群号: ");
        String input = scanner.nextLine().trim();
        
        try {
            long groupId = Long.parseLong(input);
            List<PendingGroupRequest> requests = requestManager.getSuspendedRequestsByGroup(groupId);
            
            if (requests.isEmpty()) {
                ConsoleUtil.info("群 " + groupId + " 没有挂起的申请");
                return;
            }
            
            ConsoleUtil.info("\n=== 群 " + groupId + " 的挂起申请 ===");
            for (int i = 0; i < requests.size(); i++) {
                PendingGroupRequest request = requests.get(i);
                ConsoleUtil.info(String.format("%d. 用户: %d, 标识: %s", 
                        i + 1, request.getUserId(), request.getFlag()));
                ConsoleUtil.info(String.format("   申请信息: %s", request.getComment()));
                ConsoleUtil.info(String.format("   挂起原因: %s", request.getReason()));
                ConsoleUtil.info(String.format("   申请时间: %s", dateFormat.format(new Date(request.getTimestamp()))));
                ConsoleUtil.info("");
            }
        } catch (NumberFormatException e) {
            ConsoleUtil.error("无效的群号格式");
        }
    }
    
    /**
     * 同意申请
     */
    private void approveRequest() {
        ConsoleUtil.info("请输入要同意的申请标识(flag): ");
        String flag = scanner.nextLine().trim();
        
        if (flag.isEmpty()) {
            ConsoleUtil.error("申请标识不能为空");
            return;
        }
        
        PendingGroupRequest request = requestManager.getRequestByFlag(flag);
        if (request == null) {
            ConsoleUtil.error("未找到标识为 " + flag + " 的挂起申请");
            return;
        }
        
        ConsoleUtil.info("申请详情:");
        ConsoleUtil.info(String.format("群号: %d, 用户: %d", request.getGroupId(), request.getUserId()));
        ConsoleUtil.info(String.format("申请信息: %s", request.getComment()));
        ConsoleUtil.info(String.format("挂起原因: %s", request.getReason()));
        
        ConsoleUtil.info("确认同意此申请吗？(y/N): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if ("y".equals(confirm) || "yes".equals(confirm)) {
            boolean success = requestManager.approveRequest(flag);
            if (success) {
                ConsoleUtil.success("申请已同意");
            } else {
                ConsoleUtil.error("同意申请失败");
            }
        } else {
            ConsoleUtil.info("操作已取消");
        }
    }
    
    /**
     * 拒绝申请
     */
    private void rejectRequest() {
        ConsoleUtil.info("请输入要拒绝的申请标识(flag): ");
        String flag = scanner.nextLine().trim();
        
        if (flag.isEmpty()) {
            ConsoleUtil.error("申请标识不能为空");
            return;
        }
        
        PendingGroupRequest request = requestManager.getRequestByFlag(flag);
        if (request == null) {
            ConsoleUtil.error("未找到标识为 " + flag + " 的挂起申请");
            return;
        }
        
        ConsoleUtil.info("申请详情:");
        ConsoleUtil.info(String.format("群号: %d, 用户: %d", request.getGroupId(), request.getUserId()));
        ConsoleUtil.info(String.format("申请信息: %s", request.getComment()));
        ConsoleUtil.info(String.format("挂起原因: %s", request.getReason()));
        
        ConsoleUtil.info("请输入拒绝原因（可选，直接回车使用默认原因）: ");
        String reason = scanner.nextLine().trim();
        
        ConsoleUtil.info("确认拒绝此申请吗？(y/N): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if ("y".equals(confirm) || "yes".equals(confirm)) {
            boolean success = requestManager.rejectRequest(flag, reason.isEmpty() ? null : reason);
            if (success) {
                ConsoleUtil.success("申请已拒绝");
            } else {
                ConsoleUtil.error("拒绝申请失败");
            }
        } else {
            ConsoleUtil.info("操作已取消");
        }
    }
    
    /**
     * 清理过期申请
     */
    private void cleanupExpiredRequests() {
        ConsoleUtil.info("正在清理过期的挂起申请...");
        requestManager.cleanupExpiredRequests();
        ConsoleUtil.success("清理完成");
    }
    

    
    // 以下是公共方法，供命令行直接调用
    
    /**
     * 显示所有挂起的申请（公共方法）
     */
    public void showAllRequests() {
        showAllSuspendedRequests();
    }
    
    /**
     * 显示指定群的挂起申请（公共方法）
     */
    public void showRequestsByGroup(long groupId) {
        List<PendingGroupRequest> requests = requestManager.getSuspendedRequestsByGroup(groupId);
        
        if (requests.isEmpty()) {
            ConsoleUtil.info("群 " + groupId + " 没有挂起的申请");
            return;
        }
        
        ConsoleUtil.info("\n=== 群 " + groupId + " 的挂起申请 ===");
        for (int i = 0; i < requests.size(); i++) {
            PendingGroupRequest request = requests.get(i);
            ConsoleUtil.info(String.format("%d. 用户: %d, 标识: %s", 
                    i + 1, request.getUserId(), request.getFlag()));
            ConsoleUtil.info(String.format("   申请信息: %s", request.getComment()));
            ConsoleUtil.info(String.format("   挂起原因: %s", request.getReason()));
            ConsoleUtil.info(String.format("   申请时间: %s", dateFormat.format(new Date(request.getTimestamp()))));
            ConsoleUtil.info("");
        }
    }
    
    /**
     * 同意申请（公共方法）
     */
    public void approveRequest(String flag) {
        if (flag == null || flag.trim().isEmpty()) {
            ConsoleUtil.error("申请标识不能为空");
            return;
        }
        
        PendingGroupRequest request = requestManager.getRequestByFlag(flag);
        if (request == null) {
            ConsoleUtil.error("未找到标识为 " + flag + " 的挂起申请");
            return;
        }
        
        ConsoleUtil.info("申请详情:");
        ConsoleUtil.info(String.format("群号: %d, 用户: %d", request.getGroupId(), request.getUserId()));
        ConsoleUtil.info(String.format("申请信息: %s", request.getComment()));
        ConsoleUtil.info(String.format("挂起原因: %s", request.getReason()));
        
        boolean success = requestManager.approveRequest(flag);
        if (success) {
            ConsoleUtil.success("申请已同意");
        } else {
            ConsoleUtil.error("同意申请失败");
        }
    }
    
    /**
     * 拒绝申请（公共方法）
     */
    public void rejectRequest(String flag) {
        rejectRequest(flag, null);
    }
    
    /**
     * 拒绝申请（公共方法，带拒绝原因）
     */
    public void rejectRequest(String flag, String reason) {
        if (flag == null || flag.trim().isEmpty()) {
            ConsoleUtil.error("申请标识不能为空");
            return;
        }
        
        PendingGroupRequest request = requestManager.getRequestByFlag(flag);
        if (request == null) {
            ConsoleUtil.error("未找到标识为 " + flag + " 的挂起申请");
            return;
        }
        
        ConsoleUtil.info("申请详情:");
        ConsoleUtil.info(String.format("群号: %d, 用户: %d", request.getGroupId(), request.getUserId()));
        ConsoleUtil.info(String.format("申请信息: %s", request.getComment()));
        ConsoleUtil.info(String.format("挂起原因: %s", request.getReason()));
        
        boolean success = requestManager.rejectRequest(flag, reason);
        if (success) {
            ConsoleUtil.success("申请已拒绝");
        } else {
            ConsoleUtil.error("拒绝申请失败");
        }
    }
    
    /**
     * 清理过期申请（公共方法）
     */
    public void cleanExpiredRequests() {
        ConsoleUtil.info("正在清理过期的挂起申请...");
        requestManager.cleanupExpiredRequests();
        ConsoleUtil.success("清理完成");
    }
    
    /**
     * 显示申请统计（公共方法）
     */
    public void showStatistics() {
        int suspendedCount = requestManager.getSuspendedRequestCount();
        
        ConsoleUtil.info("\n=== 申请统计 ===");
        ConsoleUtil.info("当前挂起的申请数量: " + suspendedCount);
        
        if (suspendedCount > 0) {
            List<PendingGroupRequest> requests = requestManager.getSuspendedRequests();
            
            // 按群号统计
            ConsoleUtil.info("\n按群号统计:");
            requests.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            PendingGroupRequest::getGroupId,
                            java.util.stream.Collectors.counting()))
                    .forEach((groupId, count) -> 
                            ConsoleUtil.info(String.format("群 %d: %d 个申请", groupId, count)));
        }
    }
}