package com.meession.etm.module.crm.framework.permission;

import com.meession.etm.framework.common.biz.system.permission.PermissionCommonApi;
import com.meession.etm.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.meession.etm.module.crm.dal.dataobject.permission.CrmPermissionDO;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmAuthorizationServiceTest {

    @Test
    void missingPlatformScopeFailsClosedToSelf() {
        CrmAuthorizationService service = service(false, null, List.of(), List.of());

        CrmOwnerReadScope result = service.resolveOwnerReadScope(7L);

        assertFalse(result.all());
        assertEquals(Set.of(7L), result.ownerUserIds());
    }

    @Test
    void departmentScopeResolvesOwnerUsers() {
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(Set.of(20L, 21L));
        CrmAuthorizationService service = service(false, dataPermission,
                List.of(new AdminUserRespDTO().setId(8L), new AdminUserRespDTO().setId(9L)), List.of());

        CrmOwnerReadScope result = service.resolveOwnerReadScope(7L);

        assertFalse(result.all());
        assertEquals(Set.of(8L, 9L), result.ownerUserIds());
    }

    @Test
    void allScopeDoesNotEnumerateDepartmentUsers() {
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setAll(true);
        CrmAuthorizationService service = service(false, dataPermission, List.of(), List.of());

        CrmOwnerReadScope result = service.resolveOwnerReadScope(7L);

        assertTrue(result.all());
        assertTrue(result.ownerUserIds().isEmpty());
    }

    @Test
    void organizationOwnerGrantsReadButNeverWrite() {
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(Set.of(20L));
        CrmAuthorizationService service = service(false, dataPermission,
                List.of(new AdminUserRespDTO().setId(8L)), List.of());
        List<CrmPermissionDO> permissions = List.of(new CrmPermissionDO().setUserId(8L)
                .setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));

        assertTrue(service.isGranted(permissions, 7L, CrmPermissionLevelEnum.READ.getLevel()));
        assertFalse(service.isGranted(permissions, 7L, CrmPermissionLevelEnum.WRITE.getLevel()));
        assertFalse(service.isGranted(permissions, 7L, CrmPermissionLevelEnum.OWNER.getLevel()));
    }

    @Test
    void directObjectGrantKeepsPermissionHierarchy() {
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setSelf(true);
        CrmAuthorizationService service = service(false, dataPermission, List.of(), List.of());
        List<CrmPermissionDO> permissions = List.of(new CrmPermissionDO().setUserId(7L)
                .setLevel(CrmPermissionLevelEnum.WRITE.getLevel()));

        assertTrue(service.isGranted(permissions, 7L, CrmPermissionLevelEnum.READ.getLevel()));
        assertTrue(service.isGranted(permissions, 7L, CrmPermissionLevelEnum.WRITE.getLevel()));
        assertFalse(service.isGranted(permissions, 7L, CrmPermissionLevelEnum.OWNER.getLevel()));
    }

    @Test
    void subordinateViewIntersectsOrganizationScope() {
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(Set.of(20L));
        CrmAuthorizationService service = service(false, dataPermission,
                List.of(new AdminUserRespDTO().setId(8L)),
                List.of(new AdminUserRespDTO().setId(8L), new AdminUserRespDTO().setId(9L)));

        assertEquals(Set.of(8L), service.resolveReadableSubordinateOwnerUserIds(7L));
    }

    @Test
    void configuredAdminRoleBypassesEveryObjectLevel() {
        CrmAuthorizationService service = service(true, null, List.of(), List.of());

        assertTrue(service.resolveOwnerReadScope(7L).all());
        assertTrue(service.isGranted(List.of(), 7L, CrmPermissionLevelEnum.OWNER.getLevel()));
    }

    private static CrmAuthorizationService service(boolean admin, DeptDataPermissionRespDTO dataPermission,
                                                   List<AdminUserRespDTO> departmentUsers,
                                                   List<AdminUserRespDTO> subordinates) {
        CrmAuthorizationService service = new CrmAuthorizationService();
        CrmAuthorizationProperties properties = new CrmAuthorizationProperties();
        properties.setAdminRoleCodes(List.of("super_admin", "crm_admin"));
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "permissionCommonApi", proxy(PermissionCommonApi.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasAnyRoles" -> admin;
                    case "getDeptDataPermission" -> dataPermission;
                    default -> false;
                }));
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUserListByDeptIds" -> departmentUsers;
                    case "getUserListBySubordinate" -> subordinates;
                    default -> null;
                }));
        return service;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
