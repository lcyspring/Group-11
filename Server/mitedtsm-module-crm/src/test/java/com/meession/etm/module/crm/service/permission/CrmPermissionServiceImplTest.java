package com.meession.etm.module.crm.service.permission;

import com.meession.etm.framework.common.biz.system.permission.PermissionCommonApi;
import com.meession.etm.module.crm.controller.admin.permission.vo.CrmPermissionSaveReqVO;
import com.meession.etm.module.crm.controller.admin.permission.vo.CrmPermissionUpdateReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.permission.CrmPermissionDO;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.permission.CrmPermissionMapper;
import com.meession.etm.module.crm.enums.clue.CrmCluePoolStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolStatusEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationProperties;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CLUE_UPDATE_FAIL_TRANSFORMED;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CRM_PERMISSION_NOT_EXISTS;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CRM_EXPORT_PERMISSION_DENIED;

class CrmPermissionServiceImplTest {

    @Test
    void hasPermissionUsesOwnerWriteReadHierarchy() {
        CrmPermissionServiceImpl service = permissionCheckService(List.of(
                new CrmPermissionDO().setUserId(7L).setLevel(CrmPermissionLevelEnum.OWNER.getLevel())));

        assertTrue(service.hasPermission(CrmBizTypeEnum.CRM_CLUE.getType(), 10L, 7L,
                CrmPermissionLevelEnum.WRITE));
        assertTrue(service.hasPermission(CrmBizTypeEnum.CRM_CLUE.getType(), 10L, 7L,
                CrmPermissionLevelEnum.READ));
    }

    @Test
    void hasPermissionDoesNotPromoteReadGrantToWrite() {
        CrmPermissionServiceImpl service = permissionCheckService(List.of(
                new CrmPermissionDO().setUserId(7L).setLevel(CrmPermissionLevelEnum.READ.getLevel())));

        assertFalse(service.hasPermission(CrmBizTypeEnum.CRM_CLUE.getType(), 10L, 7L,
                CrmPermissionLevelEnum.WRITE));
    }

    @Test
    void hasPermissionAllowsOnlyExplicitPublicObjectsWithoutGrant() {
        CrmPermissionServiceImpl service = permissionCheckService(List.of());
        ReflectionTestUtils.setField(service, "clueMapper", proxy(CrmClueMapper.class,
                (proxy, method, args) -> new CrmClueDO().setId(10L).setTransformStatus(false)
                        .setPoolStatus(CrmCluePoolStatusEnum.PUBLIC.getStatus()).setOwnerUserId(null)));
        ReflectionTestUtils.setField(service, "customerMapper", proxy(CrmCustomerMapper.class,
                (proxy, method, args) -> new CrmCustomerDO().setId(20L)
                        .setPoolStatus(CrmCustomerPoolStatusEnum.OWNED.getStatus())));

        assertTrue(service.hasPermission(CrmBizTypeEnum.CRM_CLUE.getType(), 10L, 7L,
                CrmPermissionLevelEnum.READ));
        assertFalse(service.hasPermission(CrmBizTypeEnum.CRM_CUSTOMER.getType(), 20L, 7L,
                CrmPermissionLevelEnum.READ));
    }

