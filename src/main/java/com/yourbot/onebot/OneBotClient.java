package com.yourbot.onebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourbot.config.ConfigManager;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourbot.util.ConsoleUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OneBotClient {
    private static final Logger logger = LoggerFactory.getLogger(OneBotClient.class);
    
    private static OneBotClient instance;
    private WebSocketClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private boolean connected = false;
    
    // 添加消息频率限制相关常量
    private static final int MSG_INTERVAL_MS = 1500; // 消息最小间隔(毫秒)
    private static final int GROUP_MSG_LIMIT = 20;   // 群消息每分钟限制
    private static final int PRIVATE_MSG_LIMIT = 10; // 私聊消息每分钟限制
    
    // 添加消息计数器和时间记录
    private final Map<Long, Integer> groupMsgCounter = new ConcurrentHashMap<>();
    private final Map<Long, Integer> privateMsgCounter = new ConcurrentHashMap<>();
    private long lastMsgTime = 0;
    
    // 添加消息长度限制
    private static final int MAX_MESSAGE_LENGTH = 4500;
    
    private OneBotClient() {
        connect();
    }
    
    public static OneBotClient getInstance() {
        if (instance == null) {
            logger.debug("创建OneBotClient实例");
            instance = new OneBotClient();
        }
        return instance;
    }
    
    public void connect() {
        try {
            ConfigManager.BotConfig botConfig = ConfigManager.getInstance().getBotConfig();
            if (botConfig == null) {
                logger.error("机器人配置为空，无法连接到OneBot服务器");
                ConsoleUtil.error("机器人配置为空，无法连接到OneBot服务器");
                return;
            }
            
            String wsUrl = botConfig.getWebsocket();
            String token = botConfig.getAccessToken();
            
            if (wsUrl == null || wsUrl.isEmpty()) {
                logger.error("WebSocket URL为空，无法连接到OneBot服务器");
                ConsoleUtil.error("WebSocket URL为空，无法连接到OneBot服务器");
                return;
            }
            
            if ("ws://127.0.0.1:6700".equals(wsUrl)) {
                logger.warn("正在使用默认WebSocket地址，如果连接失败，请修改config.yml中的websocket地址");
                ConsoleUtil.warn("正在使用默认WebSocket地址，如果连接失败，请修改config.yml中的websocket地址");
            }
            
            logger.info("正在连接到OneBot服务器: {}", wsUrl);
            ConsoleUtil.info("正在连接到OneBot服务器: " + wsUrl);
            
            client = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    connected = true;
                    logger.info("已连接到OneBot服务器: {}", wsUrl);
                    ConsoleUtil.success("已连接到OneBot服务器: " + wsUrl);
                }
                
                @Override
                public void onMessage(String message) {
                    try {
                        logger.debug("收到消息: {}", message);
                        
                        // 检查是否启用消息日志
                        if (ConfigManager.getInstance().getBotConfig().getLog().isEnableMessageLog()) {
                            ConsoleUtil.debug("收到OneBot消息: " + message.substring(0, Math.min(100, message.length())) + 
                                    (message.length() > 100 ? "..." : ""));
                        }
                        
                        JsonNode json = mapper.readTree(message);
                        
                        // 如果是心跳消息且未启用调试日志，则不记录
                        if (isHeartbeatMessage(json) && !ConfigManager.getInstance().getBotConfig().getLog().isEnableDebugLog()) {
                            return;
                        }
                        
                        // 处理消息...
                    } catch (Exception e) {
                        logger.error("处理消息失败", e);
                        ConsoleUtil.error("处理消息失败: " + e.getMessage());
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    logger.warn("WebSocket连接已断开: code={}, reason={}, remote={}", code, reason, remote);
                    ConsoleUtil.warn("WebSocket连接已断开，尝试重连...");
                    reconnect();
                }
                
                @Override
                public void onError(Exception ex) {
                    logger.error("WebSocket错误", ex);
                    ConsoleUtil.error("WebSocket错误: " + ex.getMessage());
                }
            };
            
            if (token != null && !token.isEmpty()) {
                logger.debug("添加Authorization头: Bearer {}", token.substring(0, Math.min(5, token.length())) + "...");
                client.addHeader("Authorization", "Bearer " + token);
            }
            
            logger.debug("开始连接WebSocket...");
            client.connect();
        } catch (Exception e) {
            logger.error("连接OneBot服务器失败", e);
            ConsoleUtil.error("连接OneBot服务器失败: " + e.getMessage());
            
            // 添加更详细的错误提示
            if (e instanceof java.net.ConnectException) {
                ConsoleUtil.error("无法连接到OneBot服务器，请检查：");
                ConsoleUtil.error("1. OneBot服务器是否已启动");
                ConsoleUtil.error("2. config.yml中的websocket地址是否正确");
                ConsoleUtil.error("3. 网络连接是否正常");
            } else if (e instanceof java.net.UnknownHostException) {
                ConsoleUtil.error("无法解析OneBot服务器地址，请检查config.yml中的websocket地址是否正确");
            }
            
            e.printStackTrace();
        }
    }
    
    public boolean isConnected() {
        return connected && client != null && client.isOpen();
    }
    
    public void sendGroupMessage(long groupId, String message) {
        try {
            // 检查消息长度
            if (message.length() > MAX_MESSAGE_LENGTH) {
                logger.warn("消息长度超过限制({})，已截断", MAX_MESSAGE_LENGTH);
                message = message.substring(0, MAX_MESSAGE_LENGTH);
            }
            
            // 检查消息频率
            if (!checkMessageFrequency(groupId, true)) {
                logger.warn("群 {} 消息发送过于频繁，已暂停发送", groupId);
                return;
            }
            
            // 检查是否连接
            if (!isConnected()) {
                logger.warn("未连接到OneBot服务器，无法发送群消息");
                return;
            }
            
            // 构建消息
            ObjectNode json = mapper.createObjectNode();
            json.put("action", "send_group_msg");
            
            ObjectNode params = mapper.createObjectNode();
            params.put("group_id", groupId);
            params.put("message", message);
            params.put("auto_escape", false);
            
            json.set("params", params);
            json.put("echo", UUID.randomUUID().toString());
            
            String jsonStr = json.toString();
            client.send(jsonStr);
            
            // 更新最后发送时间
            lastMsgTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            logger.error("发送群消息失败", e);
        }
    }
    
    public void sendPrivateMessage(long userId, String message) {
        try {
            if (!isConnected()) {
                logger.warn("未连接到OneBot服务器，无法发送私聊消息");
                System.err.println("未连接到OneBot服务器，无法发送私聊消息");
                return;
            }
            
            logger.info("发送私聊消息到 {}: {}", userId, message);
            
            ObjectNode json = mapper.createObjectNode();
            json.put("action", "send_private_msg");
            
            ObjectNode params = mapper.createObjectNode();
            params.put("user_id", userId);
            params.put("message", new String(message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            
            json.set("params", params);
            json.put("echo", UUID.randomUUID().toString());
            
            String jsonStr = json.toString();
            logger.debug("发送WebSocket消息: {}", jsonStr);
            client.send(jsonStr);
            
            System.out.println("已发送私聊消息到 " + userId + ": " + message);
        } catch (Exception e) {
            logger.error("发送私聊消息失败", e);
            System.err.println("发送私聊消息失败: " + e.getMessage());
        }
    }
    
    public void setGroupWholeBan(long groupId, boolean enable) {
        try {
            if (!isConnected()) {
                logger.warn("未连接到OneBot服务器，无法设置全体禁言");
                System.err.println("未连接到OneBot服务器，无法设置全体禁言");
                return;
            }
            
            logger.info("设置群 {} 全体禁言: {}", groupId, enable);
            
            ObjectNode json = mapper.createObjectNode();
            json.put("action", "set_group_whole_ban");
            
            ObjectNode params = mapper.createObjectNode();
            params.put("group_id", groupId);
            params.put("enable", enable);
            
            json.set("params", params);
            json.put("echo", UUID.randomUUID().toString());
            
            String jsonStr = json.toString();
            logger.debug("发送WebSocket消息: {}", jsonStr);
            client.send(jsonStr);
            
            System.out.println("已" + (enable ? "开启" : "关闭") + "群 " + groupId + " 的全体禁言");
        } catch (Exception e) {
            logger.error("设置全体禁言失败", e);
            System.err.println("设置全体禁言失败: " + e.getMessage());
        }
    }
    
    public void setGroupBan(long groupId, long userId, int duration) {
        try {
            if (!isConnected()) {
                logger.warn("未连接到OneBot服务器，无法设置成员禁言");
                System.err.println("未连接到OneBot服务器，无法设置成员禁言");
                return;
            }
            
            logger.info("设置群 {} 成员 {} 禁言 {} 秒", groupId, userId, duration);
            
            ObjectNode json = mapper.createObjectNode();
            json.put("action", "set_group_ban");
            
            ObjectNode params = mapper.createObjectNode();
            params.put("group_id", groupId);
            params.put("user_id", userId);
            params.put("duration", duration);
            
            json.set("params", params);
            json.put("echo", UUID.randomUUID().toString());
            
            String jsonStr = json.toString();
            logger.debug("发送WebSocket消息: {}", jsonStr);
            client.send(jsonStr);
            
            if (duration > 0) {
                System.out.println("已禁言群 " + groupId + " 中的成员 " + userId + " " + duration + " 秒");
            } else {
                System.out.println("已解除群 " + groupId + " 中成员 " + userId + " 的禁言");
            }
        } catch (Exception e) {
            logger.error("设置成员禁言失败", e);
            System.err.println("设置成员禁言失败: " + e.getMessage());
        }
    }
    
    /**
     * 判断是否为心跳消息
     */
    private boolean isHeartbeatMessage(JsonNode json) {
        return json.has("meta_event_type") && 
               "heartbeat".equals(json.get("meta_event_type").asText());
    }
    
    // 添加消息频率检查方法
    private boolean checkMessageFrequency(long targetId, boolean isGroup) {
        long currentTime = System.currentTimeMillis();
        
        // 检查全局消息间隔
        if (currentTime - lastMsgTime < MSG_INTERVAL_MS) {
            return false;
        }
        
        Map<Long, Integer> counter = isGroup ? groupMsgCounter : privateMsgCounter;
        int limit = isGroup ? GROUP_MSG_LIMIT : PRIVATE_MSG_LIMIT;
        
        // 获取当前计数
        int count = counter.getOrDefault(targetId, 0);
        
        // 每分钟重置计数
        if (currentTime - lastMsgTime > 60000) {
            counter.clear();
            count = 0;
        }
        
        // 检查是否超过限制
        if (count >= limit) {
            return false;
        }
        
        // 更新计数
        counter.put(targetId, count + 1);
        return true;
    }
    
    // 添加风控检测方法
    private void handleResponse(JsonNode response) {
        if (response.has("retcode")) {
            int retcode = response.get("retcode").asInt();
            if (retcode == 100) { // 风控码
                logger.error("消息发送失败：账号可能被风控");
                ConsoleUtil.error("警告：账号可能被风控，建议降低发送频率");
            }
        }
    }
    
    // 添加重连机制
    private void reconnect() {
        try {
            Thread.sleep(5000); // 等待5秒后重连
            connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 