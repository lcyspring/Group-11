package com.meession.etm.module.crm.service.business;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.business.vo.business.CrmBusinessUpdateStatusReqVO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessStatusDO;
import com.meession.etm.module.crm.dal.mysql.business.CrmBusinessMapper;
import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import com.meession.etm.module.crm.service.quote.CrmBusinessQuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.BUSINESS_UPDATE_STATUS_CONCURRENT;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.BUSINESS_UPDATE_STATUS_BACKWARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrmBusinessServiceImplTest {

    @Test
    void updateBusinessStatusPersistsTrimmedLostReason() {
        CrmBusinessServiceImpl service = new CrmBusinessServiceImpl();
        CrmBusinessDO business = activeBusiness();
        AtomicReference<Object[]> updateArgs = new AtomicReference<>();
        ReflectionTestUtils.setField(service, "businessMapper", proxy(CrmBusinessMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByIdForUpdate")) {
                        return business;
                    }
                    if (method.getName().equals("updateStatusIfUnchanged")) {
                        updateArgs.set(args);
                        return 1;
                    }
                    throw new AssertionError("未预期的 Mapper 方法: " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "businessStatusService", statusService());
        allowLockedQuote(service);

        service.updateBusinessStatus(new CrmBusinessUpdateStatusReqVO().setId(business.getId())
                .setEndStatus(CrmBusinessEndStatusEnum.LOSE.getStatus()).setEndRemark("  客户本年度预算取消无法继续采购  "));

        assertEquals(business.getStatusId(), updateArgs.get()[3]);
        assertEquals(CrmBusinessEndStatusEnum.LOSE.getStatus(), updateArgs.get()[4]);
        assertEquals("客户本年度预算取消无法继续采购", updateArgs.get()[5]);
        assertNotNull(updateArgs.get()[6]);
    }

    @Test
    void updateBusinessStatusRejectsConcurrentChange() {
        CrmBusinessServiceImpl service = new CrmBusinessServiceImpl();
        CrmBusinessDO business = activeBusiness();
        ReflectionTestUtils.setField(service, "businessMapper", proxy(CrmBusinessMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByIdForUpdate")) {
                        return business;
                    }
                    if (method.getName().equals("updateStatusIfUnchanged")) {
                        return 0;
                    }
                    throw new AssertionError("未预期的 Mapper 方法: " + method.getName());
                }));
        allowLockedQuote(service);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateBusinessStatus(new CrmBusinessUpdateStatusReqVO().setId(business.getId())
                        .setEndStatus(CrmBusinessEndStatusEnum.WIN.getStatus())));

        assertEquals(BUSINESS_UPDATE_STATUS_CONCURRENT.getCode(), exception.getCode());
    }

    @Test
    void wonBusinessDoesNotPersistStaleEndReason() {
        CrmBusinessServiceImpl service = new CrmBusinessServiceImpl();
        CrmBusinessDO business = activeBusiness();
        AtomicReference<Object[]> updateArgs = new AtomicReference<>();
        ReflectionTestUtils.setField(service, "businessMapper", proxy(CrmBusinessMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByIdForUpdate")) {
                        return business;
                    }
                    if (method.getName().equals("updateStatusIfUnchanged")) {
                        updateArgs.set(args);
                        return 1;
                    }
                    throw new AssertionError("未预期的 Mapper 方法: " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "businessStatusService", statusService());
        allowLockedQuote(service);

        service.updateBusinessStatus(new CrmBusinessUpdateStatusReqVO().setId(business.getId())
                .setEndStatus(CrmBusinessEndStatusEnum.WIN.getStatus()).setEndRemark("不应保存"));

        assertEquals(business.getStatusId(), updateArgs.get()[3]);
        assertNull(updateArgs.get()[5]);
        assertNotNull(updateArgs.get()[6]);
    }

    @Test
    void updateBusinessStatusRejectsBackwardStage() {
        CrmBusinessServiceImpl service = new CrmBusinessServiceImpl();
        CrmBusinessDO business = activeBusiness();
        ReflectionTestUtils.setField(service, "businessMapper", proxy(CrmBusinessMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByIdForUpdate")) {
                        return business;
                    }
                    throw new AssertionError("未预期的 Mapper 方法: " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "businessStatusService", statusTransitionService(2, 1));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateBusinessStatus(new CrmBusinessUpdateStatusReqVO().setId(business.getId())
                        .setStatusId(40L).setStatusRemark("尝试回退到上一阶段")));

        assertEquals(BUSINESS_UPDATE_STATUS_BACKWARD.getCode(), exception.getCode());
    }

    @Test
    void updateBusinessStatusAcceptsForwardStage() {
        CrmBusinessServiceImpl service = new CrmBusinessServiceImpl();
        CrmBusinessDO business = activeBusiness();
        AtomicReference<Object[]> updateArgs = new AtomicReference<>();
        ReflectionTestUtils.setField(service, "businessMapper", proxy(CrmBusinessMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByIdForUpdate")) {
                        return business;
                    }
                    if (method.getName().equals("updateStatusIfUnchanged")) {
                        updateArgs.set(args);
                        return 1;
                    }
                    throw new AssertionError("未预期的 Mapper 方法: " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "businessStatusService", statusTransitionService(1, 2));

        service.updateBusinessStatus(new CrmBusinessUpdateStatusReqVO().setId(business.getId())
                .setStatusId(40L).setStatusRemark("  客户需求范围已经确认  "));

        assertEquals(40L, updateArgs.get()[3]);
        assertNull(updateArgs.get()[4]);
        assertNull(updateArgs.get()[6]);
    }

    private static CrmBusinessDO activeBusiness() {
        return new CrmBusinessDO().setId(10L).setName("重点商机").setStatusTypeId(20L).setStatusId(30L);
    }

    private static CrmBusinessStatusService statusService() {
        return proxy(CrmBusinessStatusService.class, (proxy, method, args) -> {
            if (method.getName().equals("getBusinessStatus")) {
                return new CrmBusinessStatusDO().setId(30L).setName("跟进中");
            }
            throw new AssertionError("未预期的状态服务方法: " + method.getName());
        });
    }

    private static void allowLockedQuote(CrmBusinessServiceImpl service) {
        ReflectionTestUtils.setField(service, "quoteService", proxy(CrmBusinessQuoteService.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("requireCurrentLocked")) return null;
                    if (method.getName().equals("terminateCurrent")) return null;
                    throw new AssertionError("未预期的报价服务方法: " + method.getName());
                }));
    }

    private static CrmBusinessStatusService statusTransitionService(int currentSort, int targetSort) {
        return proxy(CrmBusinessStatusService.class, (proxy, method, args) -> {
            if (method.getName().equals("validateBusinessStatus")) {
                Long statusId = (Long) args[1];
                return statusId.equals(30L)
                        ? new CrmBusinessStatusDO().setId(30L).setName("当前阶段").setSort(currentSort)
                        : new CrmBusinessStatusDO().setId(40L).setName("目标阶段").setSort(targetSort);
            }
            if (method.getName().equals("getBusinessStatus")) {
                return new CrmBusinessStatusDO().setId(30L).setName("当前阶段").setSort(currentSort);
            }
            throw new AssertionError("未预期的状态服务方法: " + method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
