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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    
    // 添加异步请求响应映射
    private final Map<String, CompletableFuture<JsonNode>> responseFutures = new ConcurrentHashMap<>();
    
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
                        JsonNode json = mapper.readTree(message);
                        
                        // 处理心跳消息
                        if (isHeartbeatMessage(json)) {
                            return;
                        }
                        
                        // 处理API调用响应
                        if (json.has("echo") && json.has("status") && json.has("retcode")) {
                            String echo = json.get("echo").asText();
                            synchronized (responseFutures) {
                                CompletableFuture<JsonNode> future = responseFutures.get(echo);
                                if (future != null) {
                                    future.complete(json);
                                }
                            }
                            return;
                        }
                        
                        // 处理事件消息
                        if (json.has("post_type")) {
                            String postType = json.get("post_type").asText();
                            
                            // 记录收到的消息
                            if (ConfigManager.getInstance().getBotConfig().getLog().isEnableMessageLog()) {
                                logger.info("收到消息: {}", message);
                            }
                            
                            // 消息事件
                            if ("message".equals(postType)) {
                                String messageType = json.get("message_type").asText();
                                String rawMessage = json.has("raw_message") ? 
                                        json.get("raw_message").asText() : json.get("message").asText();
                                long senderId = json.get("sender").get("user_id").asLong();
                                
                                if ("group".equals(messageType)) {
                                    long groupId = json.get("group_id").asLong();
                                    logger.debug("收到群 {} 中用户 {} 的消息: {}", groupId, senderId, rawMessage);
                                    OneBotEventListener.fireEvent("message.group", json);
                                } else if ("private".equals(messageType)) {
                                    logger.debug("收到用户 {} 的私聊消息: {}", senderId, rawMessage);
                                    OneBotEventListener.fireEvent("message.private", json);
                                }
                            }
                            // 请求事件（如加群请求）
                            else if ("request".equals(postType)) {
                                String requestType = json.get("request_type").asText();
                                
                                if ("group".equals(requestType)) {
                                    String subType = json.get("sub_type").asText();
                                    if ("add".equals(subType)) {
                                        logger.debug("收到进群请求事件");
                                        OneBotEventListener.fireEvent("request.group.add", json);
                                    } else if ("invite".equals(subType)) {
                                        logger.debug("收到群邀请事件");
                                        OneBotEventListener.fireEvent("request.group.invite", json);
                                    }
                                }
                            }
                            // 通知事件
                            else if ("notice".equals(postType)) {
                                String noticeType = json.get("notice_type").asText();
                                OneBotEventListener.fireEvent("notice." + noticeType, json);
                            }
                            // 元事件
                            else if ("meta_event".equals(postType)) {
                                String metaType = json.get("meta_event_type").asText();
                                if ("heartbeat".equals(metaType)) {
                                    // 心跳事件，不做处理
                                } else {
                                    OneBotEventListener.fireEvent("meta." + metaType, json);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("处理WebSocket消息时发生错误: {}", e.getMessage(), e);
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
    
    /**
     * 发送群消息
     */
    public void sendGroupMessage(long groupId, String message) {
        try {
            if (!isConnected()) {
                logger.warn("未连接到OneBot服务器，无法发送群消息");
                ConsoleUtil.warn("未连接到OneBot服务器，无法发送群消息");
                return;
            }
            
            // 检查消息长度
            if (message.length() > MAX_MESSAGE_LENGTH) {
                logger.warn("消息长度超过限制 ({}), 将被截断", MAX_MESSAGE_LENGTH);
                message = message.substring(0, MAX_MESSAGE_LENGTH);
            }
            
            // 检查消息频率
            if (!checkMessageFrequency(groupId, true)) {
                logger.warn("消息发送过于频繁，已跳过本次发送");
                return;
            }
            
            logger.info("发送群消息到 {}: {}", groupId, message);
            ConsoleUtil.debug("原始消息内容: " + message);
            
            // 处理换行符和图片
            String processedMessage = processMessage(message);
            ConsoleUtil.debug("处理后的消息内容: " + processedMessage);
            
            ObjectNode json = mapper.createObjectNode();
            json.put("action", "send_group_msg");
            
            ObjectNode params = mapper.createObjectNode();
            params.put("group_id", groupId);
            params.put("message", processedMessage);
            params.put("auto_escape", false);  // 不转义CQ码
            
            json.set("params", params);
            String echo = UUID.randomUUID().toString();
            json.put("echo", echo);
            
            String jsonStr = json.toString();
            logger.debug("发送WebSocket消息: {}", jsonStr);
            ConsoleUtil.debug("发送WebSocket消息: " + jsonStr);
            
            client.send(jsonStr);
            lastMsgTime = System.currentTimeMillis();
            
            ConsoleUtil.success("消息已发送到群 " + groupId);
        } catch (Exception e) {
            logger.error("发送群消息失败", e);
            ConsoleUtil.error("发送群消息失败: " + e.getMessage());
            ConsoleUtil.error("异常堆栈: ");
            e.printStackTrace();
            
            // 添加更详细的错误诊断信息
            if (e instanceof java.net.ConnectException) {
                ConsoleUtil.error("连接失败，请检查:");
                ConsoleUtil.error("1. OneBot服务是否正在运行");
                ConsoleUtil.error("2. WebSocket地址是否正确");
                ConsoleUtil.error("3. 端口是否正确且未被占用");
            } else if (e instanceof java.net.UnknownHostException) {
                ConsoleUtil.error("无法解析服务器地址，请检查WebSocket地址是否正确");
            }
        }
    }
    
    /**
     * 处理消息中的特殊内容（换行符、图片和艾特）
     */
    private String processMessage(String message) {
        try {
            // 处理换行符
            message = message.replace("\\n", "\n");
            ConsoleUtil.debug("处理换行后: " + message);
            
            // 处理艾特全体成员
            if (message.contains("[艾特全体]")) {
                ConsoleUtil.debug("处理艾特全体成员标记");
                message = message.replace("[艾特全体]", "[CQ:at,qq=all]");
            }
            
            // 处理艾特指定成员
            if (message.contains("[艾特:")) {
                ConsoleUtil.debug("检测到艾特标记，开始处理...");
                
                String tempMessage = message;
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[艾特:(\\d+)\\]");
                java.util.regex.Matcher matcher = pattern.matcher(tempMessage);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String qq = matcher.group(1);
                    ConsoleUtil.debug("处理艾特成员: " + qq);
                    matcher.appendReplacement(sb, "[CQ:at,qq=" + qq + "]");
                }
                matcher.appendTail(sb);
                message = sb.toString();
                
                ConsoleUtil.debug("艾特处理完成: " + message);
            }
            
            // 处理图片链接
            if (message.contains("[图片:")) {
                ConsoleUtil.debug("检测到图片标记，开始处理...");
                
                // 处理网络图片
                String tempMessage = message;
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[图片:(https?://[^\\]]+)\\]");
                java.util.regex.Matcher matcher = pattern.matcher(tempMessage);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String url = matcher.group(1);
                    ConsoleUtil.debug("处理网络图片: " + url);
                    matcher.appendReplacement(sb, "[CQ:image,file=" + url + "]");
                }
                matcher.appendTail(sb);
                message = sb.toString();
                
                // 处理本地图片
                pattern = java.util.regex.Pattern.compile("\\[图片:file://([^\\]]+)\\]");
                matcher = pattern.matcher(message);
                sb = new StringBuffer();
                while (matcher.find()) {
                    String path = matcher.group(1);
                    ConsoleUtil.debug("处理本地图片: " + path);
                    matcher.appendReplacement(sb, "[CQ:image,file=file://" + path + "]");
                }
                matcher.appendTail(sb);
                message = sb.toString();
                
                ConsoleUtil.debug("图片处理完成: " + message);
            }
            
            return message;
        } catch (Exception e) {
            logger.error("处理消息内容失败", e);
            ConsoleUtil.error("处理消息内容失败: " + e.getMessage());
            return message; // 发生错误时返回原始消息
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
    
    /**
     * 处理进群请求
     * @param flag 请求标识
     * @param approve 是否同意
     * @param reason 拒绝理由（仅在拒绝时有效）
     * @return 操作是否成功
     */
    public boolean handleGroupRequest(String flag, boolean approve, String reason) {
        if (!isConnected()) {
            logger.error("处理进群请求失败: 未连接到OneBotAPI");
            return false;
        }
        
        try {
            ObjectNode requestJson = mapper.createObjectNode();
            requestJson.put("action", approve ? "set_group_add_request" : "set_group_add_request");
            
            ObjectNode params = mapper.createObjectNode();
            params.put("flag", flag);
            params.put("sub_type", "add");
            params.put("approve", approve);
            if (!approve && reason != null && !reason.trim().isEmpty()) {
                params.put("reason", reason);
            }
            
            requestJson.set("params", params);
            
            String requestStr = mapper.writeValueAsString(requestJson);
            logger.debug("发送处理进群请求: {}", requestStr);
            client.send(requestStr);
            
            return true;
        } catch (Exception e) {
            logger.error("处理进群请求失败", e);
            return false;
        }
    }
    
    /**
     * 获取进群请求中的验证回答
     * @param comment 验证信息
     * @return 提取的答案，如果没有找到则返回原始字符串
     */
    public String extractVerifyAnswer(String comment) {
        if (comment == null || comment.isEmpty()) {
            return "";
        }
        
        // 尝试提取问题和答案格式
        // 常见格式：问题：xxx 答案：yyy
        if (comment.contains("答案")) {
            String[] parts = comment.split("答案[：:]");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        
        // 简单格式：直接提取所有内容作为答案
        return comment.trim();
    }
    
    /**
     * 获取群成员信息，包括等级
     * @param groupId 群号
     * @param userId 用户QQ号
     * @return 用户等级，如果获取失败则返回0
     */
    public int getGroupMemberLevel(long groupId, long userId) {
        if (!isConnected()) {
            logger.error("Bot未连接到服务器，无法获取群成员等级");
            return 0;
        }
        
        try {
            ObjectNode json = mapper.createObjectNode();
            json.put("action", "get_group_member_info");
            json.put("params", mapper.createObjectNode()
                .put("group_id", groupId)
                .put("user_id", userId)
                .put("no_cache", true));
                
            String requestStr = mapper.writeValueAsString(json);
            logger.debug("发送获取群成员信息请求: {}", requestStr);
            
            // 这里使用同步方式获取群成员信息
            CompletableFuture<JsonNode> future = new CompletableFuture<>();
            String requestId = UUID.randomUUID().toString();
            json.put("echo", requestId);
            
            synchronized (responseFutures) {
                responseFutures.put(requestId, future);
            }
            
            client.send(requestStr);
            
            // 等待响应，最多等待5秒
            JsonNode response;
            try {
                response = future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error("获取群成员信息超时或失败", e);
                return 0;
            } finally {
                synchronized (responseFutures) {
                    responseFutures.remove(requestId);
                }
            }
            
            if (response != null && response.has("data")) {
                JsonNode data = response.get("data");
                if (data.has("level")) {
                    return data.get("level").asInt(0);
                }
            }
            
            logger.warn("无法获取用户等级信息，可能是API不支持");
            return 0;
        } catch (Exception e) {
            logger.error("获取群成员等级失败", e);
            return 0;
        }
    }
}
