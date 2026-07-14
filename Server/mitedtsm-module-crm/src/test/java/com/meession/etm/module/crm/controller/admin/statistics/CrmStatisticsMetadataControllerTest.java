package com.meession.etm.module.crm.controller.admin.statistics;

import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmStatisticsMetadataControllerTest {

    @Test
    void catalogKeepsScopeSpecificApiPermissionBoundary() throws Exception {
        Method method = CrmStatisticsMetadataController.class.getMethod("getCatalog", String.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        String expression = preAuthorize.value();

        assertTrue(expression.contains("#scope == 'customer'"));
        assertTrue(expression.contains("crm:statistics-customer:query"));
        assertTrue(expression.contains("#scope == 'funnel'"));
        assertTrue(expression.contains("crm:statistics-funnel:query"));
        assertTrue(expression.contains("#scope == 'performance'"));
        assertTrue(expression.contains("crm:statistics-performance:query"));
        assertTrue(expression.contains("#scope == 'portrait'"));
        assertTrue(expression.contains("crm:statistics-portrait:query"));
        assertTrue(expression.contains("#scope == 'rank'"));
        assertTrue(expression.contains("crm:statistics-rank:query"));
        assertTrue(expression.contains("#scope == 'workorder'"));
        assertTrue(expression.contains("crm:statistics-work-order:query"));
    }

    @Test
    void catalogOnlyAcceptsIntegratedScopes() throws Exception {
        Method method = CrmStatisticsMetadataController.class.getMethod("getCatalog", String.class);
        Pattern pattern = findPattern(method);

        assertEquals("customer|funnel|performance|portrait|rank|workorder", pattern.regexp());
        RequestMapping mapping = CrmStatisticsMetadataController.class.getAnnotation(RequestMapping.class);
        assertEquals("/crm/statistics-metadata", mapping.value()[0]);
    }

    private static Pattern findPattern(Method method) {
        for (java.lang.annotation.Annotation annotation : method.getParameterAnnotations()[0]) {
            if (annotation instanceof Pattern pattern) {
                return pattern;
            }
        }
        throw new AssertionError("scope 参数缺少 @Pattern 边界");
    }
}
