package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.customer.CrmStatisticsCustomerDealCycleByDateRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.customer.CrmStatisticsCustomerReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.customer.CrmStatisticsFollowUpSummaryByDateRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.customer.CrmStatisticsFollowUpSummaryByUserRespVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsCustomerMapper;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmStatisticsCustomerServiceImplTest {

    @Test
    void getFollowUpSummaryByDateKeepsRecordAndCustomerCountsInTheirFields() {
        CrmStatisticsCustomerServiceImpl service = serviceWithMapper((proxy, method, args) -> switch (method.getName()) {
            case "selectFollowUpRecordCountGroupByDate" -> List.of(new CrmStatisticsFollowUpSummaryByDateRespVO()
                    .setTime("2024-01-01").setFollowUpRecordCount(7));
            case "selectFollowUpCustomerCountGroupByDate" -> List.of(new CrmStatisticsFollowUpSummaryByDateRespVO()
                    .setTime("2024-01-01").setFollowUpCustomerCount(3));
            default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
        });

        List<CrmStatisticsFollowUpSummaryByDateRespVO> result = service.getFollowUpSummaryByDate(request());

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getFollowUpRecordCount());
        assertEquals(3, result.get(0).getFollowUpCustomerCount());
    }

    @Test
    void getFollowUpSummaryByUserKeepsRecordAndCustomerCountsInTheirFields() {
        CrmStatisticsCustomerServiceImpl service = serviceWithMapper((proxy, method, args) -> switch (method.getName()) {
            case "selectFollowUpRecordCountGroupByUser" -> List.of(new CrmStatisticsFollowUpSummaryByUserRespVO()
                    .setFollowUpRecordCount(7).setOwnerUserId(1L));
            case "selectFollowUpCustomerCountGroupByUser" -> List.of(new CrmStatisticsFollowUpSummaryByUserRespVO()
                    .setFollowUpCustomerCount(3).setOwnerUserId(1L));
            default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
        });
        AdminUserRespDTO user = new AdminUserRespDTO().setId(1L).setNickname("测试用户");
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> Map.of(1L, user)));

        List<CrmStatisticsFollowUpSummaryByUserRespVO> result = service.getFollowUpSummaryByUser(request());

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getFollowUpRecordCount());
        assertEquals(3, result.get(0).getFollowUpCustomerCount());
        assertEquals("测试用户", result.get(0).getOwnerUserName());
    }

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
        CrmStatisticsCustomerReqVO reqVO = request().setInterval(3).setTimes(new LocalDateTime[]{
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

    private static CrmStatisticsCustomerReqVO request() {
        return new CrmStatisticsCustomerReqVO()
                .setDeptId(10L)
                .setUserId(1L)
                .setInterval(1)
                .setTimes(new LocalDateTime[]{
                        LocalDateTime.of(2024, 1, 1, 0, 0),
                        LocalDateTime.of(2024, 1, 1, 23, 59, 59)
                });
    }

    private static CrmStatisticsCustomerServiceImpl serviceWithMapper(java.lang.reflect.InvocationHandler handler) {
        CrmStatisticsCustomerServiceImpl service = new CrmStatisticsCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmStatisticsCustomerMapper.class, handler));
        return service;
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
