package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticCustomerDealStatusRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticsPortraitReqVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsPortraitMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmStatisticsPortraitServiceImplTest {

    @Test
    void getCustomerSummaryByDealStatusUsesSelectedOwnerScope() {
        CrmStatisticsPortraitServiceImpl service = new CrmStatisticsPortraitServiceImpl();
        List<CrmStatisticCustomerDealStatusRespVO> expected = List.of(
                new CrmStatisticCustomerDealStatusRespVO().setDealStatus(false).setCustomerCount(7L),
                new CrmStatisticCustomerDealStatusRespVO().setDealStatus(true).setCustomerCount(3L)
        );
        ReflectionTestUtils.setField(service, "portraitMapper", proxy(CrmStatisticsPortraitMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectCustomerDealStatusList")) {
                        CrmStatisticsPortraitReqVO reqVO = (CrmStatisticsPortraitReqVO) args[0];
                        assertEquals(List.of(99L), reqVO.getUserIds());
                        return expected;
                    }
                    throw new AssertionError("未预期的画像 Mapper 方法: " + method.getName());
                }));
        CrmStatisticsPortraitReqVO reqVO = new CrmStatisticsPortraitReqVO().setDeptId(1L).setUserId(99L)
                .setTimes(new LocalDateTime[]{LocalDateTime.now().minusDays(7), LocalDateTime.now()});

        assertEquals(expected, service.getCustomerSummaryByDealStatus(reqVO));
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
