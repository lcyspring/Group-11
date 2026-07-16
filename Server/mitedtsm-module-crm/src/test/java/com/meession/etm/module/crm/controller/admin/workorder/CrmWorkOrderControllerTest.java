package com.meession.etm.module.crm.controller.admin.workorder;

import com.meession.etm.module.crm.controller.admin.workorder.vo.CrmWorkOrderRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.workorder.CrmWorkOrderGroupService;
import com.meession.etm.module.crm.service.workorder.CrmWorkOrderService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrmWorkOrderControllerTest {
    @Test
    @SuppressWarnings("unchecked")
    void unassignedUngroupedOrderDoesNotLookupNullInImmutableMaps() {
        CrmWorkOrderController controller = new CrmWorkOrderController();
        CrmCustomerService customerService = mock(CrmCustomerService.class);
        CrmWorkOrderService workOrderService = mock(CrmWorkOrderService.class);
        CrmWorkOrderGroupService groupService = mock(CrmWorkOrderGroupService.class);
        AdminUserApi adminUserApi = mock(AdminUserApi.class);
        ReflectionTestUtils.setField(controller, "customerService", customerService);
        ReflectionTestUtils.setField(controller, "workOrderService", workOrderService);
        ReflectionTestUtils.setField(controller, "groupService", groupService);
        ReflectionTestUtils.setField(controller, "adminUserApi", adminUserApi);

        when(customerService.getCustomerMap(java.util.Set.of(17L)))
                .thenReturn(Map.of(17L, new CrmCustomerDO().setId(17L).setName("客户甲")));
        when(workOrderService.getCcUserIdsMap(List.of(2L))).thenReturn(Map.of());
        when(groupService.getGroupMap(List.of())).thenReturn(Map.of());
        when(adminUserApi.getUserMap(java.util.Set.of(1L)))
                .thenReturn(Map.of(1L, new AdminUserRespDTO().setId(1L).setNickname("创建人")));

        CrmWorkOrderDO order = new CrmWorkOrderDO();
        order.setId(2L);
        order.setCustomerId(17L);
        order.setCreator("1");
        order.setHandlerUserId(null);
        order.setGroupId(null);
        List<CrmWorkOrderRespVO> result = ReflectionTestUtils.invokeMethod(controller, "build", List.of(order));

        assertEquals("客户甲", result.get(0).getCustomerName());
        assertEquals("创建人", result.get(0).getCreatorName());
    }
}
