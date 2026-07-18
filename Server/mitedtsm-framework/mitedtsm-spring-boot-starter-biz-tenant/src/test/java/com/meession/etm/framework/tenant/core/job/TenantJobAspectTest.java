package com.meession.etm.framework.tenant.core.job;

import com.meession.etm.framework.tenant.core.service.TenantFrameworkService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantJobAspectTest {

    @Test
    void propagatesApplicationContextClassLoaderToParallelTenantWorkers() throws Throwable {
        TenantFrameworkService tenantFrameworkService = mock(TenantFrameworkService.class);
        List<Long> tenantIds = LongStream.rangeClosed(1, 32).boxed().toList();
        when(tenantFrameworkService.getTenantIds()).thenReturn(tenantIds);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Set<ClassLoader> observed = ConcurrentHashMap.newKeySet();
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            observed.add(Thread.currentThread().getContextClassLoader());
            return "ok";
        });
        ClassLoader applicationClassLoader = new ClassLoader(getClass().getClassLoader()) { };
        Thread current = Thread.currentThread();
        ClassLoader previous = current.getContextClassLoader();
        current.setContextClassLoader(applicationClassLoader);
        try {
            new TenantJobAspect(tenantFrameworkService).around(joinPoint, mock(TenantJob.class));
        } finally {
            current.setContextClassLoader(previous);
        }

        assertEquals(Set.of(applicationClassLoader), observed);
    }
}
