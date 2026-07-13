package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_NAME_EXISTS;

class CrmCustomerServiceImplTest {

    @Test
    void createCustomerRejectsDuplicateName() {
        String name = "重复客户";
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByCustomerName")) {
                        return new CrmCustomerDO().setId(10L).setName(name);
                    }
                    throw new AssertionError("名称冲突时不应继续调用 " + method.getName());
                }));
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO().setName(name).setOwnerUserId(1L);

        assertServiceException(() -> service.createCustomer(reqVO, 1L), CUSTOMER_NAME_EXISTS, name);
    }

    @Test
    void updateCustomerRejectsNameOwnedByAnotherCustomer() {
        String name = "其他客户已使用";
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectById")) {
                        return new CrmCustomerDO().setId(20L).setName("原客户").setOwnerUserId(1L);
                    }
                    if (method.getName().equals("selectByCustomerName")) {
                        return new CrmCustomerDO().setId(30L).setName(name);
                    }
                    throw new AssertionError("名称冲突时不应继续调用 " + method.getName());
                }));
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO().setId(20L).setName(name);

        assertServiceException(() -> service.updateCustomer(reqVO), CUSTOMER_NAME_EXISTS, name);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
