package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticCustomerAreaRespVO;
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
    void areaSummariesAggregateRawDistrictsAtRequestedLevel() {
        CrmStatisticsPortraitServiceImpl service = new CrmStatisticsPortraitServiceImpl();
        List<CrmStatisticCustomerAreaRespVO> raw = List.of(
                area(110101, 2, 1),
                area(110102, 3, 2),
                area(310104, 4, 1),
                area(440106, 6, 3));
        ReflectionTestUtils.setField(service, "portraitMapper", proxy(CrmStatisticsPortraitMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectSummaryListGroupByAreaId")) {
                        CrmStatisticsPortraitReqVO reqVO = (CrmStatisticsPortraitReqVO) args[0];
                        assertEquals(List.of(99L), reqVO.getUserIds());
                        return raw;
                    }
                    throw new AssertionError("未预期的画像 Mapper 方法: " + method.getName());
                }));

        List<CrmStatisticCustomerAreaRespVO> cities = service.getCustomerSummaryByCity(request());
        assertEquals(List.of(440100, 110100, 310100),
                cities.stream().map(CrmStatisticCustomerAreaRespVO::getAreaId).toList());
        assertEquals(List.of(6, 5, 4),
                cities.stream().map(CrmStatisticCustomerAreaRespVO::getCustomerCount).toList());
        assertEquals(List.of(3, 3, 1),
                cities.stream().map(CrmStatisticCustomerAreaRespVO::getDealCount).toList());

        List<CrmStatisticCustomerAreaRespVO> provinces = service.getCustomerSummaryByArea(request());
        assertEquals(List.of(440000, 110000, 310000),
                provinces.stream().map(CrmStatisticCustomerAreaRespVO::getAreaId).toList());
        assertEquals(List.of(6, 5, 4),
                provinces.stream().map(CrmStatisticCustomerAreaRespVO::getCustomerCount).toList());

        List<CrmStatisticCustomerAreaRespVO> countries = service.getCustomerSummaryByCountry(request());
        assertEquals(1, countries.size());
        assertEquals(1, countries.get(0).getAreaId());
        assertEquals(15, countries.get(0).getCustomerCount());
        assertEquals(7, countries.get(0).getDealCount());
    }

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
        assertEquals(expected, service.getCustomerSummaryByDealStatus(request()));
    }

    private static CrmStatisticCustomerAreaRespVO area(Integer areaId, Integer customerCount, Integer dealCount) {
        return new CrmStatisticCustomerAreaRespVO().setAreaId(areaId)
                .setCustomerCount(customerCount).setDealCount(dealCount);
    }

    private static CrmStatisticsPortraitReqVO request() {
        return new CrmStatisticsPortraitReqVO().setDeptId(1L).setUserId(99L)
                .setTimes(new LocalDateTime[]{LocalDateTime.now().minusDays(7), LocalDateTime.now()});
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
