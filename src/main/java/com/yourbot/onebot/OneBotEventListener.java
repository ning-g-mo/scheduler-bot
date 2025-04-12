package com.yourbot.onebot;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * OneBot事件监听器管理
 */
public class OneBotEventListener {
    private static final Logger logger = LoggerFactory.getLogger(OneBotEventListener.class);
    
    // 使用线程安全的列表存储监听器
    private static final List<BiConsumer<String, JsonNode>> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * 注册事件监听器
     * @param listener 监听器，接收事件名和事件数据
     */
    public static void registerListener(BiConsumer<String, JsonNode> listener) {
        listeners.add(listener);
        logger.debug("注册OneBot事件监听器: {}", listener.getClass().getName());
    }
    
    /**
     * 移除事件监听器
     * @param listener 要移除的监听器
     */
    public static void removeListener(BiConsumer<String, JsonNode> listener) {
        listeners.remove(listener);
        logger.debug("移除OneBot事件监听器: {}", listener.getClass().getName());
    }
    
    /**
     * 清除所有监听器
     */
    public static void clearListeners() {
        listeners.clear();
        logger.debug("清除所有OneBot事件监听器");
    }
    
    /**
     * 触发事件
     * @param event 事件名
     * @param data 事件数据
     */
    public static void fireEvent(String event, JsonNode data) {
        for (BiConsumer<String, JsonNode> listener : listeners) {
            try {
                listener.accept(event, data);
            } catch (Exception e) {
                logger.error("触发事件监听器时出错: {}", e.getMessage(), e);
            }
        }
    }
} 