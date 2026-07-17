package com.meession.etm.module.infra.job;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消息通知定时任务
 *
 * 定期检查待发送的通知并处理
 *
 * @author 密讯
 */
@Component
@Slf4j
public class NotificationJobHandler implements JobHandler {

    /**
     * 默认每批次处理的通知数量
     */
    private static final Integer DEFAULT_BATCH_SIZE = 100;

    @Override
    @TenantIgnore
    public String execute(String param) {
        // 解析参数，默认每批次处理 100 条
        Integer batchSize = DEFAULT_BATCH_SIZE;
        if (param != null && !param.isEmpty()) {
            try {
                batchSize = Integer.parseInt(param);
            } catch (NumberFormatException e) {
                log.warn("[execute] 参数解析失败，使用默认批次大小: {}", DEFAULT_BATCH_SIZE);
            }
        }
        // 检查待发送的通知并处理
        int processedCount = processPendingNotifications(batchSize);
        log.info("[execute][定时执行处理待发送通知数量 ({}) 个]", processedCount);
        return String.format("定时执行处理待发送通知数量 %s 个", processedCount);
    }

    /**
     * 处理待发送的通知
     *
     * @param batchSize 每批次处理数量
     * @return 已处理的通知数量
     */
    private int processPendingNotifications(Integer batchSize) {
        // TODO 此处可对接实际的通知服务，例如查询待发送的通知列表并逐条发送
        log.info("[processPendingNotifications][开始处理待发送通知，批次大小 ({})]", batchSize);
        return 0;
    }

}
