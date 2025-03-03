package com.yourbot.scheduler;

import com.yourbot.config.ConfigManager;
import com.yourbot.onebot.OneBotClient;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import com.yourbot.util.ConsoleUtil;

public class SchedulerManager {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);
    
    private static SchedulerManager instance;
    private Scheduler scheduler;
    private OneBotClient oneBotClient;
    
    private SchedulerManager() {
        try {
            logger.debug("初始化Quartz调度器");
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            logger.info("Quartz调度器已启动");
            
            logger.debug("初始化OneBot客户端");
            oneBotClient = OneBotClient.getInstance();
        } catch (SchedulerException e) {
            logger.error("初始化定时任务管理器失败", e);
            System.err.println("初始化定时任务管理器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static SchedulerManager getInstance() {
        if (instance == null) {
            logger.debug("创建SchedulerManager实例");
            instance = new SchedulerManager();
        }
        return instance;
    }
    
    public void loadTasks() {
        try {
            // 清除所有现有任务
            logger.info("清除所有现有任务");
            scheduler.clear();
            
            List<ScheduledTask> tasks = ConfigManager.getInstance().getScheduledTasks();
            if (tasks == null || tasks.isEmpty()) {
                logger.warn("没有找到定时任务配置");
                System.out.println("没有找到定时任务配置");
                return;
            }
            
            logger.info("开始加载 {} 个定时任务", tasks.size());
            for (ScheduledTask task : tasks) {
                scheduleTask(task);
            }
            
            logger.info("成功加载 {} 个定时任务", tasks.size());
            System.out.println("成功加载 " + tasks.size() + " 个定时任务");
        } catch (SchedulerException e) {
            logger.error("加载定时任务失败", e);
            System.err.println("加载定时任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void scheduleTask(ScheduledTask task) throws SchedulerException {
        logger.debug("开始调度任务: {}", task.getName());
        
        try {
            // 验证Cron表达式
            CronScheduleBuilder.cronSchedule(task.getCronExpression());
        } catch (Exception e) {
            logger.error("任务 {} 的Cron表达式 {} 无效", task.getName(), task.getCronExpression(), e);
            System.err.println("任务 " + task.getName() + " 的Cron表达式无效: " + e.getMessage());
            return;
        }
        
        JobDetail job = JobBuilder.newJob(TaskJob.class)
                .withIdentity(task.getName(), "tasks")
                .build();
        
        job.getJobDataMap().put("task", task);
        job.getJobDataMap().put("oneBotClient", oneBotClient);
        
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(task.getName() + "_trigger", "tasks")
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                .build();
        
        scheduler.scheduleJob(job, trigger);
        
        logger.info("已加载定时任务: {} ({})", task.getName(), task.getCronExpression());
        logger.debug("任务详情: 类型={}, 目标类型={}, 目标ID={}", 
                task.getType(), task.getTargetType(), task.getTargetId());
        
        // 计算下一次执行时间
        Date nextFireTime = trigger.getNextFireTime();
        logger.info("任务 {} 下一次执行时间: {}", task.getName(), nextFireTime);
        
        System.out.println("已加载定时任务: " + task.getName() + " (" + task.getCronExpression() + ")");
        System.out.println("下一次执行时间: " + nextFireTime);
    }
    
    public static class TaskJob implements Job {
        private static final Logger jobLogger = LoggerFactory.getLogger(TaskJob.class);
        
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            ScheduledTask task = (ScheduledTask) dataMap.get("task");
            OneBotClient client = (OneBotClient) dataMap.get("oneBotClient");
            
            jobLogger.info("执行定时任务: {}", task.getName());
            ConsoleUtil.task(task.getName(), "开始执行");
            
            try {
                switch (task.getType()) {
                    case SEND_MESSAGE:
                        if ("GROUP".equals(task.getTargetType())) {
                            jobLogger.info("发送群消息到 {}: {}", task.getTargetId(), task.getContent());
                            ConsoleUtil.task(task.getName(), "发送群消息到 " + task.getTargetId());
                            client.sendGroupMessage(task.getTargetId(), task.getContent());
                        } else if ("PRIVATE".equals(task.getTargetType())) {
                            jobLogger.info("发送私聊消息到 {}: {}", task.getTargetId(), task.getContent());
                            ConsoleUtil.task(task.getName(), "发送私聊消息到 " + task.getTargetId());
                            client.sendPrivateMessage(task.getTargetId(), task.getContent());
                        } else {
                            jobLogger.warn("未知的目标类型: {}", task.getTargetType());
                            ConsoleUtil.warn("未知的目标类型: " + task.getTargetType());
                        }
                        break;
                        
                    case GROUP_BAN_ALL:
                        jobLogger.info("设置群 {} 全体禁言: {}", task.getTargetId(), task.isEnable());
                        ConsoleUtil.task(task.getName(), "设置群 " + task.getTargetId() + " 全体" + (task.isEnable() ? "禁言" : "解禁"));
                        client.setGroupWholeBan(task.getTargetId(), task.isEnable());
                        
                        // 发送禁言/解禁通知
                        if (task.isSendNotice() && task.getNoticeContent() != null && !task.getNoticeContent().isEmpty()) {
                            String noticeMsg = task.getNoticeContent();
                            jobLogger.info("发送全体{}通知: {}", task.isEnable() ? "禁言" : "解禁", noticeMsg);
                            ConsoleUtil.task(task.getName(), "发送全体" + (task.isEnable() ? "禁言" : "解禁") + "通知");
                            client.sendGroupMessage(task.getTargetId(), noticeMsg);
                        }
                        break;
                        
                    case GROUP_BAN_MEMBER:
                        jobLogger.info("设置群 {} 成员 {} 禁言 {} 秒", 
                                task.getTargetId(), task.getMemberId(), task.getDuration());
                        ConsoleUtil.task(task.getName(), "设置群 " + task.getTargetId() + " 成员 " + task.getMemberId() + 
                                (task.getDuration() > 0 ? " 禁言 " + formatDuration(task.getDuration()) : " 解除禁言"));
                        client.setGroupBan(task.getTargetId(), task.getMemberId(), task.getDuration());
                        
                        // 发送禁言/解禁通知
                        if (task.isSendNotice() && task.getNoticeContent() != null && !task.getNoticeContent().isEmpty()) {
                            String noticeMsg = task.getNoticeContent();
                            // 替换变量
                            noticeMsg = noticeMsg.replace("{memberId}", String.valueOf(task.getMemberId()))
                                                .replace("{duration}", formatDuration(task.getDuration()));
                            
                            jobLogger.info("发送成员{}通知: {}", task.getDuration() > 0 ? "禁言" : "解禁", noticeMsg);
                            ConsoleUtil.task(task.getName(), "发送成员" + (task.getDuration() > 0 ? "禁言" : "解禁") + "通知");
                            client.sendGroupMessage(task.getTargetId(), noticeMsg);
                        }
                        break;
                        
                    default:
                        jobLogger.warn("未知的任务类型: {}", task.getType());
                        ConsoleUtil.warn("未知的任务类型: " + task.getType());
                        break;
                }
                
                jobLogger.info("任务 {} 执行完成", task.getName());
                ConsoleUtil.task(task.getName(), "执行完成");
            } catch (Exception e) {
                jobLogger.error("执行定时任务失败: {}", task.getName(), e);
                ConsoleUtil.error("执行定时任务失败: " + task.getName() + " - " + e.getMessage());
                e.printStackTrace();
                throw new JobExecutionException(e);
            }
        }
        
        /**
         * 格式化禁言时长
         */
        private String formatDuration(int seconds) {
            if (seconds <= 0) {
                return "永久";
            }
            
            StringBuilder sb = new StringBuilder();
            int days = seconds / (24 * 3600);
            seconds %= (24 * 3600);
            int hours = seconds / 3600;
            seconds %= 3600;
            int minutes = seconds / 60;
            seconds %= 60;
            
            if (days > 0) {
                sb.append(days).append("天");
            }
            if (hours > 0) {
                sb.append(hours).append("小时");
            }
            if (minutes > 0) {
                sb.append(minutes).append("分钟");
            }
            if (seconds > 0 || sb.length() == 0) {
                sb.append(seconds).append("秒");
            }
            
            return sb.toString();
        }
    }
} 