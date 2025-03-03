package com.yourbot.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yourbot.util.ConsoleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务日志管理器
 */
public class TaskLogManager {
    private static final Logger logger = LoggerFactory.getLogger(TaskLogManager.class);
    private static TaskLogManager instance;
    
    private final String LOG_DIR = "logs/tasks";
    private final int MAX_LOGS_PER_TASK = 100; // 每个任务最多保留的日志数量
    private final ObjectMapper mapper;
    
    private TaskLogManager() {
        // 创建日志目录
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
            logger.info("创建任务日志目录: {}", logDir.getAbsolutePath());
        }
        
        // 配置ObjectMapper以支持Java 8日期时间
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }
    
    public static TaskLogManager getInstance() {
        if (instance == null) {
            instance = new TaskLogManager();
        }
        return instance;
    }
    
    /**
     * 记录任务执行日志
     */
    public void logTaskExecution(TaskExecutionLog log) {
        try {
            // 确保任务目录存在
            String taskDir = LOG_DIR + "/" + sanitizeFileName(log.getTaskName());
            File dir = new File(taskDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 生成日志文件名
            String fileName = log.getId() + ".json";
            String filePath = taskDir + "/" + fileName;
            
            // 写入日志文件
            mapper.writeValue(new File(filePath), log);
            
            // 清理旧日志
            cleanupOldLogs(taskDir);
            
            logger.debug("已记录任务执行日志: {}", filePath);
        } catch (IOException e) {
            logger.error("记录任务执行日志失败", e);
        }
    }
    
    /**
     * 获取指定任务的所有日志
     */
    public List<TaskExecutionLog> getTaskLogs(String taskName) {
        String taskDir = LOG_DIR + "/" + sanitizeFileName(taskName);
        File dir = new File(taskDir);
        if (!dir.exists()) {
            return Collections.emptyList();
        }
        
        List<TaskExecutionLog> logs = new ArrayList<>();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return Collections.emptyList();
        }
        
        for (File file : files) {
            try {
                TaskExecutionLog log = mapper.readValue(file, TaskExecutionLog.class);
                logs.add(log);
            } catch (IOException e) {
                logger.error("读取任务日志失败: {}", file.getName(), e);
            }
        }
        
        // 按执行时间降序排序
        logs.sort((a, b) -> b.getExecutionTime().compareTo(a.getExecutionTime()));
        return logs;
    }
    
    /**
     * 获取最近的任务日志
     */
    public List<TaskExecutionLog> getRecentLogs(int limit) {
        List<TaskExecutionLog> allLogs = new ArrayList<>();
        File rootDir = new File(LOG_DIR);
        File[] taskDirs = rootDir.listFiles(File::isDirectory);
        if (taskDirs == null) {
            return Collections.emptyList();
        }
        
        for (File taskDir : taskDirs) {
            File[] logFiles = taskDir.listFiles((d, name) -> name.endsWith(".json"));
            if (logFiles == null) continue;
            
            for (File logFile : logFiles) {
                try {
                    TaskExecutionLog log = mapper.readValue(logFile, TaskExecutionLog.class);
                    allLogs.add(log);
                } catch (IOException e) {
                    logger.error("读取任务日志失败: {}", logFile.getName(), e);
                }
            }
        }
        
        // 按执行时间降序排序并限制数量
        return allLogs.stream()
                .sorted((a, b) -> b.getExecutionTime().compareTo(a.getExecutionTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 清理旧日志文件
     */
    private void cleanupOldLogs(String taskDir) {
        File dir = new File(taskDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length <= MAX_LOGS_PER_TASK) {
            return;
        }
        
        // 按最后修改时间排序
        List<File> fileList = new ArrayList<>();
        Collections.addAll(fileList, files);
        fileList.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        // 删除多余的旧日志
        for (int i = MAX_LOGS_PER_TASK; i < fileList.size(); i++) {
            if (!fileList.get(i).delete()) {
                logger.warn("无法删除旧日志文件: {}", fileList.get(i).getAbsolutePath());
            }
        }
    }
    
    /**
     * 清理文件名，确保安全
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    
    /**
     * 导出任务日志到文本文件
     */
    public String exportTaskLogs(String taskName, int limit) {
        List<TaskExecutionLog> logs = getTaskLogs(taskName);
        if (logs.isEmpty()) {
            return null;
        }
        
        if (limit > 0 && logs.size() > limit) {
            logs = logs.subList(0, limit);
        }
        
        try {
            String exportDir = "exports";
            File dir = new File(exportDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = exportDir + "/" + sanitizeFileName(taskName) + "_" + timestamp + ".txt";
            
            StringBuilder content = new StringBuilder();
            content.append("任务执行日志: ").append(taskName).append("\n");
            content.append("导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            content.append("=".repeat(50)).append("\n\n");
            
            for (TaskExecutionLog log : logs) {
                content.append("ID: ").append(log.getId()).append("\n");
                content.append("执行时间: ").append(log.getExecutionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                content.append("任务类型: ").append(log.getTaskType()).append("\n");
                content.append("目标: ").append(log.getTargetType()).append(" ").append(log.getTargetId()).append("\n");
                content.append("结果: ").append(log.isSuccess() ? "成功" : "失败").append("\n");
                content.append("详情: ").append(log.getDetails()).append("\n");
                if (!log.isSuccess() && log.getErrorMessage() != null) {
                    content.append("错误: ").append(log.getErrorMessage()).append("\n");
                }
                content.append("-".repeat(50)).append("\n\n");
            }
            
            Files.write(Paths.get(fileName), content.toString().getBytes());
            return fileName;
        } catch (IOException e) {
            logger.error("导出任务日志失败", e);
            return null;
        }
    }
} 