package com.meession.etm.module.crm.framework.permission;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.web.core.util.WebFrameworkUtils;
import com.meession.etm.module.crm.controller.admin.statistics.CrmStatisticsCustomerController;
import com.meession.etm.module.crm.controller.admin.statistics.CrmStatisticsFunnelController;
import com.meession.etm.module.crm.controller.admin.statistics.CrmStatisticsPerformanceController;
import com.meession.etm.module.crm.controller.admin.statistics.CrmStatisticsPortraitController;
import com.meession.etm.module.crm.controller.admin.statistics.CrmStatisticsRankController;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceReqVO;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmStatisticsDataScope;
import com.meession.etm.module.crm.framework.permission.core.aop.CrmStatisticsDataScopeAspect;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CRM_STATISTICS_SCOPE_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmStatisticsDataScopeAspectTest {

    @Mock
    private CrmStatisticsDataScopeService dataScopeService;
    @Mock
    private JoinPoint joinPoint;
    @InjectMocks
    private CrmStatisticsDataScopeAspect aspect;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void passesAuthenticatedCallerAndScopedRequestToValidator() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        WebFrameworkUtils.setLoginUserId(servletRequest, 7L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        CrmStatisticsPerformanceReqVO reqVO = new CrmStatisticsPerformanceReqVO().setDeptId(10L);
        when(joinPoint.getArgs()).thenReturn(new Object[]{reqVO});

        aspect.validate(joinPoint);

        verify(dataScopeService).validate(7L, reqVO);
    }

    @Test
    void missingScopedRequestFailsClosed() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"unexpected"});

        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.validate(joinPoint));

        assertEquals(CRM_STATISTICS_SCOPE_DENIED.getCode(), ex.getCode());
    }

    @Test
    void allGeneralCrmStatisticsControllersCarryScopeGuard() {
        List<Class<?>> controllers = List.of(CrmStatisticsCustomerController.class,
                CrmStatisticsFunnelController.class, CrmStatisticsPerformanceController.class,
                CrmStatisticsPortraitController.class, CrmStatisticsRankController.class);

        controllers.forEach(controller -> assertTrue(controller.isAnnotationPresent(CrmStatisticsDataScope.class),
                () -> controller.getSimpleName() + " must carry @CrmStatisticsDataScope"));
    }

}
