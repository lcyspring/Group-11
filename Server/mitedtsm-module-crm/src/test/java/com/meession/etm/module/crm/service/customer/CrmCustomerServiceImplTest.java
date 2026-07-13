package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerDuplicateCheckReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerOwnerRecordDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerOwnerRecordMapper;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordTypeEnum.PUT_POOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmCustomerServiceImplTest {

    @Test
    void duplicateCheckNormalizesConditionsAndReturnsCandidates() {
        AtomicReference<Object[]> queryArgs = new AtomicReference<>();
        CrmCustomerDO candidate = new CrmCustomerDO().setId(20L).setName("候选客户").setMobile("18000000000");
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectDuplicateList")) {
                        queryArgs.set(args);
                        return List.of(candidate);
                    }
                    throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                }));
        CrmCustomerDuplicateCheckReqVO reqVO = new CrmCustomerDuplicateCheckReqVO()
                .setName("  候选客户  ").setMobile(" 18000000000 ").setExcludeId(10L);

        List<CrmCustomerDO> result = service.getDuplicateCustomerList(reqVO, 1L);

        assertEquals(List.of(candidate), result);
        assertEquals("候选客户", queryArgs.get()[0]);
        assertEquals("18000000000", queryArgs.get()[1]);
        assertEquals(10L, queryArgs.get()[2]);
        assertEquals(1L, queryArgs.get()[3]);
    }

    @Test
    void duplicateCheckSkipsQueryWhenConditionsAreBlank() {
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> {
                    throw new AssertionError("空条件不应查询 Mapper");
                }));
        CrmCustomerDuplicateCheckReqVO reqVO = new CrmCustomerDuplicateCheckReqVO()
                .setName("  ").setMobile(null);

        assertTrue(service.getDuplicateCustomerList(reqVO, 1L).isEmpty());
        assertFalse(reqVO.isSearchConditionPresent());
        assertTrue(new CrmCustomerDuplicateCheckReqVO().setName("客户").isSearchConditionPresent());
        assertTrue(new CrmCustomerDuplicateCheckReqVO().setMobile("18000000000").isSearchConditionPresent());
    }

    @Test
    void putCustomerPoolRecordsPreviousOwner() {
        AtomicReference<CrmCustomerOwnerRecordDO> ownerRecord = new AtomicReference<>();
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerOwnerRecordMapper", proxy(CrmCustomerOwnerRecordMapper.class,
                (proxy, method, args) -> {
                    ownerRecord.set((CrmCustomerOwnerRecordDO) args[0]);
                    return 1;
                }));
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> 1));
        ReflectionTestUtils.setField(service, "contactService", proxy(CrmContactService.class,
                (proxy, method, args) -> null));
        ReflectionTestUtils.setField(service, "permissionService", proxy(CrmPermissionService.class,
                (proxy, method, args) -> null));
        CrmCustomerDO customer = new CrmCustomerDO().setId(20L).setOwnerUserId(100L);

        service.putCustomerPool(customer);

        assertEquals(20L, ownerRecord.get().getCustomerId());
        assertEquals(100L, ownerRecord.get().getOwnerUserId());
        assertEquals(PUT_POOL.getType(), ownerRecord.get().getType());
    }

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
                    if (method.getName().equals("selectHierarchyListForUpdate")) {
                        return List.of(new CrmCustomerDO().setId(20L));
                    }
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

    @Test
    void createCustomerPersistsValidParentCustomer() {
        AtomicReference<CrmCustomerDO> insertedCustomer = new AtomicReference<>();
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectHierarchyListForUpdate" -> List.of(new CrmCustomerDO().setId(10L));
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
                (proxy, method, args) -> null));

        service.createCustomer(new CrmCustomerSaveReqVO().setName("分公司")
                .setParentCustomerId(10L).setOwnerUserId(1L), 1L);

        assertEquals(10L, insertedCustomer.get().getParentCustomerId());
    }

    @Test
    void updateCustomerRejectsSelfAsParent() {
        CrmCustomerServiceImpl service = serviceWithHierarchy(List.of(new CrmCustomerDO().setId(10L)));
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO().setId(10L)
                .setName("客户 A").setParentCustomerId(10L);

        assertServiceException(() -> service.updateCustomer(reqVO), CUSTOMER_PARENT_SELF);
    }

    @Test
    void updateCustomerRejectsMissingOrCrossTenantParent() {
        CrmCustomerServiceImpl service = serviceWithHierarchy(List.of(new CrmCustomerDO().setId(10L)));
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO().setId(10L)
                .setName("客户 A").setParentCustomerId(999L);

        assertServiceException(() -> service.updateCustomer(reqVO), CUSTOMER_PARENT_NOT_EXISTS);
    }

    @Test
    void updateCustomerRejectsTwoNodeCycle() {
        CrmCustomerServiceImpl service = serviceWithHierarchy(List.of(
                new CrmCustomerDO().setId(10L),
                new CrmCustomerDO().setId(20L).setParentCustomerId(10L)));
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO().setId(10L)
                .setName("客户 A").setParentCustomerId(20L);

        assertServiceException(() -> service.updateCustomer(reqVO), CUSTOMER_HIERARCHY_CYCLE);
    }

    @Test
    void updateCustomerRejectsDeepCycle() {
        CrmCustomerServiceImpl service = serviceWithHierarchy(List.of(
                new CrmCustomerDO().setId(10L),
                new CrmCustomerDO().setId(20L).setParentCustomerId(10L),
                new CrmCustomerDO().setId(30L).setParentCustomerId(20L)));
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO().setId(10L)
                .setName("客户 A").setParentCustomerId(30L);

        assertServiceException(() -> service.updateCustomer(reqVO), CUSTOMER_HIERARCHY_CYCLE);
    }

    @Test
    void updateCustomerExplicitlyClearsParentRelationship() {
        AtomicReference<Long> updatedParentId = new AtomicReference<>(99L);
        CrmCustomerDO oldCustomer = new CrmCustomerDO().setId(10L).setName("分公司")
                .setOwnerUserId(1L).setParentCustomerId(20L);
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectHierarchyListForUpdate" -> List.of(oldCustomer,
                            new CrmCustomerDO().setId(20L));
                    case "selectById" -> oldCustomer;
                    case "selectByCustomerName" -> oldCustomer;
                    case "updateById" -> 1;
                    case "updateParentCustomerIdById" -> {
                        updatedParentId.set((Long) args[1]);
                        yield 1;
                    }
                    default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                }));
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO().setId(10L)
                .setName("分公司").setParentCustomerId(null);

        service.updateCustomer(reqVO);

        assertNull(updatedParentId.get());
    }

    @Test
    void deleteCustomerRejectsCustomerWithChildren() {
        List<CrmCustomerDO> hierarchy = List.of(new CrmCustomerDO().setId(10L),
                new CrmCustomerDO().setId(20L).setParentCustomerId(10L));
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectHierarchyListForUpdate" -> hierarchy;
                    case "selectById" -> new CrmCustomerDO().setId(10L).setName("集团总部");
                    default -> throw new AssertionError("存在下级客户时不应继续调用 " + method.getName());
                }));

        assertServiceException(() -> service.deleteCustomer(10L), CUSTOMER_DELETE_FAIL_HAVE_REFERENCE, "下级客户");
    }

    private static CrmCustomerServiceImpl serviceWithHierarchy(List<CrmCustomerDO> hierarchy) {
        CrmCustomerServiceImpl service = new CrmCustomerServiceImpl();
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectHierarchyListForUpdate")) {
                        return hierarchy;
                    }
                    throw new AssertionError("层级校验失败时不应继续调用 " + method.getName());
                }));
        return service;
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