    @Test
    void replaceOwnerPermissionPromotesTargetAndPreservesTeamPermission() {
        AtomicReference<CrmPermissionDO> updated = new AtomicReference<>();
        AtomicReference<Set<Long>> deleted = new AtomicReference<>();
        List<CrmPermissionDO> permissions = List.of(
                new CrmPermissionDO().setId(1L).setUserId(1L).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()),
                new CrmPermissionDO().setId(2L).setUserId(2L).setLevel(CrmPermissionLevelEnum.READ.getLevel()),
                new CrmPermissionDO().setId(3L).setUserId(3L).setLevel(CrmPermissionLevelEnum.WRITE.getLevel()));
        CrmPermissionServiceImpl service = new CrmPermissionServiceImpl();
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> null));
        ReflectionTestUtils.setField(service, "permissionMapper", proxy(CrmPermissionMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectByBizTypeAndBizId" -> permissions;
                    case "updateById" -> {
                        updated.set((CrmPermissionDO) args[0]);
                        yield 1;
                    }
                    case "deleteByIds" -> {
                        Collection<?> ids = (Collection<?>) args[0];
                        deleted.set(ids.stream().map(id -> (Long) id).collect(java.util.stream.Collectors.toSet()));
                        yield 1;
                    }
                    default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                }));

        service.replaceOwnerPermission(CrmBizTypeEnum.CRM_CONTACT.getType(), 10L, 3L);

        assertEquals(3L, updated.get().getId());
        assertEquals(CrmPermissionLevelEnum.OWNER.getLevel(), updated.get().getLevel());
        assertEquals(Set.of(1L), deleted.get());
    }

    @Test
    void replaceOwnerPermissionWithNullOnlyDeletesOwner() {
        AtomicReference<Set<Long>> deleted = new AtomicReference<>();
        List<CrmPermissionDO> permissions = List.of(
                new CrmPermissionDO().setId(1L).setUserId(1L).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()),
                new CrmPermissionDO().setId(2L).setUserId(2L).setLevel(CrmPermissionLevelEnum.READ.getLevel()));
        CrmPermissionServiceImpl service = new CrmPermissionServiceImpl();
        ReflectionTestUtils.setField(service, "permissionMapper", proxy(CrmPermissionMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectByBizTypeAndBizId" -> permissions;
                    case "deleteByIds" -> {
                        Collection<?> ids = (Collection<?>) args[0];
                        deleted.set(ids.stream().map(id -> (Long) id).collect(java.util.stream.Collectors.toSet()));
                        yield 1;
                    }
                    default -> throw new AssertionError("清空负责人不应调用 " + method.getName());
                }));

        service.replaceOwnerPermission(CrmBizTypeEnum.CRM_CONTACT.getType(), 10L, null);

        assertEquals(Set.of(1L), deleted.get());
        assertEquals(CrmPermissionLevelEnum.READ.getLevel(), permissions.get(1).getLevel());
    }

    @Test
    void replaceOwnerPermissionRemovesDuplicateOwnersForTargetUser() {
        AtomicReference<Collection<?>> deleted = new AtomicReference<>();
        List<CrmPermissionDO> permissions = List.of(
                new CrmPermissionDO().setId(1L).setUserId(9L).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()),
                new CrmPermissionDO().setId(2L).setUserId(9L).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()),
                new CrmPermissionDO().setId(3L).setUserId(8L).setLevel(CrmPermissionLevelEnum.WRITE.getLevel()));
        CrmPermissionServiceImpl service = new CrmPermissionServiceImpl();
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> null));
        ReflectionTestUtils.setField(service, "permissionMapper", proxy(CrmPermissionMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectByBizTypeAndBizId" -> permissions;
                    case "deleteByIds" -> {
                        deleted.set((Collection<?>) args[0]);
                        yield 1;
                    }
                    default -> throw new AssertionError("已有正确 OWNER 时不应调用 " + method.getName());
                }));

        service.replaceOwnerPermission(CrmBizTypeEnum.CRM_CONTACT.getType(), 10L, 9L);

        assertEquals(Set.of(2L), Set.copyOf(deleted.get()));
    }

    @Test
    void transformedClueRejectsTeamPermissionCreate() {
        CrmPermissionServiceImpl service = new CrmPermissionServiceImpl();
        ReflectionTestUtils.setField(service, "clueMapper", proxy(CrmClueMapper.class,
                (proxy, method, args) -> new CrmClueDO().setId(10L).setTransformStatus(true)));
        ReflectionTestUtils.setField(service, "permissionMapper", proxy(CrmPermissionMapper.class,
                (proxy, method, args) -> {
                    throw new AssertionError("只读校验失败后不应写权限");
                }));

        CrmPermissionSaveReqVO reqVO = new CrmPermissionSaveReqVO().setBizType(CrmBizTypeEnum.CRM_CLUE.getType())
                .setBizId(10L).setUserId(2L).setLevel(CrmPermissionLevelEnum.READ.getLevel());
        assertServiceException(() -> service.createPermission(reqVO, 1L), CLUE_UPDATE_FAIL_TRANSFORMED);
    }

    @Test
    void permissionUpdateRejectsIdsFromForgedBusinessObject() {
        CrmPermissionServiceImpl service = new CrmPermissionServiceImpl();
        ReflectionTestUtils.setField(service, "permissionMapper", proxy(CrmPermissionMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByIds")) {
                        return List.of(new CrmPermissionDO().setId(30L)
                                .setBizType(CrmBizTypeEnum.CRM_CLUE.getType()).setBizId(10L));
                    }
                    throw new AssertionError("伪造对象参数不应继续调用 " + method.getName());
                }));

        CrmPermissionUpdateReqVO reqVO = new CrmPermissionUpdateReqVO().setIds(List.of(30L))
                .setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType()).setBizId(20L)
                .setLevel(CrmPermissionLevelEnum.READ.getLevel());
        assertServiceException(() -> service.updatePermission(reqVO), CRM_PERMISSION_NOT_EXISTS);
    }

    @Test
    void exportRejectsReadOnlyCollaborator() {
        CrmPermissionServiceImpl service = exportService(List.of(new CrmPermissionDO().setBizId(10L)
                .setUserId(2L).setLevel(CrmPermissionLevelEnum.READ.getLevel())), List.of());

        assertServiceException(() -> service.validateExportPermission(
                CrmBizTypeEnum.CRM_CUSTOMER.getType(), List.of(10L), 2L), CRM_EXPORT_PERMISSION_DENIED, "客户");
    }

    @Test
    void exportAllowsWriteCollaborator() {
        CrmPermissionServiceImpl service = exportService(List.of(new CrmPermissionDO().setBizId(10L)
                .setUserId(2L).setLevel(CrmPermissionLevelEnum.WRITE.getLevel())), List.of());

        service.validateExportPermission(CrmBizTypeEnum.CRM_CUSTOMER.getType(), List.of(10L), 2L);
    }

    @Test
    void exportRejectsSubordinateOwnerWithoutDirectWriteGrant() {
        CrmPermissionServiceImpl service = exportService(List.of(new CrmPermissionDO().setBizId(10L)
                .setUserId(3L).setLevel(CrmPermissionLevelEnum.OWNER.getLevel())),
                List.of(new AdminUserRespDTO().setId(3L)));

        assertServiceException(() -> service.validateExportPermission(
                CrmBizTypeEnum.CRM_CUSTOMER.getType(), List.of(10L), 2L), CRM_EXPORT_PERMISSION_DENIED, "客户");
    }

    private static CrmPermissionServiceImpl exportService(List<CrmPermissionDO> permissions,
                                                          List<AdminUserRespDTO> subordinates) {
        CrmPermissionServiceImpl service = new CrmPermissionServiceImpl();
        CrmAuthorizationService authorizationService = new CrmAuthorizationService();
        CrmAuthorizationProperties properties = new CrmAuthorizationProperties();
        properties.setAdminRoleCodes(List.of("crm_admin"));
        ReflectionTestUtils.setField(authorizationService, "properties", properties);
        ReflectionTestUtils.setField(authorizationService, "permissionCommonApi", proxy(PermissionCommonApi.class,
                (proxy, method, args) -> false));
        ReflectionTestUtils.setField(service, "crmAuthorizationService", authorizationService);
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> method.getName().equals("getUserListBySubordinate")
                        ? subordinates : null));
        ReflectionTestUtils.setField(service, "permissionMapper", proxy(CrmPermissionMapper.class,
                (proxy, method, args) -> method.getName().equals("selectByBizTypeAndBizIds")
                        ? permissions : null));
        return service;
    }

    private static CrmPermissionServiceImpl permissionCheckService(List<CrmPermissionDO> permissions) {
        CrmPermissionServiceImpl service = new CrmPermissionServiceImpl();
        ReflectionTestUtils.setField(service, "permissionMapper", proxy(CrmPermissionMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByBizTypeAndBizId")) return permissions;
                    throw new AssertionError("未预期的权限 Mapper 调用 " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "crmAuthorizationService", new CrmAuthorizationService() {
            @Override
            public boolean isGranted(Collection<CrmPermissionDO> grants, Long userId, Integer requiredLevel) {
                CrmPermissionDO direct = grants.stream().filter(item -> item.getUserId().equals(userId)).findFirst().orElse(null);
                if (direct == null) return false;
                if (direct.getLevel().equals(CrmPermissionLevelEnum.OWNER.getLevel())) return true;
                if (requiredLevel.equals(CrmPermissionLevelEnum.READ.getLevel())) return true;
                return direct.getLevel().equals(CrmPermissionLevelEnum.WRITE.getLevel())
                        && requiredLevel.equals(CrmPermissionLevelEnum.WRITE.getLevel());
            }
        });
        return service;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
