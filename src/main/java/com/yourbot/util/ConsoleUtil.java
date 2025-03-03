package com.yourbot.util;

import com.yourbot.gui.GuiManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 控制台输出工具类，提供带颜色和时间戳的日志输出
 */
public class ConsoleUtil {
    // ANSI颜色代码
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    
    // 日期格式化器
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // 是否启用颜色输出
    private static boolean colorEnabled = true;
    
    // 是否启用GUI
    private static boolean guiEnabled = false;
    
    /**
     * 设置是否启用颜色输出
     */
    public static void setColorEnabled(boolean enabled) {
        colorEnabled = enabled;
    }
    
    /**
     * 设置是否启用GUI
     */
    public static void setGuiEnabled(boolean enabled) {
        guiEnabled = enabled;
    }
    
    /**
     * 获取当前时间戳
     */
    private static String timestamp() {
        return LocalDateTime.now().format(formatter);
    }
    
    /**
     * 输出信息日志
     */
    public static void info(String message) {
        if (colorEnabled) {
            System.out.println(timestamp() + " " + ANSI_GREEN + "[INFO] " + ANSI_RESET + message);
        } else {
            System.out.println(timestamp() + " [INFO] " + message);
        }
        
        if (guiEnabled) {
            GuiManager.getInstance().appendLog(timestamp() + " [INFO] " + message, "INFO");
        }
    }
    
    /**
     * 输出调试日志
     */
    public static void debug(String message) {
        if (colorEnabled) {
            System.out.println(timestamp() + " " + ANSI_BLUE + "[DEBUG] " + ANSI_RESET + message);
        } else {
            System.out.println(timestamp() + " [DEBUG] " + message);
        }
        
        if (guiEnabled) {
            GuiManager.getInstance().appendLog(timestamp() + " [DEBUG] " + message, "DEBUG");
        }
    }
    
    /**
     * 输出警告日志
     */
    public static void warn(String message) {
        if (colorEnabled) {
            System.out.println(timestamp() + " " + ANSI_YELLOW + "[WARN] " + ANSI_RESET + message);
        } else {
            System.out.println(timestamp() + " [WARN] " + message);
        }
        
        if (guiEnabled) {
            GuiManager.getInstance().appendLog(timestamp() + " [WARN] " + message, "WARN");
        }
    }
    
    /**
     * 输出错误日志
     */
    public static void error(String message) {
        if (colorEnabled) {
            System.err.println(timestamp() + " " + ANSI_RED + "[ERROR] " + ANSI_RESET + message);
        } else {
            System.err.println(timestamp() + " [ERROR] " + message);
        }
        
        if (guiEnabled) {
            GuiManager.getInstance().appendLog(timestamp() + " [ERROR] " + message, "ERROR");
        }
    }
    
    /**
     * 输出成功消息
     */
    public static void success(String message) {
        if (colorEnabled) {
            System.out.println(timestamp() + " " + ANSI_GREEN + "[SUCCESS] " + message + ANSI_RESET);
        } else {
            System.out.println(timestamp() + " [SUCCESS] " + message);
        }
        
        if (guiEnabled) {
            GuiManager.getInstance().appendLog(timestamp() + " [SUCCESS] " + message, "SUCCESS");
        }
    }
    
    /**
     * 输出任务执行消息
     */
    public static void task(String taskName, String message) {
        if (colorEnabled) {
            System.out.println(timestamp() + " " + ANSI_CYAN + "[TASK:" + taskName + "] " + ANSI_RESET + message);
        } else {
            System.out.println(timestamp() + " [TASK:" + taskName + "] " + message);
        }
        
        if (guiEnabled) {
            GuiManager.getInstance().appendLog(timestamp() + " [TASK:" + taskName + "] " + message, "normal");
        }
    }
} 