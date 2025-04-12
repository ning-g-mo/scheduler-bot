package com.yourbot;

import com.yourbot.config.ConfigManager;
import com.yourbot.gui.GuiManager;
import com.yourbot.scheduler.SchedulerManager;
import com.yourbot.util.ConsoleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.yourbot.log.TaskExecutionLog;
import com.yourbot.log.TaskLogManager;
import com.yourbot.onebot.OneBotClient;
import com.yourbot.onebot.GroupRequestProcessor;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static boolean guiMode = true;
    
    // 添加版本号常量
    public static final String VERSION = "1.2.2";
    
    public static void main(String[] args) {
        // 检查命令行参数
        if (Arrays.asList(args).contains("nogui")) {
            guiMode = false;
        }
        
        // 设置默认字符编码
        System.setProperty("file.encoding", "UTF-8");
        
        // 设置日志配置
        ConfigManager.BotConfig botConfig = ConfigManager.getInstance().getBotConfig();
        System.setProperty("bot.log.enableMessageLog", 
                String.valueOf(botConfig.getLog().isEnableMessageLog()));
        System.setProperty("bot.log.enableDebugLog", 
                String.valueOf(botConfig.getLog().isEnableDebugLog()));
        System.setProperty("bot.log.includeInfoInNormal", 
                String.valueOf(botConfig.getLog().isIncludeInfoInNormal()));
        System.setProperty("bot.log.maxDays", 
                String.valueOf(botConfig.getLog().getMaxDays()));
        
        logger.info("设置默认字符编码: UTF-8");
        
        // 设置是否启用GUI
        ConsoleUtil.setGuiEnabled(guiMode);
        
        // 创建日志目录
        File logDir = new File("logs");
        if (!logDir.exists()) {
            logDir.mkdir();
            logger.info("创建日志目录: {}", logDir.getAbsolutePath());
        }
        
        ConsoleUtil.info("正在启动定时任务机器人 v" + VERSION + "...");
        logger.info("正在启动定时任务机器人 v{}", VERSION);
        
        try {
            // 加载配置
            logger.debug("开始加载配置...");
            ConsoleUtil.debug("开始加载配置...");
            ConfigManager configManager = ConfigManager.getInstance();
            
            // 检查配置是否需要修改
            checkConfigNeedsModification();
            
            // 初始化定时任务
            logger.debug("开始初始化定时任务...");
            ConsoleUtil.debug("开始初始化定时任务...");
            SchedulerManager schedulerManager = SchedulerManager.getInstance();
            schedulerManager.loadTasks();
            
            // 连接OneBot服务器
            logger.debug("开始连接OneBot服务器...");
            ConsoleUtil.debug("开始连接OneBot服务器...");
            OneBotClient oneBotClient = OneBotClient.getInstance();
            oneBotClient.connect();
            
            // 初始化进群请求处理器
            logger.debug("初始化进群请求处理器...");
            ConsoleUtil.debug("初始化进群请求处理器...");
            GroupRequestProcessor requestProcessor = new GroupRequestProcessor();
            requestProcessor.init();
            
            // 初始化GUI界面
            if (guiMode) {
                GuiManager.getInstance().initGui(Main::handleCommand);
            }
            
            // 命令行交互
            logger.info("机器人已启动，输入 'reload' 重新加载配置，输入 'exit' 退出程序");
            ConsoleUtil.success("机器人已启动，输入 'reload' 重新加载配置，输入 'exit' 退出程序");
            
            // 控制台模式下使用Scanner获取输入
            if (!guiMode) {
                Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
                while (true) {
                    String command = scanner.nextLine().trim();
                    handleCommand(command);
                }
            }
        } catch (Exception e) {
            logger.error("程序启动过程中发生错误", e);
            ConsoleUtil.error("程序启动失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查配置是否需要修改，并提示用户
     */
    private static void checkConfigNeedsModification() {
        ConfigManager configManager = ConfigManager.getInstance();
        boolean needsModification = false;
        
        // 检查WebSocket地址是否是默认值
        if (configManager.getBotConfig() != null && 
            "ws://127.0.0.1:6700".equals(configManager.getBotConfig().getWebsocket())) {
            needsModification = true;
        }
        
        // 检查任务中的群号和QQ号是否是默认值
        if (configManager.getScheduledTasks() != null && !configManager.getScheduledTasks().isEmpty()) {
            for (int i = 0; i < configManager.getScheduledTasks().size(); i++) {
                if (configManager.getScheduledTasks().get(i).getTargetId() == 123456789 ||
                    configManager.getScheduledTasks().get(i).getTargetId() == 987654321 ||
                    configManager.getScheduledTasks().get(i).getMemberId() == 111222333) {
                    needsModification = true;
                    break;
                }
            }
        }
        
        if (needsModification) {
            ConsoleUtil.warn("检测到配置文件使用了默认值，请修改 config.yml 文件中的以下内容：");
            ConsoleUtil.warn("1. 修改 bot.websocket 为您的 OneBot 服务器地址");
            ConsoleUtil.warn("2. 修改任务中的群号和QQ号为实际值");
            ConsoleUtil.warn("3. 根据需要调整任务的执行时间和内容");
            ConsoleUtil.warn("修改完成后，请输入 'reload' 命令重新加载配置");
            
            logger.warn("配置文件使用了默认值，需要修改");
        }
    }
    
    /**
     * 显示帮助信息
     */
    private static void showHelpInfo() {
        ConsoleUtil.info("=== 定时任务机器人帮助 ===");
        ConsoleUtil.info("可用命令:");
        ConsoleUtil.info("  help   - 显示此帮助信息");
        ConsoleUtil.info("  reload - 重新加载配置文件");
        ConsoleUtil.info("  exit   - 退出程序");
        ConsoleUtil.info("  logs   - 显示任务执行日志");
        ConsoleUtil.info("    logs         - 显示最近10条日志");
        ConsoleUtil.info("    logs recent [数量] - 显示最近的日志，可指定数量");
        ConsoleUtil.info("    logs task [任务名] - 显示特定任务的日志");
        ConsoleUtil.info("    logs export [任务名] - 导出特定任务的日志到文件");
        ConsoleUtil.info("");
        ConsoleUtil.info("配置文件: config.yml");
        ConsoleUtil.info("  修改此文件可以配置机器人连接信息和定时任务");
        ConsoleUtil.info("  修改后需要输入 'reload' 命令使配置生效");
        ConsoleUtil.info("");
        ConsoleUtil.info("日志文件:");
        ConsoleUtil.info("  logs/scheduler-bot.log - 主日志文件");
        ConsoleUtil.info("  logs/debug.log         - 调试日志文件");
        ConsoleUtil.info("");
        ConsoleUtil.info("支持的任务类型:");
        ConsoleUtil.info("  SEND_MESSAGE    - 发送消息");
        ConsoleUtil.info("  GROUP_BAN_ALL   - 全体禁言/解禁");
        ConsoleUtil.info("  GROUP_BAN_MEMBER - 禁言/解禁特定成员");
        ConsoleUtil.info("");
        ConsoleUtil.info("Cron表达式示例:");
        ConsoleUtil.info("  0 0 8 * * ?     - 每天早上8点");
        ConsoleUtil.info("  0 30 7 * * ?    - 每天早上7点30分");
        ConsoleUtil.info("  0 0 12 ? * MON-FRI - 每周一至周五中午12点");
        ConsoleUtil.info("  0 0 20 ? * FRI  - 每周五晚上8点");
        ConsoleUtil.info("  0 0/30 * * * ?  - 每30分钟");
    }

    /**
     * 处理日志查询命令
     */
    private static void handleLogsCommand(String command) {
        String[] parts = command.split("\\s+", 3);
        
        if (parts.length == 1) {
            // 显示最近的日志
            showRecentLogs(10);
        } else if (parts.length >= 2) {
            String subCommand = parts[1];
            
            if ("recent".equalsIgnoreCase(subCommand)) {
                // 显示最近的日志，可以指定数量
                int limit = 10;
                if (parts.length > 2) {
                    try {
                        limit = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        ConsoleUtil.warn("无效的数量参数，使用默认值 10");
                    }
                }
                showRecentLogs(limit);
            } else if ("task".equalsIgnoreCase(subCommand)) {
                // 显示特定任务的日志
                if (parts.length > 2) {
                    String taskName = parts[2];
                    showTaskLogs(taskName);
                } else {
                    ConsoleUtil.warn("请指定任务名称，例如: logs task 早安问候");
                }
            } else if ("export".equalsIgnoreCase(subCommand)) {
                // 导出特定任务的日志
                if (parts.length > 2) {
                    String taskName = parts[2];
                    exportTaskLogs(taskName);
                } else {
                    ConsoleUtil.warn("请指定任务名称，例如: logs export 早安问候");
                }
            } else {
                ConsoleUtil.warn("未知的日志命令: " + subCommand);
                ConsoleUtil.info("可用的日志命令:");
                ConsoleUtil.info("  logs         - 显示最近10条日志");
                ConsoleUtil.info("  logs recent [数量] - 显示最近的日志，可指定数量");
                ConsoleUtil.info("  logs task [任务名] - 显示特定任务的日志");
                ConsoleUtil.info("  logs export [任务名] - 导出特定任务的日志到文件");
            }
        }
    }

    /**
     * 显示最近的日志
     */
    private static void showRecentLogs(int limit) {
        List<TaskExecutionLog> logs = TaskLogManager.getInstance().getRecentLogs(limit);
        
        if (logs.isEmpty()) {
            ConsoleUtil.info("没有找到任务执行日志");
            return;
        }
        
        ConsoleUtil.info("最近 " + logs.size() + " 条任务执行日志:");
        ConsoleUtil.info("=".repeat(50));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TaskExecutionLog log : logs) {
            String status = log.isSuccess() ? ConsoleUtil.ANSI_GREEN + "成功" + ConsoleUtil.ANSI_RESET : 
                                             ConsoleUtil.ANSI_RED + "失败" + ConsoleUtil.ANSI_RESET;
            
            ConsoleUtil.info(log.getExecutionTime().format(formatter) + " | " + 
                    log.getTaskName() + " | " + status);
            ConsoleUtil.info("  类型: " + log.getTaskType() + ", 目标: " + log.getTargetType() + " " + log.getTargetId());
            ConsoleUtil.info("  详情: " + log.getDetails());
            
            if (!log.isSuccess() && log.getErrorMessage() != null) {
                ConsoleUtil.info("  错误: " + ConsoleUtil.ANSI_RED + log.getErrorMessage() + ConsoleUtil.ANSI_RESET);
            }
            
            ConsoleUtil.info("-".repeat(50));
        }
    }

    /**
     * 显示特定任务的日志
     */
    private static void showTaskLogs(String taskName) {
        List<TaskExecutionLog> logs = TaskLogManager.getInstance().getTaskLogs(taskName);
        
        if (logs.isEmpty()) {
            ConsoleUtil.info("没有找到任务 '" + taskName + "' 的执行日志");
            return;
        }
        
        ConsoleUtil.info("任务 '" + taskName + "' 的执行日志 (共 " + logs.size() + " 条):");
        ConsoleUtil.info("=".repeat(50));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TaskExecutionLog log : logs) {
            String status = log.isSuccess() ? ConsoleUtil.ANSI_GREEN + "成功" + ConsoleUtil.ANSI_RESET : 
                                             ConsoleUtil.ANSI_RED + "失败" + ConsoleUtil.ANSI_RESET;
            
            ConsoleUtil.info(log.getExecutionTime().format(formatter) + " | " + status);
            ConsoleUtil.info("  详情: " + log.getDetails());
            
            if (!log.isSuccess() && log.getErrorMessage() != null) {
                ConsoleUtil.info("  错误: " + ConsoleUtil.ANSI_RED + log.getErrorMessage() + ConsoleUtil.ANSI_RESET);
            }
            
            ConsoleUtil.info("-".repeat(50));
        }
    }

    /**
     * 导出特定任务的日志
     */
    private static void exportTaskLogs(String taskName) {
        String filePath = TaskLogManager.getInstance().exportTaskLogs(taskName, 0);
        
        if (filePath == null) {
            ConsoleUtil.warn("没有找到任务 '" + taskName + "' 的执行日志，或导出失败");
            return;
        }
        
        ConsoleUtil.success("任务日志已导出到文件: " + filePath);
    }

    /**
     * 处理用户输入的命令
     */
    private static void handleCommand(String command) {
        if ("reload".equalsIgnoreCase(command)) {
            logger.info("用户请求重新加载配置");
            ConsoleUtil.info("正在重新加载配置...");
            ConfigManager.getInstance().loadConfig();
            SchedulerManager.getInstance().loadTasks();
            logger.info("配置重新加载完成");
            ConsoleUtil.success("配置重新加载完成");
        } else if ("exit".equalsIgnoreCase(command)) {
            logger.info("用户请求退出程序");
            ConsoleUtil.info("正在关闭程序...");
            System.exit(0);
        } else if ("help".equalsIgnoreCase(command)) {
            logger.info("用户请求帮助信息");
            showHelpInfo();
        } else if (command.startsWith("logs")) {
            logger.info("用户请求查看日志");
            handleLogsCommand(command);
        } else {
            logger.debug("用户输入了未知命令: {}", command);
            ConsoleUtil.warn("未知命令，输入 'help' 查看可用命令");
        }
    }
} 