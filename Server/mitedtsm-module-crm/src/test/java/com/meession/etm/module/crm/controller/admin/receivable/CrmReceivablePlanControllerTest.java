package com.meession.etm.module.crm.controller.admin.receivable;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.receivable.vo.plan.CrmReceivablePlanPageReqVO;
import com.meession.etm.module.crm.controller.admin.receivable.vo.plan.CrmReceivablePlanRespVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.receivable.CrmReceivablePlanStatusEnum;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.receivable.CrmReceivablePlanService;
import com.meession.etm.module.crm.service.receivable.CrmReceivableService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmReceivablePlanControllerTest {

    @Test
    void planStatusAndAmountsRequireApprovedReceivable() {
        CrmReceivablePlanController controller = new CrmReceivablePlanController();
        List<CrmReceivablePlanDO> plans = List.of(
                plan(1L, 11L, LocalDateTime.now().plusDays(1)),
                plan(2L, null, LocalDateTime.now().minusDays(1)),
                plan(3L, 13L, LocalDateTime.now().plusDays(1)));
        ReflectionTestUtils.setField(controller, "receivablePlanService", proxy(CrmReceivablePlanService.class,
                (proxy, method, args) -> new PageResult<>(plans, (long) plans.size())));
        ReflectionTestUtils.setField(controller, "receivableService", proxy(CrmReceivableService.class,
                (proxy, method, args) -> new HashMap<>(Map.of(
                        11L, receivable(11L, CrmAuditStatusEnum.PROCESS, "30.00"),
                        13L, receivable(13L, CrmAuditStatusEnum.APPROVE, "40.00")))));
        ReflectionTestUtils.setField(controller, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> Map.of(30L, new CrmCustomerDO().setId(30L).setName("客户"))));
        ReflectionTestUtils.setField(controller, "contractService", proxy(CrmContractService.class,
                (proxy, method, args) -> Map.of(20L, new CrmContractDO().setId(20L).setNo("HT-20"))));
        ReflectionTestUtils.setField(controller, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> Map.of(1L, new AdminUserRespDTO().setId(1L).setNickname("负责人"))));

        CommonResult<PageResult<CrmReceivablePlanRespVO>> result = controller.getReceivablePlanPage(
                new CrmReceivablePlanPageReqVO());

        assertEquals(0, result.getCode());
        List<CrmReceivablePlanRespVO> response = result.getData().getList();
        assertPlan(response.get(0), CrmReceivablePlanStatusEnum.PENDING, "0", "50.00");
        assertPlan(response.get(1), CrmReceivablePlanStatusEnum.OVERDUE, "0", "50.00");
        assertPlan(response.get(2), CrmReceivablePlanStatusEnum.RECEIVED, "40.00", "10.00");
    }

    private static CrmReceivablePlanDO plan(Long id, Long receivableId, LocalDateTime returnTime) {
        CrmReceivablePlanDO plan = new CrmReceivablePlanDO().setId(id).setPeriod(id.intValue())
                .setCustomerId(30L).setContractId(20L).setOwnerUserId(1L).setReceivableId(receivableId)
                .setReturnTime(returnTime).setPrice(new BigDecimal("50.00"));
        plan.setCreator("1");
        return plan;
    }

    private static CrmReceivableDO receivable(Long id, CrmAuditStatusEnum status, String price) {
        return new CrmReceivableDO().setId(id).setAuditStatus(status.getStatus())
                .setPrice(new BigDecimal(price)).setReturnTime(LocalDateTime.now());
    }

    private static void assertPlan(CrmReceivablePlanRespVO plan, CrmReceivablePlanStatusEnum status,
                                   String receivedPrice, String unreceivedPrice) {
        assertEquals(status.getStatus(), plan.getStatus());
        assertEquals(0, new BigDecimal(receivedPrice).compareTo(plan.getReceivedPrice()));
        assertEquals(0, new BigDecimal(unreceivedPrice).compareTo(plan.getUnreceivedPrice()));
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
