package com.yourbot;

import com.yourbot.config.ConfigManager;
import com.yourbot.scheduler.SchedulerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        // 设置默认字符编码
        System.setProperty("file.encoding", "UTF-8");
        logger.info("设置默认字符编码: UTF-8");
        
        // 创建日志目录
        File logDir = new File("logs");
        if (!logDir.exists()) {
            logDir.mkdir();
            logger.info("创建日志目录: {}", logDir.getAbsolutePath());
        }
        
        logger.info("正在启动机器人...");
        
        try {
            // 加载配置
            logger.debug("开始加载配置...");
            ConfigManager configManager = ConfigManager.getInstance();
            
            // 初始化定时任务
            logger.debug("开始初始化定时任务...");
            SchedulerManager schedulerManager = SchedulerManager.getInstance();
            schedulerManager.loadTasks();
            
            // 命令行交互
            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
            logger.info("机器人已启动，输入 'reload' 重新加载配置，输入 'exit' 退出程序");
            System.out.println("机器人已启动，输入 'reload' 重新加载配置，输入 'exit' 退出程序");
            
            while (true) {
                String command = scanner.nextLine().trim();
                
                if ("reload".equalsIgnoreCase(command)) {
                    logger.info("用户请求重新加载配置");
                    System.out.println("正在重新加载配置...");
                    configManager.loadConfig();
                    schedulerManager.loadTasks();
                    logger.info("配置重新加载完成");
                    System.out.println("配置重新加载完成");
                } else if ("exit".equalsIgnoreCase(command)) {
                    logger.info("用户请求退出程序");
                    System.out.println("正在关闭程序...");
                    System.exit(0);
                } else {
                    logger.debug("用户输入了未知命令: {}", command);
                    System.out.println("未知命令，可用命令: reload, exit");
                }
            }
        } catch (Exception e) {
            logger.error("程序启动过程中发生错误", e);
            System.err.println("程序启动失败: " + e.getMessage());
        }
    }
} 