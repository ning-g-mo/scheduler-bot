package com.yourbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.yourbot.scheduler.ScheduledTask;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourbot.util.ConsoleUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
                logger.error("配置文件不存在: {}", configPath);
                ConsoleUtil.error("配置文件不存在: " + configPath);
                return;
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
    
    @Data
    public static class Config {
        private BotConfig bot;
        private List<ScheduledTask> scheduledTasks;
    }
    
    @Data
    public static class BotConfig {
        private String websocket;
        private String accessToken;
    }
} 