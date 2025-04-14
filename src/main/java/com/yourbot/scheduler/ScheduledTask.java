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
    
    // 进群验证相关属性
    private String verifyQuestion;  // 验证问题
    private List<String> verifyAnswers = new ArrayList<>(); // 有效验证答案列表（支持多个正确答案）
    private String rejectMessage;   // 拒绝消息
    private boolean caseSensitive = false; // 答案是否区分大小写
    
    // 新增验证相关属性
    private boolean ignoreWhitespace = false; // 答案是否忽略空格
    private boolean fuzzyMatch = false;       // 答案是否模糊匹配
    private int minLevel = 0;                 // 最低等级要求（0表示不检查）
    private int maxAutoAcceptLevel = 0;       // 达到多少等级时自动同意（0表示不自动同意）
    private VerifyMode verifyMode = VerifyMode.ANSWER_ONLY; // 验证模式
    
    // 验证模式枚举
    public enum VerifyMode {
        IGNORE_ALL,              // 都忽略
        ANY_ONE_PASS,            // 仅一个满足即可同意
        BOTH_REQUIRED,           // 都满足条件同意
        ANSWER_ONLY,             // 满足答案验证同意
        LEVEL_ONLY,              // 满足等级验证同意
        ANSWER_PASS_LEVEL_PENDING, // 满足答案验证但等级不符合要求将不处理
        LEVEL_PASS_ANSWER_PENDING  // 满足等级验证但答案验证不符合要求将不处理
    }
    
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
    
    // 兼容单个验证答案的setter
    public void setVerifyAnswer(String verifyAnswer) {
        this.verifyAnswers.clear();
        if (verifyAnswer != null && !verifyAnswer.isEmpty()) {
            this.verifyAnswers.add(verifyAnswer);
        }
    }
    
    // 兼容单个验证答案的getter
    public String getVerifyAnswer() {
        return verifyAnswers.isEmpty() ? "" : verifyAnswers.get(0);
    }
    
    /**
     * 检查答案是否正确
     * @param answer 用户提供的答案
     * @return 是否正确
     */
    public boolean checkAnswer(String answer) {
        if (answer == null || answer.isEmpty() || verifyAnswers.isEmpty()) {
            return false;
        }
        
        // 处理空格忽略
        String processedUserAnswer = answer;
        if (ignoreWhitespace) {
            processedUserAnswer = answer.replaceAll("\\s+", "");
        }
        
        for (String correctAnswer : verifyAnswers) {
            String processedCorrectAnswer = correctAnswer;
            if (ignoreWhitespace) {
                processedCorrectAnswer = correctAnswer.replaceAll("\\s+", "");
            }
            
            // 不区分大小写情况
            if (!caseSensitive) {
                processedUserAnswer = processedUserAnswer.toLowerCase();
                processedCorrectAnswer = processedCorrectAnswer.toLowerCase();
            }
            
            // 检查答案
            if (fuzzyMatch) {
                // 模糊匹配，只要用户答案包含正确答案或正确答案包含用户答案就算通过
                if (processedUserAnswer.contains(processedCorrectAnswer) || 
                    processedCorrectAnswer.contains(processedUserAnswer)) {
                    return true;
                }
            } else {
                // 精确匹配
                if (processedCorrectAnswer.equals(processedUserAnswer)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查用户等级是否符合要求
     * @param userLevel 用户等级
     * @return 是否符合要求
     */
    public boolean checkLevel(int userLevel) {
        // 如果没有设置最低等级要求，则默认通过
        if (minLevel <= 0) {
            return true;
        }
        
        // 检查用户等级是否达到最低要求
        return userLevel >= minLevel;
    }
    
    /**
     * 检查用户是否达到自动通过等级
     * @param userLevel 用户等级
     * @return 是否达到自动通过等级
     */
    public boolean checkAutoAcceptLevel(int userLevel) {
        // 如果没有设置自动通过等级，或设置为0，则不自动通过
        if (maxAutoAcceptLevel <= 0) {
            return false;
        }
        
        // 检查用户等级是否达到自动通过等级
        return userLevel >= maxAutoAcceptLevel;
    }
} 