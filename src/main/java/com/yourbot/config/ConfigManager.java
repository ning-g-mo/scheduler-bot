package com.yourbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.yourbot.scheduler.ScheduledTask;
import com.yourbot.scheduler.TaskType;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourbot.util.ConsoleUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    private static ConfigManager instance;
    private BotConfig botConfig;
    private List<ScheduledTask> scheduledTasks = new ArrayList<>();
    private final String configPath = "config.yml";
    
    private ConfigManager() {
        loadConfig();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            logger.debug("创建ConfigManager实例");
            instance = new ConfigManager();
        }
        return instance;
    }
    
    public void loadConfig() {
        logger.info("开始加载配置文件: {}", configPath);
        ConsoleUtil.info("开始加载配置文件: " + configPath);
        
        try {
            File configFile = new File(configPath);
            if (!configFile.exists()) {
                logger.warn("配置文件不存在: {}", configPath);
                ConsoleUtil.warn("配置文件不存在: " + configPath);
                
                // 创建默认配置文件
                createDefaultConfig(configFile);
                ConsoleUtil.success("已创建默认配置文件: " + configPath);
                logger.info("已创建默认配置文件: {}", configPath);
            }
            
            logger.debug("解析YAML配置文件，使用UTF-8编码");
            ConsoleUtil.debug("解析YAML配置文件，使用UTF-8编码");
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            // 确保使用UTF-8编码
            mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
            Config config = mapper.readValue(configFile, Config.class);
            
            this.botConfig = config.getBot();
            this.scheduledTasks = config.getScheduledTasks();
            
            logger.info("配置文件加载成功，共加载 {} 个定时任务", scheduledTasks.size());
            ConsoleUtil.success("配置文件加载成功，共加载 " + scheduledTasks.size() + " 个定时任务");
            
            logger.debug("机器人配置: {}", botConfig);
            ConsoleUtil.debug("机器人配置: websocket=" + botConfig.getWebsocket());
            
            for (ScheduledTask task : scheduledTasks) {
                logger.debug("加载任务: {}, 类型: {}, Cron表达式: {}", 
                        task.getName(), task.getType(), task.getCronExpression());
                ConsoleUtil.debug("加载任务: " + task.getName() + ", 类型: " + task.getType() + 
                        ", Cron表达式: " + task.getCronExpression());
            }
        } catch (IOException e) {
            logger.error("加载配置文件失败", e);
            ConsoleUtil.error("加载配置文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig(File configFile) throws IOException {
        logger.info("创建默认配置文件: {}", configFile.getAbsolutePath());
        ConsoleUtil.info("创建默认配置文件...");
        
        // 创建默认机器人配置
        BotConfig defaultBotConfig = new BotConfig();
        defaultBotConfig.setWebsocket("ws://127.0.0.1:6700");
        defaultBotConfig.setAccessToken("");
        
        // 创建默认定时任务
        List<ScheduledTask> defaultTasks = createDefaultTasks();
        
        // 创建配置对象
        Config config = new Config();
        config.setBot(defaultBotConfig);
        config.setScheduledTasks(defaultTasks);
        
        // 写入配置文件
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValue(configFile, config);
        
        logger.info("默认配置文件创建成功，包含 {} 个示例任务", defaultTasks.size());
        ConsoleUtil.info("默认配置文件创建成功，包含 " + defaultTasks.size() + " 个示例任务");
    }
    
    /**
     * 创建默认定时任务列表
     */
    private List<ScheduledTask> createDefaultTasks() {
        List<ScheduledTask> tasks = new ArrayList<>();
        
        // 示例1：早安问候
        ScheduledTask task1 = new ScheduledTask();
        task1.setName("早安问候");
        task1.setType(TaskType.SEND_MESSAGE);
        task1.setTargetType("GROUP");
        task1.getTargetIds().add(123456789L); // 示例群号1
        task1.getTargetIds().add(987654321L); // 示例群号2
        task1.setCronExpression("0 30 7 * * ?");
        task1.setContent("早上好，今天也要元气满满哦！");
        tasks.add(task1);
        
        // 示例2：晚间提醒
        ScheduledTask task2 = new ScheduledTask();
        task2.setName("晚间提醒");
        task2.setType(TaskType.SEND_MESSAGE);
        task2.setTargetType("PRIVATE");
        task2.setTargetId(111222333); // 示例QQ号，用户需要修改
        task2.setCronExpression("0 0 22 * * ?"); // 每天晚上10点
        task2.setContent("该休息了，记得早点睡觉哦~");
        tasks.add(task2);
        
        // 示例3：周末全体禁言
        ScheduledTask task3 = new ScheduledTask();
        task3.setName("周末全体禁言");
        task3.setType(TaskType.GROUP_BAN_ALL);
        task3.setTargetType("GROUP");
        task3.setTargetId(123456789); // 示例群号，用户需要修改
        task3.setCronExpression("0 0 23 ? * FRI"); // 每周五晚上11点
        task3.setEnable(true); // 开启全体禁言
        task3.setSendNotice(true);
        task3.setNoticeContent("周末愉快！全体禁言开启，请各位周一见~");
        tasks.add(task3);
        
        // 示例4：周一解除全体禁言
        ScheduledTask task4 = new ScheduledTask();
        task4.setName("周一解除全体禁言");
        task4.setType(TaskType.GROUP_BAN_ALL);
        task4.setTargetType("GROUP");
        task4.setTargetId(123456789); // 示例群号，用户需要修改
        task4.setCronExpression("0 0 8 ? * MON"); // 每周一早上8点
        task4.setEnable(false); // 关闭全体禁言
        task4.setSendNotice(true);
        task4.setNoticeContent("早上好！新的一周开始了，全体禁言已解除~");
        tasks.add(task4);
        
        // 示例5：特定用户禁言
        ScheduledTask task5 = new ScheduledTask();
        task5.setName("特定用户禁言");
        task5.setType(TaskType.GROUP_BAN_MEMBER);
        task5.setTargetType("GROUP");
        task5.setTargetId(123456789); // 示例群号，用户需要修改
        task5.setMemberId(111222333); // 示例成员QQ号，用户需要修改
        task5.setCronExpression("0 0 12 * * ?"); // 每天中午12点
        task5.setDuration(3600); // 禁言1小时
        task5.setSendNotice(true);
        task5.setNoticeContent("成员 {memberId} 已被禁言 {duration}，请遵守群规则。");
        tasks.add(task5);
        
        // 示例6：进群验证 - 基础答案验证
        ScheduledTask task6 = new ScheduledTask();
        task6.setName("默认进群验证");
        task6.setType(TaskType.GROUP_REQUEST_VERIFY);
        task6.setTargetType("GROUP");
        task6.setTargetId(123456789); // 示例群号，用户需要修改
        task6.setVerifyQuestion("请回答: 1+1=?");
        task6.setVerifyAnswer("2"); // 设置正确答案
        task6.getVerifyAnswers().add("二"); // 添加额外正确答案
        task6.getVerifyAnswers().add("两"); // 添加额外正确答案
        task6.setRejectMessage("回答错误，正确答案是2，请重新申请加群并正确回答问题。");
        task6.setCaseSensitive(false); // 答案不区分大小写
        task6.setIgnoreWhitespace(true); // 忽略空格
        task6.setFuzzyMatch(false); // 不使用模糊匹配
        task6.setVerifyMode(ScheduledTask.VerifyMode.ANSWER_ONLY); // 仅使用答案验证
        tasks.add(task6);
        
        // 示例7：进群验证 - 高级验证（等级+答案）
        ScheduledTask task7 = new ScheduledTask();
        task7.setName("高级进群验证");
        task7.setType(TaskType.GROUP_REQUEST_VERIFY);
        task7.setTargetType("GROUP");
        task7.setTargetId(987654321); // 示例群号，用户需要修改
        task7.setVerifyQuestion("请回答社区规则中禁止发布的内容类型（提示：违法...）");
        task7.getVerifyAnswers().add("违法"); // 添加多个可能的正确答案
        task7.getVerifyAnswers().add("色情");
        task7.getVerifyAnswers().add("广告");
        task7.setRejectMessage("回答错误，请阅读群公告了解社区规则后再申请。");
        task7.setCaseSensitive(false); // 答案不区分大小写
        task7.setIgnoreWhitespace(true); // 忽略空格
        task7.setFuzzyMatch(true); // 使用模糊匹配
        task7.setMinLevel(10); // 最低等级要求
        task7.setMaxAutoAcceptLevel(50); // 达到50级自动通过
        task7.setVerifyMode(ScheduledTask.VerifyMode.ANY_ONE_PASS); // 任一验证通过即可
        tasks.add(task7);
        
        return tasks;
    }
    
    @Data
    public static class Config {
        private BotConfig bot;
        private List<ScheduledTask> scheduledTasks;
    }
    
    @Data
    public static class BotConfig {
        private String websocket;
        private String accessToken;
        private LogConfig log = new LogConfig();
        private SafetyConfig safety = new SafetyConfig();
    }
    
    @Data
    public static class LogConfig {
        private boolean enableMessageLog = false;    // 是否记录收到的消息
        private boolean enableDebugLog = false;      // 是否启用调试日志
        private boolean includeInfoInNormal = true;  // 是否在普通日志中包含INFO和MIXIN日志
        private int maxMessageLogs = 1000;          // 最大消息日志数量
        private int maxDays = 30;                   // 日志保留天数
    }
    
    @Data
    public static class SafetyConfig {
        private boolean enableMsgLimit = true;
        private int msgIntervalMs = 1500;
        private int groupMsgLimit = 20;
        private int privateMsgLimit = 10;
        private int taskMinIntervalMs = 5000;
        private boolean enableAutoRiskControl = true;
    }
} 