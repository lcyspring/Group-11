package com.meession.etm.framework.excel.progress;

import lombok.Data;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 导入进度跟踪器
 * 支持通过 taskId 查询导入进度
 */
public class ImportProgressTracker {

    private static final ConcurrentHashMap<String, ImportTask> TASKS = new ConcurrentHashMap<>();

    @Data
    public static class ImportTask {
        private final String taskId;
        private final int totalCount;
        private final AtomicInteger processedCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failCount = new AtomicInteger(0);
        private volatile String status = "PROCESSING"; // PROCESSING, COMPLETED, FAILED
        private volatile String errorMessage;

        public ImportTask(String taskId, int totalCount) {
            this.taskId = taskId;
            this.totalCount = totalCount;
        }

        public double getProgress() {
            return totalCount > 0 ? (double) processedCount.get() / totalCount * 100 : 0;
        }

        public void incrementProcessed() {
            processedCount.incrementAndGet();
        }

        public void incrementSuccess() {
            successCount.incrementAndGet();
        }

        public void incrementFail() {
            failCount.incrementAndGet();
        }
    }

    /** 创建导入任务 */
    public static ImportTask createTask(String taskId, int totalCount) {
        ImportTask task = new ImportTask(taskId, totalCount);
        TASKS.put(taskId, task);
        return task;
    }

    /** 获取导入任务 */
    public static ImportTask getTask(String taskId) {
        return TASKS.get(taskId);
    }

    /** 移除导入任务 */
    public static void removeTask(String taskId) {
        TASKS.remove(taskId);
    }

    /** 获取所有任务 */
    public static ConcurrentHashMap<String, ImportTask> getAllTasks() {
        return TASKS;
    }
}
