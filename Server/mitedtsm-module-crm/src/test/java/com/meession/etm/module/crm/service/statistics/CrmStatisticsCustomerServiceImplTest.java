package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.customer.CrmStatisticsCustomerDealCycleByDateRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.customer.CrmStatisticsCustomerReqVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsCustomerMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmStatisticsCustomerServiceImplTest {

    @Test
    void getCustomerDealCycleByDateAveragesCustomersAcrossRequestedInterval() {
        List<CrmStatisticsCustomerDealCycleByDateRespVO> mapperResult = List.of(
                dealCycle("2024-01-01T09:00:00", 5D),
                dealCycle("2024-01-02T09:00:00", 20D),
                dealCycle("2024-01-02T10:00:00", 30D)
        );
        CrmStatisticsCustomerServiceImpl service = new CrmStatisticsCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmStatisticsCustomerMapper.class,
                (proxy, method, args) -> mapperResult));
        CrmStatisticsCustomerReqVO reqVO = new CrmStatisticsCustomerReqVO()
                .setDeptId(10L)
                .setUserId(1L)
                .setInterval(3)
                .setTimes(new LocalDateTime[]{
                        LocalDateTime.of(2024, 1, 1, 0, 0),
                        LocalDateTime.of(2024, 1, 31, 23, 59, 59)
                });

        List<CrmStatisticsCustomerDealCycleByDateRespVO> result = service.getCustomerDealCycleByDate(reqVO);

        assertEquals(1, result.size());
        assertEquals(18.3D, result.get(0).getCustomerDealCycle());
    }

    private static CrmStatisticsCustomerDealCycleByDateRespVO dealCycle(String time, double cycle) {
        return new CrmStatisticsCustomerDealCycleByDateRespVO()
                .setTime(time)
                .setCustomerDealCycle(cycle);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
