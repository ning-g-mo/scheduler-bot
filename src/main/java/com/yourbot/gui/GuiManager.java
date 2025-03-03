package com.yourbot.gui;

import com.yourbot.config.ConfigManager;
import com.yourbot.scheduler.ScheduledTask;
import com.yourbot.scheduler.SchedulerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GuiManager {
    private static final Logger logger = LoggerFactory.getLogger(GuiManager.class);
    private static GuiManager instance;
    
    private JFrame mainFrame;
    private JTextPane logPane;
    private JTextField commandField;
    private JTextPane taskListPane;
    private JTextPane systemInfoPane;
    
    private DefaultStyledDocument logDocument;
    private StyledDocument taskDocument;
    private StyledDocument systemInfoDocument;
    
    private Map<String, AttributeSet> styles;
    private Timer updateTimer;
    private Consumer<String> commandHandler;
    
    // 标记GUI是否已初始化
    private boolean guiInitialized = false;
    
    // 用于缓存初始化前的日志
    private ConcurrentLinkedQueue<LogEntry> pendingLogs = new ConcurrentLinkedQueue<>();
    
    private GuiManager() {
        styles = new HashMap<>();
        initStyles();
    }
    
    public static GuiManager getInstance() {
        if (instance == null) {
            instance = new GuiManager();
        }
        return instance;
    }
    
    // 初始化样式
    private void initStyles() {
        StyleContext styleContext = StyleContext.getDefaultStyleContext();
        
        // 正常文本样式
        styles.put("normal", styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.WHITE));
        
        // 不同日志级别的样式
        styles.put("INFO", styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(85, 255, 85)));
        styles.put("DEBUG", styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(85, 85, 255)));
        styles.put("WARN", styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(255, 255, 85)));
        styles.put("ERROR", styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(255, 85, 85)));
        styles.put("SUCCESS", styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(85, 255, 85)));
        
        // 任务信息样式
        styles.put("task_header", styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(255, 170, 0)));
        AttributeSet taskInfoStyle = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(170, 170, 170));
        taskInfoStyle = styleContext.addAttribute(taskInfoStyle, StyleConstants.FontSize, 12);
        styles.put("task_info", taskInfoStyle);
        
        // 系统信息样式
        styles.put("system_header", styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(85, 255, 255)));
        styles.put("system_value", styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(255, 255, 85)));
    }
    
    // 初始化GUI界面
    public void initGui(Consumer<String> commandHandler) {
        this.commandHandler = commandHandler;
        
        try {
            // 在EDT线程中执行GUI初始化
            SwingUtilities.invokeAndWait(() -> {
                try {
                    // 设置系统外观
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    
                    // 只为终端和特定组件设置暗色主题，其他使用系统默认
                    // 移除全局UI覆盖，只保留终端相关设置
                    
                    // 创建主窗口
                    mainFrame = new JFrame("定时任务机器人");
                    mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    mainFrame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            int result = JOptionPane.showConfirmDialog(
                                mainFrame,
                                "确定要退出程序吗？",
                                "确认退出",
                                JOptionPane.YES_NO_OPTION
                            );
                            if (result == JOptionPane.YES_OPTION) {
                                if (updateTimer != null) {
                                    updateTimer.cancel();
                                }
                                System.exit(0);
                            }
                        }
                    });
                    
                    // 设置应用图标
                    try {
                        ImageIcon icon = new ImageIcon(GuiManager.class.getResource("/icon.png"));
                        if (icon.getIconWidth() > 0) {
                            mainFrame.setIconImage(icon.getImage());
                        }
                    } catch (Exception e) {
                        logger.warn("加载应用图标失败", e);
                    }
                    
                    mainFrame.setSize(1024, 600);
                    mainFrame.setMinimumSize(new Dimension(800, 500));
                    
                    // 创建主面板
                    JPanel mainPanel = new JPanel();
                    mainPanel.setLayout(new BorderLayout());
                    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                    
                    // 创建左右分割面板
                    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
                    splitPane.setResizeWeight(0.7); // 左侧占70%
                    splitPane.setDividerLocation(0.7);
                    splitPane.setContinuousLayout(true);
                    
                    // 创建日志面板
                    logDocument = new DefaultStyledDocument();
                    logPane = new JTextPane(logDocument);
                    logPane.setEditable(false);
                    logPane.setBackground(new Color(30, 30, 30)); // 只为日志区域设置深色背景
                    logPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    logPane.setForeground(Color.WHITE);
                    
                    JScrollPane logScrollPane = new JScrollPane(logPane);
                    logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                    
                    // 创建右侧面板
                    JPanel rightPanel = new JPanel();
                    rightPanel.setLayout(new BorderLayout());
                    
                    // 创建系统信息面板
                    systemInfoDocument = new DefaultStyledDocument();
                    systemInfoPane = new JTextPane(systemInfoDocument);
                    systemInfoPane.setEditable(false);
                    systemInfoPane.setBackground(new Color(30, 30, 30)); // 只为系统信息区域设置深色背景
                    systemInfoPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    
                    JScrollPane systemInfoScrollPane = new JScrollPane(systemInfoPane);
                    systemInfoScrollPane.setPreferredSize(new Dimension(300, 200));
                    
                    // 创建任务列表面板
                    taskDocument = new DefaultStyledDocument();
                    taskListPane = new JTextPane(taskDocument);
                    taskListPane.setEditable(false);
                    taskListPane.setBackground(new Color(30, 30, 30)); // 只为任务列表区域设置深色背景
                    taskListPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    
                    JScrollPane taskListScrollPane = new JScrollPane(taskListPane);
                    
                    // 创建右侧上下分割面板
                    JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                    rightSplitPane.setTopComponent(systemInfoScrollPane);
                    rightSplitPane.setBottomComponent(taskListScrollPane);
                    rightSplitPane.setResizeWeight(0.3); // 系统信息占30%
                    rightSplitPane.setContinuousLayout(true);
                    
                    rightPanel.add(rightSplitPane, BorderLayout.CENTER);
                    
                    // 设置分割面板
                    splitPane.setLeftComponent(logScrollPane);
                    splitPane.setRightComponent(rightPanel);
                    
                    // 创建命令输入面板
                    JPanel commandPanel = new JPanel(new BorderLayout());
                    commandField = new JTextField();
                    commandField.setBackground(new Color(50, 50, 50));
                    commandField.setForeground(Color.WHITE);
                    commandField.setCaretColor(Color.WHITE);
                    commandField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
                    commandField.addActionListener((ActionEvent e) -> {
                        String command = commandField.getText().trim();
                        if (!command.isEmpty()) {
                            // 将命令添加到日志
                            appendLog("> " + command, "normal");
                            
                            // 执行命令
                            if (commandHandler != null) {
                                commandHandler.accept(command);
                            }
                            
                            // 清空命令输入框
                            commandField.setText("");
                        }
                    });
                    
                    commandPanel.add(commandField, BorderLayout.CENTER);
                    
                    // 添加组件到主面板
                    mainPanel.add(splitPane, BorderLayout.CENTER);
                    mainPanel.add(commandPanel, BorderLayout.SOUTH);
                    
                    // 设置主面板
                    mainFrame.setContentPane(mainPanel);
                    
                    // 显示窗口
                    mainFrame.setLocationRelativeTo(null);
                    mainFrame.setVisible(true);
                    
                    // 设置焦点到命令输入框
                    commandField.requestFocusInWindow();
                    
                    // 创建更新定时器
                    updateTimer = new Timer(true);
                    updateTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            SwingUtilities.invokeLater(() -> {
                                updateSystemInfo();
                                updateTaskList();
                            });
                        }
                    }, 0, 2000); // 每2秒更新一次
                    
                    // 标记GUI初始化完成
                    guiInitialized = true;
                    
                    // 显示缓存的日志
                    processPendingLogs();
                } catch (Exception e) {
                    logger.error("GUI初始化失败", e);
                }
            });
        } catch (Exception e) {
            logger.error("启动GUI失败", e);
        }
    }
    
    // 处理初始化前缓存的日志
    private void processPendingLogs() {
        LogEntry entry;
        while ((entry = pendingLogs.poll()) != null) {
            try {
                AttributeSet style = styles.getOrDefault(entry.level, styles.get("normal"));
                appendToDocument(logDocument, entry.message + "\n", style);
            } catch (Exception e) {
                logger.error("处理缓存日志失败", e);
            }
        }
    }
    
    // 更新系统信息
    private void updateSystemInfo() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            int memoryPercent = (int)((usedMemory * 100) / maxMemory);
            
            // 格式化为MB
            usedMemory = usedMemory / (1024 * 1024);
            maxMemory = maxMemory / (1024 * 1024);
            
            double cpuLoad = -1;
            try {
                java.lang.reflect.Method method = osBean.getClass().getDeclaredMethod("getProcessCpuLoad");
                method.setAccessible(true);
                cpuLoad = (double) method.invoke(osBean) * 100;
            } catch (Exception e) {
                // 如果不支持这个方法，就忽略
            }
            
            systemInfoDocument.remove(0, systemInfoDocument.getLength());
            
            appendToDocument(systemInfoDocument, "=== 系统信息 ===\n", styles.get("system_header"));
            appendToDocument(systemInfoDocument, "内存使用: ", styles.get("normal"));
            appendToDocument(systemInfoDocument, usedMemory + "MB / " + maxMemory + "MB (" + memoryPercent + "%)\n", styles.get("system_value"));
            
            if (cpuLoad >= 0) {
                appendToDocument(systemInfoDocument, "CPU使用: ", styles.get("normal"));
                appendToDocument(systemInfoDocument, String.format("%.1f%%\n", cpuLoad), styles.get("system_value"));
            }
            
            appendToDocument(systemInfoDocument, "操作系统: ", styles.get("normal"));
            appendToDocument(systemInfoDocument, System.getProperty("os.name") + "\n", styles.get("system_value"));
            
            appendToDocument(systemInfoDocument, "Java版本: ", styles.get("normal"));
            appendToDocument(systemInfoDocument, System.getProperty("java.version") + "\n", styles.get("system_value"));
            
            appendToDocument(systemInfoDocument, "当前时间: ", styles.get("normal"));
            appendToDocument(systemInfoDocument, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n", styles.get("system_value"));
            
            // 添加WebSocket连接信息
            appendToDocument(systemInfoDocument, "WebSocket: ", styles.get("normal"));
            if (com.yourbot.onebot.OneBotClient.getInstance().isConnected()) {
                appendToDocument(systemInfoDocument, "已连接\n", styles.get("INFO"));
            } else {
                appendToDocument(systemInfoDocument, "未连接\n", styles.get("ERROR"));
            }
        } catch (Exception e) {
            logger.error("更新系统信息失败", e);
        }
    }
    
    // 更新任务列表
    private void updateTaskList() {
        try {
            List<ScheduledTask> tasks = ConfigManager.getInstance().getScheduledTasks();
            
            taskDocument.remove(0, taskDocument.getLength());
            
            appendToDocument(taskDocument, "=== 任务列表 (" + tasks.size() + ") ===\n", styles.get("task_header"));
            
            for (ScheduledTask task : tasks) {
                appendToDocument(taskDocument, task.getName() + "\n", styles.get("INFO"));
                appendToDocument(taskDocument, "  类型: " + task.getType() + "\n", styles.get("task_info"));
                appendToDocument(taskDocument, "  目标: " + task.getTargetType() + " ", styles.get("task_info"));
                
                // 显示目标ID列表
                if (!task.getTargetIds().isEmpty()) {
                    appendToDocument(taskDocument, task.getTargetIds().toString() + "\n", styles.get("task_info"));
                } else {
                    appendToDocument(taskDocument, task.getTargetId() + "\n", styles.get("task_info"));
                }
                
                appendToDocument(taskDocument, "  时间: " + task.getCronExpression() + "\n", styles.get("task_info"));
                
                // 获取任务下一次执行时间
                try {
                    org.quartz.Trigger trigger = SchedulerManager.getInstance().getTrigger(task.getName());
                    if (trigger != null) {
                        java.util.Date nextFireTime = trigger.getNextFireTime();
                        if (nextFireTime != null) {
                            appendToDocument(taskDocument, "  下次执行: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(nextFireTime) + "\n", styles.get("task_info"));
                        }
                    }
                } catch (Exception e) {
                    logger.error("获取任务下一次执行时间失败", e);
                }
                
                appendToDocument(taskDocument, "\n", styles.get("normal"));
            }
        } catch (Exception e) {
            logger.error("更新任务列表失败", e);
        }
    }
    
    // 向文档添加带样式的文本
    private void appendToDocument(StyledDocument doc, String text, AttributeSet style) {
        if (doc == null) return;
        
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            logger.error("添加文本到文档失败", e);
        }
    }
    
    // 添加日志到日志面板
    public void appendLog(String message, String level) {
        // 如果GUI未初始化完成，添加到等待队列
        if (!guiInitialized) {
            pendingLogs.offer(new LogEntry(message, level));
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                if (logDocument != null) {
                    AttributeSet style = styles.getOrDefault(level, styles.get("normal"));
                    appendToDocument(logDocument, message + "\n", style);
                    
                    // 自动滚动到底部
                    logPane.setCaretPosition(logDocument.getLength());
                }
            } catch (Exception e) {
                logger.error("添加日志到GUI失败", e);
            }
        });
    }
    
    // 清空日志
    public void clearLog() {
        if (!guiInitialized) return;
        
        SwingUtilities.invokeLater(() -> {
            try {
                if (logDocument != null) {
                    logDocument.remove(0, logDocument.getLength());
                }
            } catch (BadLocationException e) {
                logger.error("清空日志失败", e);
            }
        });
    }
    
    // 日志条目类 - 用于暂存初始化前的日志
    private static class LogEntry {
        final String message;
        final String level;
        
        LogEntry(String message, String level) {
            this.message = message;
            this.level = level;
        }
    }
} 