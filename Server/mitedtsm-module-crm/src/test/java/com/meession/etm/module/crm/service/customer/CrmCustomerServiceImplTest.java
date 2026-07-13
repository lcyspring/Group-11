package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_NAME_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmCustomerServiceImplTest {

    @Test
    void createCustomerUsesSelectedOwnerForDataAndPermission() {
        long operatorUserId = 1L;
        long ownerUserId = 100L;
        AtomicReference<CrmCustomerDO> insertedCustomer = new AtomicReference<>();
        AtomicReference<CrmPermissionCreateReqBO> createdPermission = new AtomicReference<>();
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectByCustomerName" -> null;
                    case "insert" -> {
                        CrmCustomerDO customer = (CrmCustomerDO) args[0];
                        customer.setId(20L);
                        insertedCustomer.set(customer);
                        yield 1;
                    }
                    default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> null));
        ReflectionTestUtils.setField(service, "customerLimitConfigService", proxy(CrmCustomerLimitConfigService.class,
                (proxy, method, args) -> Collections.emptyList()));
        ReflectionTestUtils.setField(service, "permissionService", proxy(CrmPermissionService.class,
                (proxy, method, args) -> {
                    createdPermission.set((CrmPermissionCreateReqBO) args[0]);
                    return null;
                }));
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO()
                .setName("指定负责人客户")
                .setOwnerUserId(ownerUserId);

        Long customerId = service.createCustomer(reqVO, operatorUserId);

        assertEquals(20L, customerId);
        assertEquals(ownerUserId, insertedCustomer.get().getOwnerUserId());
        assertEquals(ownerUserId, createdPermission.get().getUserId());
        assertEquals(20L, createdPermission.get().getBizId());
    }

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
