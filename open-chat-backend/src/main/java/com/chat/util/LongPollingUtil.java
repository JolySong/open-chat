package com.chat.util;

import com.chat.model.vo.ApiResponse;
import com.chat.model.vo.PollResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 长轮询工具类
 * 用于管理和处理长轮询请求
 *
 * @author chat
 * @since 2024-01-01
 */
@Slf4j
public class LongPollingUtil {

    /** 存储每个房间的挂起请求 */
    private static final Map<String, CopyOnWriteArraySet<DeferredResult<Object>>> PENDING_REQUESTS = new ConcurrentHashMap<>();
    
    /** 用于执行超时任务的调度器 */
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    /** 存储每个房间的用户列表请求 */
    private static final Map<String, List<DeferredResult<ApiResponse<PollResult>>>> USER_LIST_REQUESTS = new ConcurrentHashMap<>();

    /**
     * 创建长轮询请求
     *
     * @param roomId 聊天室ID
     * @param timeout 超时时间（秒）
     * @return DeferredResult对象
     */
    public static DeferredResult<Object> createRequest(String roomId, Integer timeout) {
        DeferredResult<Object> deferredResult = new DeferredResult<>((long) timeout * 1000, createTimeoutResult());
        
        // 获取或创建房间的请求集合
        CopyOnWriteArraySet<DeferredResult<Object>> roomRequests = 
            PENDING_REQUESTS.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>());
        
        // 添加请求到集合
        roomRequests.add(deferredResult);
        
        // 请求完成时（正常完成或超时）移除请求
        deferredResult.onCompletion(() -> {
            roomRequests.remove(deferredResult);
            if (roomRequests.isEmpty()) {
                PENDING_REQUESTS.remove(roomId);
            }
        });
        
        return deferredResult;
    }

    /**
     * 通知指定房间的所有等待请求
     *
     * @param roomId 聊天室ID
     * @param result 要发送的结果
     */
    public static void notifyRoom(String roomId, Object result) {
        CopyOnWriteArraySet<DeferredResult<Object>> roomRequests = PENDING_REQUESTS.get(roomId);
        if (roomRequests != null && !roomRequests.isEmpty()) {
            for (DeferredResult<Object> request : roomRequests) {
                request.setResult(result);
            }
        }
    }

    /**
     * 获取房间的等待请求数量
     *
     * @param roomId 聊天室ID
     * @return 等待请求数量
     */
    public static int getPendingRequestCount(String roomId) {
        CopyOnWriteArraySet<DeferredResult<Object>> roomRequests = PENDING_REQUESTS.get(roomId);
        return roomRequests != null ? roomRequests.size() : 0;
    }

    /**
     * 创建超时结果
     *
     * @return 超时结果对象
     */
    private static Object createTimeoutResult() {
        return new TimeoutResult();
    }

    /**
     * 超时结果类
     */
    public static class TimeoutResult {
        private final String status = "timeout";
        private final long timestamp = System.currentTimeMillis();

        public String getStatus() {
            return status;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 添加用户列表轮询请求
     *
     * @param roomId 聊天室ID
     * @param request 用户列表轮询请求
     */
    public static void addUserListRequest(String roomId, DeferredResult<ApiResponse<PollResult>> request) {
        USER_LIST_REQUESTS.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(request);
        request.onCompletion(() -> {
            List<DeferredResult<ApiResponse<PollResult>>> requests = USER_LIST_REQUESTS.get(roomId);
            if (requests != null) {
                requests.remove(request);
            }
        });
    }

    /**
     * 通知用户列表更新
     *
     * @param roomId 聊天室ID
     * @param result 用户列表更新结果
     */
    public static void notifyUserListUpdate(String roomId, PollResult result) {
        List<DeferredResult<ApiResponse<PollResult>>> requests = USER_LIST_REQUESTS.get(roomId);
        if (requests != null && !requests.isEmpty()) {
            log.info("通知用户列表更新: roomId={}, users={}, requests={}", 
                roomId, result.getOnlineUsers(), requests.size());
            // 创建新的结果对象，避免共享引用
            PollResult newResult = new PollResult();
            newResult.setOnlineUsers(new HashSet<>(result.getOnlineUsers()));
            newResult.setMessages(Collections.emptyList());
            newResult.setLastMessageId(null);
            
            requests.forEach(request -> {
                if (!request.isSetOrExpired()) {
                    request.setResult(ApiResponse.success(newResult));
                }
            });
            requests.clear();
            USER_LIST_REQUESTS.remove(roomId);
        } else {
            log.info("没有等待的用户列表请求: roomId={}", roomId);
        }
    }
} 