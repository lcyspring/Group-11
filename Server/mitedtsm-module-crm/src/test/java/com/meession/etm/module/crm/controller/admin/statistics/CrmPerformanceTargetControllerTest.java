package com.meession.etm.module.crm.controller.admin.statistics;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetListReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetRespVO;
import com.meession.etm.module.crm.dal.dataobject.statistics.CrmPerformanceTargetDO;
import com.meession.etm.module.crm.service.statistics.CrmPerformanceTargetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmPerformanceTargetControllerTest {

    @Test
    void getPerformanceTargetListDerivesQuarterAndAnnualTotalsFromMonths() {
        CrmPerformanceTargetController controller = new CrmPerformanceTargetController();
        ReflectionTestUtils.setField(controller, "performanceTargetService",
                proxy(CrmPerformanceTargetService.class, (proxy, method, args) -> {
                    if (method.getName().equals("getPerformanceTargetList")) {
                        return List.of(target(1, "10"), target(2, "20"), target(3, "30"));
                    }
                    throw new AssertionError("不应调用其他目标服务方法: " + method.getName());
                }));
        CrmPerformanceTargetListReqVO reqVO = new CrmPerformanceTargetListReqVO()
                .setScopeType(2).setScopeId(100L).setTargetYear(2026);

        CommonResult<List<CrmPerformanceTargetRespVO>> result = controller.getPerformanceTargetList(reqVO);

        CrmPerformanceTargetRespVO target = result.getData().get(0);
        assertEquals(12, target.getMonthlyTargets().size());
        assertEquals(List.of(new BigDecimal("60"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                target.getQuarterlyTargets());
        assertEquals(new BigDecimal("60"), target.getAnnualTarget());
    }

    private static CrmPerformanceTargetDO target(Integer month, String value) {
        return new CrmPerformanceTargetDO()
                .setTargetType(1).setTargetMonth(month).setTargetValue(new BigDecimal(value));
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
