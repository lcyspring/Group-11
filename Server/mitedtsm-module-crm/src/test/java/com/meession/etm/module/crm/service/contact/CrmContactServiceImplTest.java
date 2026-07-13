package com.meession.etm.module.crm.service.contact;

import com.meession.etm.module.crm.controller.admin.contact.vo.CrmContactSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.mysql.contact.CrmContactMapper;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTACT_PRIMARY_DELETE_FAIL;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTACT_PRIMARY_MOVE_FAIL;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTACT_PRIMARY_SWITCH_CONFLICT;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTACT_PRIMARY_UNSET_FAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmContactServiceImplTest {

    @Test
    void getPrimaryContactsSkipsMapperForEmptyCustomerIds() {
        CrmContactMapper mapper = proxy(CrmContactMapper.class, (proxy, method, args) -> {
            throw new AssertionError("空客户集合不应查询 Mapper");
        });

        assertTrue(newService(mapper).getPrimaryContactListByCustomerIds(List.of()).isEmpty());
    }

    @Test
    void getPrimaryContactsUsesSingleBatchQuery() {
        CrmContactDO primary = contact(10L, 100L, true);
        CrmContactMapper mapper = proxy(CrmContactMapper.class, (proxy, method, args) -> {
            if (method.getName().equals("selectPrimaryContactListByCustomerIds")) {
                assertEquals(List.of(100L, 200L), args[0]);
                return List.of(primary);
            }
            throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
        });

        assertEquals(List.of(primary),
                newService(mapper).getPrimaryContactListByCustomerIds(List.of(100L, 200L)));
    }

    @Test
    void createFirstContactForcesPrimaryAfterCustomerLock() {
        List<String> calls = new ArrayList<>();
        AtomicReference<CrmContactDO> inserted = new AtomicReference<>();
        CrmContactMapper mapper = proxy(CrmContactMapper.class, (proxy, method, args) -> switch (method.getName()) {
            case "lockCustomerById" -> {
                calls.add("lock:" + args[0]);
                yield args[0];
            }
            case "selectPrimaryContactByCustomerId" -> {
                calls.add("select-primary:" + args[0]);
                yield null;
            }
            case "insert" -> {
                calls.add("insert");
                CrmContactDO contact = (CrmContactDO) args[0];
                contact.setId(10L);
                inserted.set(contact);
                yield 1;
            }
            default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
        });
        CrmContactServiceImpl service = newService(mapper);
        CrmContactSaveReqVO reqVO = contactReq(null, 100L, false);

        Long id = service.createContact(reqVO, 1L);

        assertEquals(10L, id);
        assertTrue(reqVO.getPrimaryContact());
        assertTrue(inserted.get().getPrimaryContact());
        assertEquals(List.of("lock:100", "select-primary:100", "insert"), calls);
    }

    @Test
    void createRequestedPrimaryUnsetsExistingPrimary() {
        List<String> calls = new ArrayList<>();
        CrmContactDO oldPrimary = new CrmContactDO().setId(8L).setName("原首联系人").setPrimaryContact(true);
        CrmContactMapper mapper = proxy(CrmContactMapper.class, (proxy, method, args) -> switch (method.getName()) {
            case "lockCustomerById" -> {
                calls.add("lock");
                yield args[0];
            }
            case "selectPrimaryContactByCustomerId" -> {
                calls.add("select-primary");
                yield oldPrimary;
            }
            case "unsetPrimaryContact" -> {
                calls.add("unset:" + args[0]);
                yield 1;
            }
            case "insert" -> {
                calls.add("insert");
                ((CrmContactDO) args[0]).setId(10L);
                yield 1;
            }
            default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
        });
        CrmContactServiceImpl service = newService(mapper);

        service.createContact(contactReq(null, 100L, true), 1L);

        assertEquals(List.of("lock", "select-primary", "unset:8", "insert"), calls);
    }

    @Test
    void updateNewPrimaryUnsetsExistingPrimaryAfterLock() {
        List<String> calls = new ArrayList<>();
        AtomicReference<CrmContactDO> updated = new AtomicReference<>();
        CrmContactDO oldContact = contact(10L, 100L, false);
        CrmContactDO oldPrimary = new CrmContactDO().setId(8L).setName("原首联系人").setPrimaryContact(true);
        CrmContactMapper mapper = proxy(CrmContactMapper.class, (proxy, method, args) -> switch (method.getName()) {
            case "selectById" -> {
                calls.add("select-contact");
                yield oldContact;
            }
            case "lockCustomerById" -> {
                calls.add("lock:" + args[0]);
                yield args[0];
            }
            case "selectPrimaryContactByCustomerId" -> {
                calls.add("select-primary");
                yield oldPrimary;
            }
            case "unsetPrimaryContact" -> {
                calls.add("unset:" + args[0]);
                yield 1;
            }
            case "updateById" -> {
                calls.add("update");
                updated.set((CrmContactDO) args[0]);
                yield 1;
            }
            default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
        });
        CrmContactServiceImpl service = newService(mapper);

        service.updateContact(contactReq(10L, 100L, true));

        assertTrue(updated.get().getPrimaryContact());
        assertEquals(List.of("select-contact", "lock:100", "select-contact", "select-primary", "unset:8", "update"), calls);
    }

    @Test
    void updateNewPrimaryRejectsLostPrimaryStateInsteadOfCreatingDuplicate() {
        CrmContactDO oldContact = contact(10L, 100L, false);
        CrmContactDO oldPrimary = new CrmContactDO().setId(8L).setName("原首联系人").setPrimaryContact(true);
        CrmContactMapper mapper = proxy(CrmContactMapper.class, (proxy, method, args) -> switch (method.getName()) {
            case "selectById" -> oldContact;
            case "lockCustomerById" -> args[0];
            case "selectPrimaryContactByCustomerId" -> oldPrimary;
            case "unsetPrimaryContact" -> 0;
            default -> throw new AssertionError("状态冲突后不应调用 " + method.getName());
        });
        CrmContactServiceImpl service = newService(mapper);

        assertServiceException(() -> service.updateContact(contactReq(10L, 100L, true)),
                CONTACT_PRIMARY_SWITCH_CONFLICT);
    }

    @Test
    void updateCurrentPrimaryRejectsDirectUnset() {
        List<String> calls = new ArrayList<>();
        CrmContactDO oldContact = contact(10L, 100L, true);
        CrmContactMapper mapper = rejectingLifecycleMapper(oldContact, calls);
        CrmContactServiceImpl service = newService(mapper);

        assertServiceException(() -> service.updateContact(contactReq(10L, 100L, false)),
                CONTACT_PRIMARY_UNSET_FAIL);

        assertEquals(List.of("select-contact", "lock:100", "select-contact"), calls);
    }

    @Test
    void updateCurrentPrimaryRejectsMovingToAnotherCustomerAndLocksInIdOrder() {
        List<String> calls = new ArrayList<>();
        CrmContactDO oldContact = contact(10L, 200L, true);
        CrmContactMapper mapper = rejectingLifecycleMapper(oldContact, calls);
        CrmContactServiceImpl service = newService(mapper);

        assertServiceException(() -> service.updateContact(contactReq(10L, 100L, true)),
                CONTACT_PRIMARY_MOVE_FAIL);

        assertEquals(List.of("select-contact", "lock:100", "lock:200", "select-contact"), calls);
    }

    @Test
    void deleteCurrentPrimaryRejectsBeforeContractAndDeleteSideEffects() {
        List<String> calls = new ArrayList<>();
        CrmContactDO oldContact = contact(10L, 100L, true);
        CrmContactMapper mapper = rejectingLifecycleMapper(oldContact, calls);
        CrmContactServiceImpl service = newService(mapper);
        ReflectionTestUtils.setField(service, "contractService", proxy(CrmContractService.class,
                (proxy, method, args) -> {
                    throw new AssertionError("首联系人拒绝删除后不应查询合同");
                }));

        assertServiceException(() -> service.deleteContact(10L), CONTACT_PRIMARY_DELETE_FAIL);

        assertEquals(List.of("select-contact", "lock:100", "select-contact"), calls);
    }

    @Test
    void movingNonPrimaryIntoCustomerWithoutPrimaryRepairsInvariant() {
        List<Long> lockedCustomerIds = new ArrayList<>();
        AtomicReference<CrmContactDO> updated = new AtomicReference<>();
        CrmContactDO oldContact = contact(10L, 100L, false);
        CrmContactMapper mapper = proxy(CrmContactMapper.class, (proxy, method, args) -> switch (method.getName()) {
            case "selectById" -> oldContact;
            case "lockCustomerById" -> {
                lockedCustomerIds.add((Long) args[0]);
                yield args[0];
            }
            case "selectPrimaryContactByCustomerId" -> null;
            case "updateById" -> {
                updated.set((CrmContactDO) args[0]);
                yield 1;
            }
            default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
        });
        CrmContactServiceImpl service = newService(mapper);
        CrmContactSaveReqVO reqVO = contactReq(10L, 200L, false);

        service.updateContact(reqVO);

        assertEquals(List.of(100L, 200L), lockedCustomerIds);
        assertTrue(reqVO.getPrimaryContact());
        assertTrue(updated.get().getPrimaryContact());
        assertFalse(oldContact.getPrimaryContact());
    }

    private static CrmContactMapper rejectingLifecycleMapper(CrmContactDO contact, List<String> calls) {
        return proxy(CrmContactMapper.class, (proxy, method, args) -> switch (method.getName()) {
            case "selectById" -> {
                calls.add("select-contact");
                yield contact;
            }
            case "lockCustomerById" -> {
                calls.add("lock:" + args[0]);
                yield args[0];
            }
            default -> throw new AssertionError("约束拒绝后不应调用 " + method.getName());
        });
    }

    private static CrmContactServiceImpl newService(CrmContactMapper mapper) {
        CrmContactServiceImpl service = new CrmContactServiceImpl();
        ReflectionTestUtils.setField(service, "contactMapper", mapper);
        ReflectionTestUtils.setField(service, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> method.getName().equals("getCustomer")
                        ? new CrmCustomerDO().setId((Long) args[0]) : null));
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> null));
        ReflectionTestUtils.setField(service, "permissionService", proxy(CrmPermissionService.class,
                (proxy, method, args) -> null));
        return service;
    }

    private static CrmContactSaveReqVO contactReq(Long id, Long customerId, boolean primaryContact) {
        return new CrmContactSaveReqVO().setId(id).setName("联系人").setCustomerId(customerId)
                .setOwnerUserId(1L).setMaster(false).setPrimaryContact(primaryContact);
    }

    private static CrmContactDO contact(Long id, Long customerId, boolean primaryContact) {
        return new CrmContactDO().setId(id).setName("联系人").setCustomerId(customerId)
                .setOwnerUserId(1L).setMaster(false).setPrimaryContact(primaryContact);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
