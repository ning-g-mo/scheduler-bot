package com.yourbot.onebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourbot.config.ConfigManager;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;

public class OneBotClient {
    private static final Logger logger = LoggerFactory.getLogger(OneBotClient.class);
    
    private static OneBotClient instance;
    private WebSocketClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private boolean connected = false;
    
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
                System.err.println("机器人配置为空，无法连接到OneBot服务器");
                return;
            }
            
            String wsUrl = botConfig.getWebsocket();
            String token = botConfig.getAccessToken();
            
            if (wsUrl == null || wsUrl.isEmpty()) {
                logger.error("WebSocket URL为空，无法连接到OneBot服务器");
                System.err.println("WebSocket URL为空，无法连接到OneBot服务器");
                return;
            }
            
            logger.info("正在连接到OneBot服务器: {}", wsUrl);
            
            client = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    connected = true;
                    logger.info("已连接到OneBot服务器: {}", wsUrl);
                    System.out.println("已连接到OneBot服务器");
                }
                
                @Override
                public void onMessage(String message) {
                    try {
                        logger.debug("收到消息: {}", message);
                        JsonNode json = mapper.readTree(message);
                        // 处理接收到的消息
                    } catch (Exception e) {
                        logger.error("处理消息失败", e);
                        System.err.println("处理消息失败: " + e.getMessage());
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    logger.warn("与OneBot服务器的连接已关闭: 代码={}, 原因={}, 远程关闭={}", code, reason, remote);
                    System.out.println("与OneBot服务器的连接已关闭: " + reason);
                    
                    // 尝试重新连接
                    logger.info("5秒后尝试重新连接...");
                    new Thread(() -> {
                        try {
                            Thread.sleep(5000);
                            connect();
                        } catch (InterruptedException e) {
                            logger.error("重连线程被中断", e);
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
                
                @Override
                public void onError(Exception ex) {
                    logger.error("WebSocket错误", ex);
                    System.err.println("WebSocket错误: " + ex.getMessage());
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
            System.err.println("连接OneBot服务器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isConnected() {
        return connected && client != null && client.isOpen();
    }
    
    public void sendGroupMessage(long groupId, String message) {
        try {
            if (!isConnected()) {
                logger.warn("未连接到OneBot服务器，无法发送群消息");
                System.err.println("未连接到OneBot服务器，无法发送群消息");
                return;
            }
            
            logger.info("发送群消息到 {}: {}", groupId, message);
            
            ObjectNode json = mapper.createObjectNode();
            json.put("action", "send_group_msg");
            
            ObjectNode params = mapper.createObjectNode();
            params.put("group_id", groupId);
            params.put("message", message);
            
            json.set("params", params);
            json.put("echo", UUID.randomUUID().toString());
            
            String jsonStr = json.toString();
            logger.debug("发送WebSocket消息: {}", jsonStr);
            client.send(jsonStr);
            
            System.out.println("已发送群消息到 " + groupId + ": " + message);
        } catch (Exception e) {
            logger.error("发送群消息失败", e);
            System.err.println("发送群消息失败: " + e.getMessage());
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
            params.put("message", message);
            
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
} 