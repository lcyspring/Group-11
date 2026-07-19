package com.meession.etm.framework.tenant.core.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.framework.tenant.core.service.TenantFrameworkService;
import com.meession.etm.framework.tenant.core.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多租户 JobHandler AOP
 * 任务执行时，会按照租户逐个执行 Job 的逻辑
 *
 * 注意，需要保证 JobHandler 的幂等性。因为 Job 因为某个租户执行失败重试时，之前执行成功的租户也会再次执行。
 *
 * @author 密讯
 */
@Aspect
@RequiredArgsConstructor
@Slf4j
public class TenantJobAspect {

    private final TenantFrameworkService tenantFrameworkService;

    @Around("@annotation(tenantJob)")
    public String around(ProceedingJoinPoint joinPoint, TenantJob tenantJob) {
        // 获得租户列表
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (CollUtil.isEmpty(tenantIds)) {
            return null;
        }

        // 逐个租户，执行 Job
        Map<Long, String> results = new ConcurrentHashMap<>();
        // ForkJoinPool.commonPool 的工作线程不会可靠继承 Spring Boot LaunchedClassLoader。
        // 租户任务若从嵌套 JAR 读取资源（例如 area.csv），使用工作线程原有的上下文类加载器会
        // 错误报告资源不存在，因此在每个并行任务中显式传播并在结束后恢复。
        ClassLoader applicationClassLoader = Thread.currentThread().getContextClassLoader();
        tenantIds.parallelStream().forEach(tenantId -> {
            Thread worker = Thread.currentThread();
            ClassLoader previousClassLoader = worker.getContextClassLoader();
            worker.setContextClassLoader(applicationClassLoader);
            try {
                // TODO 芋艿：先通过 parallel 实现并行；1）多个租户，是一条执行日志；2）异常的情况
                TenantUtils.execute(tenantId, () -> {
                    try {
                        Object result = joinPoint.proceed();
                        results.put(tenantId, StrUtil.toStringOrEmpty(result));
                    } catch (Throwable e) {
                        log.error("[execute][租户({}) 执行 Job 发生异常", tenantId, e);
                        results.put(tenantId, ExceptionUtil.getRootCauseMessage(e));
                    }
                });
            } finally {
                worker.setContextClassLoader(previousClassLoader);
            }
        });
        return JsonUtils.toJsonString(results);
    }

}
