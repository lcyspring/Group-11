package com.meession.etm.module.crm.framework.permission.core.aop;

import com.meession.etm.framework.web.core.util.WebFrameworkUtils;
import com.meession.etm.module.crm.controller.admin.statistics.vo.CrmStatisticsScopedReqVO;
import com.meession.etm.module.crm.framework.permission.CrmStatisticsDataScopeService;
import jakarta.annotation.Resource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CRM_STATISTICS_SCOPE_DENIED;

/** Applies CRM organization data scope before a statistics controller invokes its service. */
@Component
@Aspect
public class CrmStatisticsDataScopeAspect {

    @Resource
    private CrmStatisticsDataScopeService dataScopeService;

    @Before("@within(com.meession.etm.module.crm.framework.permission.core.annotations.CrmStatisticsDataScope) "
            + "|| @annotation(com.meession.etm.module.crm.framework.permission.core.annotations.CrmStatisticsDataScope)")
    public void validate(JoinPoint joinPoint) {
        CrmStatisticsScopedReqVO reqVO = Arrays.stream(joinPoint.getArgs())
                .filter(CrmStatisticsScopedReqVO.class::isInstance)
                .map(CrmStatisticsScopedReqVO.class::cast)
                .findFirst()
                .orElseThrow(() -> exception(CRM_STATISTICS_SCOPE_DENIED));
        dataScopeService.validate(WebFrameworkUtils.getLoginUserId(), reqVO);
    }

}
