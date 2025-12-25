package com.loadtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadtest.model.WorkerTask;
import com.loadtest.model.WorkerResult;
import com.loadtest.model.WorkerHeartbeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisQueueService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${loadtest.redis.queue.tasks}")
    private String tasksQueue;
    
    @Value("${loadtest.redis.queue.results}")
    private String resultsQueue;
    
    @Value("${loadtest.redis.queue.heartbeat}")
    private String heartbeatKey;
    
    // ========== Task Queue Operations ==========
    
    public void publishTask(WorkerTask task) {
        try {
            String json = objectMapper.writeValueAsString(task);
            redisTemplate.opsForList().rightPush(tasksQueue, json);
            log.info("Published task {} to queue", task.getTaskId());
        } catch (Exception e) {
            log.error("Failed to publish task", e);
            throw new RuntimeException("Failed to publish task", e);
        }
    }
    
    public void publishTasks(List<WorkerTask> tasks) {
        tasks.forEach(this::publishTask);
    }
    
    public Long getTaskQueueSize() {
        return redisTemplate.opsForList().size(tasksQueue);
    }
    
    // ========== Result Queue Operations ==========
    
    public WorkerResult pollResult(Duration timeout) {
        try {
            String json = redisTemplate.opsForList()
                .leftPop(resultsQueue, timeout);
            
            if (json == null) {
                return null;
            }
            
            return objectMapper.readValue(json, WorkerResult.class);
        } catch (Exception e) {
            log.error("Failed to poll result", e);
            return null;
        }
    }
    
    public List<WorkerResult> pollResults(int count) {
        return redisTemplate.opsForList()
            .leftPop(resultsQueue, count)
            .stream()
            .map(json -> {
                try {
                    return objectMapper.readValue(json, WorkerResult.class);
                } catch (Exception e) {
                    log.error("Failed to parse result", e);
                    return null;
                }
            })
            .filter(result -> result != null)
            .collect(Collectors.toList());
    }
    
    public Long getResultQueueSize() {
        return redisTemplate.opsForList().size(resultsQueue);
    }
    
    // ========== Worker Heartbeat Operations ==========
    
    public void updateWorkerHeartbeat(WorkerHeartbeat heartbeat) {
        try {
            String json = objectMapper.writeValueAsString(heartbeat);
            String key = heartbeatKey + ":" + heartbeat.getWorkerId();
            
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(60));
            log.debug("Updated heartbeat for worker {}", heartbeat.getWorkerId());
        } catch (Exception e) {
            log.error("Failed to update heartbeat", e);
        }
    }
    
    public WorkerHeartbeat getWorkerHeartbeat(String workerId) {
        try {
            String key = heartbeatKey + ":" + workerId;
            String json = redisTemplate.opsForValue().get(key);
            
            if (json == null) {
                return null;
            }
            
            return objectMapper.readValue(json, WorkerHeartbeat.class);
        } catch (Exception e) {
            log.error("Failed to get heartbeat", e);
            return null;
        }
    }
    
    public List<String> getActiveWorkerIds() {
        Set<String> keys = redisTemplate.keys(heartbeatKey + ":*");
        
        if (keys == null) {
            return List.of();
        }
        
        return keys.stream()
            .map(key -> key.replace(heartbeatKey + ":", ""))
            .collect(Collectors.toList());
    }
    
    public int getActiveWorkerCount() {
        return getActiveWorkerIds().size();
    }
    
    // ========== Cleanup Operations ==========
    
    public void clearTaskQueue() {
        redisTemplate.delete(tasksQueue);
        log.info("Cleared task queue");
    }
    
    public void clearResultQueue() {
        redisTemplate.delete(resultsQueue);
        log.info("Cleared result queue");
    }
    
    public void clearWorkerHeartbeats() {
        Set<String> keys = redisTemplate.keys(heartbeatKey + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared {} worker heartbeats", keys.size());
        }
    }
}