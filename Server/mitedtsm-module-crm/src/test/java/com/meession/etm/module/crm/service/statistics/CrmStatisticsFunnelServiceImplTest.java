package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticFunnelSummaryRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsFunnelReqVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsFunnelMapper;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmStatisticsFunnelServiceImplTest {

    @Test
    void getFunnelSummaryReturnsZeroSummaryWhenDepartmentHasNoUsers() {
        CrmStatisticsFunnelServiceImpl funnelService = new CrmStatisticsFunnelServiceImpl();
        ReflectionTestUtils.setField(funnelService, "deptApi", proxy(DeptApi.class,
                (proxy, method, args) -> Collections.emptyList()));
        ReflectionTestUtils.setField(funnelService, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> Collections.emptyList()));
        ReflectionTestUtils.setField(funnelService, "funnelMapper", proxy(CrmStatisticsFunnelMapper.class,
                (proxy, method, args) -> {
                    throw new AssertionError("空用户范围不应访问统计 Mapper");
                }));
        CrmStatisticsFunnelReqVO reqVO = new CrmStatisticsFunnelReqVO()
                .setDeptId(10L)
                .setInterval(2)
                .setTimes(new LocalDateTime[]{LocalDateTime.now().minusDays(7), LocalDateTime.now()});

        CrmStatisticFunnelSummaryRespVO result = funnelService.getFunnelSummary(reqVO);

        assertAll(
                () -> assertEquals(0L, result.getCustomerCount()),
                () -> assertEquals(0L, result.getBusinessCount()),
                () -> assertEquals(0L, result.getBusinessWinCount())
        );
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
