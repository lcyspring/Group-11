package com.meession.etm.module.crm.controller.admin.customer;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerDuplicateCheckReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerDuplicateRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
