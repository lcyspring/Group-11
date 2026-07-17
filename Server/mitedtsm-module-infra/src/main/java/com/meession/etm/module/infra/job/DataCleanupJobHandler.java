package com.meession.etm.module.infra.job;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 数据清理定时任务
 *
 * 定期清理过期的日志数据
 *
 * @author 密讯
 */
@Component
@Slf4j
public class DataCleanupJobHandler implements JobHandler {

    /**
     * 默认清理超过（30）天的数据
     */
    private static final Integer DEFAULT_RETAIN_DAY = 30;

    /**
     * 每次删除间隔的条数，如果值太高可能会造成数据库的压力过大
     */
    private static final Integer DELETE_LIMIT = 100;

    @Override
    @TenantIgnore
    public String execute(String param) {
        // 解析参数，默认保留 30 天
        Integer retainDays = DEFAULT_RETAIN_DAY;
        if (param != null && !param.isEmpty()) {
            try {
                retainDays = Integer.parseInt(param);
            } catch (NumberFormatException e) {
                log.warn("[execute] 参数解析失败，使用默认保留天数: {}", DEFAULT_RETAIN_DAY);
            }
        }
        // 清理过期的日志数据
        int cleanedCount = cleanupExpiredLogs(retainDays, DELETE_LIMIT);
        log.info("[execute][定时执行清理过期日志数据数量 ({}) 个]", cleanedCount);
        return String.format("定时执行清理过期日志数据数量 %s 个", cleanedCount);
    }

    /**
     * 清理过期的日志数据
     *
     * @param retainDay  保留天数
     * @param deleteLimit 每次删除条数
     * @return 已清理的数据数量
     */
    private int cleanupExpiredLogs(Integer retainDay, Integer deleteLimit) {
        // TODO 此处可对接实际的日志清理服务，例如清理过期的业务日志、操作日志等
        log.info("[cleanupExpiredLogs][开始清理过期日志数据，保留天数 ({}) 每次删除条数 ({})]", retainDay, deleteLimit);
        return 0;
    }

}
