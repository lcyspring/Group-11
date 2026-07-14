package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.statistics.CrmPerformanceTargetDO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmPerformanceTargetMapper;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetScopeTypeEnum;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetTypeEnum;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_COUNT_DECIMAL;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_SCOPE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrmPerformanceTargetServiceImplTest {

    @Test
    void savePerformanceTargetUpdatesExistingMonthsAndCreatesMissingMonths() {
        CrmPerformanceTargetServiceImpl service = new CrmPerformanceTargetServiceImpl();
        AtomicInteger validatedUserCount = new AtomicInteger();
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class, (proxy, method, args) -> {
            if (method.getName().equals("validateUser")) {
                assertEquals(9L, args[0]);
                validatedUserCount.incrementAndGet();
                return null;
            }
            throw new AssertionError("不应调用其他用户 API: " + method.getName());
        }));
        AtomicInteger insertCount = new AtomicInteger();
        AtomicInteger updateCount = new AtomicInteger();
        ReflectionTestUtils.setField(service, "performanceTargetMapper", proxy(CrmPerformanceTargetMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectListByScopeAndYear" -> List.of(
                            target(101L, 1, "10"), target(102L, 2, "1"));
                    case "insert" -> {
                        CrmPerformanceTargetDO inserted = (CrmPerformanceTargetDO) args[0];
                        assertEquals(CrmPerformanceTargetScopeTypeEnum.USER.getType(), inserted.getScopeType());
                        assertEquals(9L, inserted.getScopeId());
                        assertEquals(2026, inserted.getTargetYear());
                        assertEquals(CrmPerformanceTargetTypeEnum.CUSTOMER_COUNT.getType(), inserted.getTargetType());
                        insertCount.incrementAndGet();
                        yield 1;
                    }
                    case "updateById" -> {
                        CrmPerformanceTargetDO updated = (CrmPerformanceTargetDO) args[0];
                        assertEquals(102L, updated.getId());
                        assertEquals(new BigDecimal("20"), updated.getTargetValue());
                        updateCount.incrementAndGet();
                        yield 1;
                    }
                    default -> throw new AssertionError("不应调用其他目标 Mapper 方法: " + method.getName());
                }));
        CrmPerformanceTargetSaveReqVO reqVO = request(CrmPerformanceTargetScopeTypeEnum.USER.getType(), 9L,
                CrmPerformanceTargetTypeEnum.CUSTOMER_COUNT.getType());
        reqVO.setMonthlyTargets(new ArrayList<>(List.of(new BigDecimal("10"), new BigDecimal("20"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO)));

        service.savePerformanceTarget(reqVO);

        assertEquals(1, validatedUserCount.get());
        assertEquals(10, insertCount.get());
        assertEquals(1, updateCount.get());
    }

    @Test
    void savePerformanceTargetRejectsDecimalCountTarget() {
        CrmPerformanceTargetServiceImpl service = new CrmPerformanceTargetServiceImpl();
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> null));
        CrmPerformanceTargetSaveReqVO reqVO = request(CrmPerformanceTargetScopeTypeEnum.USER.getType(), 9L,
                CrmPerformanceTargetTypeEnum.FOLLOW_UP_COUNT.getType());
        reqVO.getMonthlyTargets().set(0, new BigDecimal("1.50"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.savePerformanceTarget(reqVO));

        assertEquals(PERFORMANCE_TARGET_COUNT_DECIMAL.getCode(), exception.getCode());
    }

    @Test
    void savePerformanceTargetRejectsNonZeroCompanyScopeId() {
        CrmPerformanceTargetServiceImpl service = new CrmPerformanceTargetServiceImpl();
        CrmPerformanceTargetSaveReqVO reqVO = request(CrmPerformanceTargetScopeTypeEnum.COMPANY.getType(), 1L,
                CrmPerformanceTargetTypeEnum.CONTRACT_PRICE.getType());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.savePerformanceTarget(reqVO));

        assertEquals(PERFORMANCE_TARGET_SCOPE_INVALID.getCode(), exception.getCode());
    }

    private static CrmPerformanceTargetSaveReqVO request(Integer scopeType, Long scopeId, Integer targetType) {
        CrmPerformanceTargetSaveReqVO reqVO = new CrmPerformanceTargetSaveReqVO();
        reqVO.setScopeType(scopeType);
        reqVO.setScopeId(scopeId);
        reqVO.setTargetYear(2026);
        reqVO.setTargetType(targetType);
        reqVO.setMonthlyTargets(new ArrayList<>(List.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        return reqVO;
    }

    private static CrmPerformanceTargetDO target(Long id, Integer month, String value) {
        return new CrmPerformanceTargetDO().setId(id).setTargetMonth(month).setTargetValue(new BigDecimal(value));
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
