package com.meession.etm.module.crm.service.clue;

import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.followup.CrmFollowUpRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CLUE_TRANSFORM_FAIL_ALREADY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmClueServiceImplTest {

    @Test
    void transformClueStopsWhenAtomicClaimFails() {
        CrmClueServiceImpl service = new CrmClueServiceImpl();
        AtomicBoolean customerCreated = new AtomicBoolean();
        ReflectionTestUtils.setField(service, "clueMapper", proxy(CrmClueMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById" -> clue();
                    case "updateTransformStatusByIdAndTransformStatus" -> 0;
                    default -> throw new AssertionError("抢占失败后不应继续调用 " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> {
                    customerCreated.set(true);
                    throw new AssertionError("抢占失败后不应创建客户");
                }));

        assertServiceException(() -> service.transformClue(10L, 1L), CLUE_TRANSFORM_FAIL_ALREADY);
        assertFalse(customerCreated.get());
    }

    @Test
    void transformClueClaimsBeforeCreatingAndLinksCustomer() {
        CrmClueServiceImpl service = new CrmClueServiceImpl();
        AtomicBoolean claimed = new AtomicBoolean();
        AtomicReference<CrmClueDO> linkedClue = new AtomicReference<>();
        ReflectionTestUtils.setField(service, "clueMapper", proxy(CrmClueMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById" -> clue();
                    case "updateTransformStatusByIdAndTransformStatus" -> {
                        claimed.set(true);
                        yield 1;
                    }
                    case "updateById" -> {
                        linkedClue.set((CrmClueDO) args[0]);
                        yield 1;
                    }
                    default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> {
                    assertTrue(claimed.get(), "必须先原子抢占再创建客户");
                    return 99L;
                }));
        ReflectionTestUtils.setField(service, "followUpRecordService", proxy(CrmFollowUpRecordService.class,
                (proxy, method, args) -> Collections.emptyList()));

        service.transformClue(10L, 1L);

        assertTrue(claimed.get());
        assertEquals(10L, linkedClue.get().getId());
        assertEquals(99L, linkedClue.get().getCustomerId());
    }

    private static CrmClueDO clue() {
        return new CrmClueDO().setId(10L).setName("待转换线索").setTransformStatus(false);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
