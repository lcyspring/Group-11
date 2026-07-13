package com.meession.etm.module.crm.controller.admin.customer;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerDuplicateCheckReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerDuplicateRespVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerOwnerRecordRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerOwnerRecordDO;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmCustomerControllerTest {

    @Test
    void duplicateCheckReturnsOnlyCandidateSummaryFields() {
        CrmCustomerController controller = new CrmCustomerController();
        ReflectionTestUtils.setField(controller, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> List.of(new CrmCustomerDO().setId(20L).setName("候选客户")
                        .setMobile("18000000000").setRemark("不应暴露"))));

        CommonResult<List<CrmCustomerDuplicateRespVO>> result = controller.getDuplicateCustomerList(
                new CrmCustomerDuplicateCheckReqVO().setName("候选客户"));

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals(20L, result.getData().get(0).getId());
        assertEquals("候选客户", result.getData().get(0).getName());
        assertEquals("18000000000", result.getData().get(0).getMobile());
    }

    @Test
    void ownerRecordListResolvesOwnerAndOperatorNames() {
        CrmCustomerController controller = new CrmCustomerController();
        CrmCustomerOwnerRecordDO record = new CrmCustomerOwnerRecordDO().setId(30L).setCustomerId(20L)
                .setType(4).setPreviousOwnerUserId(100L).setNewOwnerUserId(200L);
        record.setCreator("300");
        ReflectionTestUtils.setField(controller, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> List.of(record)));
        ReflectionTestUtils.setField(controller, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> Map.of(
                        100L, new AdminUserRespDTO().setId(100L).setNickname("原负责人"),
                        200L, new AdminUserRespDTO().setId(200L).setNickname("新负责人"),
                        300L, new AdminUserRespDTO().setId(300L).setNickname("操作人"))));

        CommonResult<List<CrmCustomerOwnerRecordRespVO>> result = controller.getCustomerOwnerRecordList(20L);

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        CrmCustomerOwnerRecordRespVO response = result.getData().get(0);
        assertEquals(100L, response.getPreviousOwnerUserId());
        assertEquals("原负责人", response.getPreviousOwnerUserName());
        assertEquals(200L, response.getNewOwnerUserId());
        assertEquals("新负责人", response.getNewOwnerUserName());
        assertEquals(300L, response.getOperatorUserId());
        assertEquals("操作人", response.getOperatorUserName());
    }

    @Test
    void emptyOwnerRecordListSkipsUserLookup() {
        CrmCustomerController controller = new CrmCustomerController();
        AtomicBoolean userApiCalled = new AtomicBoolean();
        ReflectionTestUtils.setField(controller, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> List.of()));
        ReflectionTestUtils.setField(controller, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> {
                    userApiCalled.set(true);
                    return Map.of();
                }));

        CommonResult<List<CrmCustomerOwnerRecordRespVO>> result = controller.getCustomerOwnerRecordList(20L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData().isEmpty());
        assertFalse(userApiCalled.get());
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
