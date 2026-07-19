package com.meession.etm.module.crm.framework.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.biz.system.permission.PermissionCommonApi;
import com.meession.etm.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.meession.etm.module.crm.dal.dataobject.permission.CrmPermissionDO;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * Single merge point for CRM object permission and organization data scope.
 */
@Service
public class CrmAuthorizationService {

    @Resource
    private PermissionCommonApi permissionCommonApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private CrmAuthorizationProperties properties;

    public boolean isCrmAdmin(Long userId) {
        return permissionCommonApi.hasAnyRoles(userId, properties.getAdminRoleCodes().toArray(String[]::new));
    }

    /**
     * Converts ALL / CUSTOM / DEPT / DEPT_AND_CHILD / SELF into CRM owner-user scope.
     * A missing response fails closed to SELF so a platform integration outage cannot expose all CRM data.
     */
    public CrmOwnerReadScope resolveOwnerReadScope(Long userId) {
        if (isCrmAdmin(userId)) {
            return new CrmOwnerReadScope(true, Set.of());
        }
        DeptDataPermissionRespDTO dataPermission = permissionCommonApi.getDeptDataPermission(userId);
        if (dataPermission == null) {
            return new CrmOwnerReadScope(false, Set.of(userId));
        }
        if (Boolean.TRUE.equals(dataPermission.getAll())) {
            return new CrmOwnerReadScope(true, Set.of());
        }
        Set<Long> ownerUserIds = new HashSet<>();
        if (Boolean.TRUE.equals(dataPermission.getSelf())) {
            ownerUserIds.add(userId);
        }
        if (CollUtil.isNotEmpty(dataPermission.getDeptIds())) {
            List<AdminUserRespDTO> departmentUsers = adminUserApi.getUserListByDeptIds(dataPermission.getDeptIds());
            ownerUserIds.addAll(convertSet(CollUtil.emptyIfNull(departmentUsers), AdminUserRespDTO::getId));
        }
        return new CrmOwnerReadScope(false, ownerUserIds);
    }

    /**
     * Object grants are direct grants to the acting user. Organization scope is considered for READ only.
     */
    public boolean isGranted(Collection<CrmPermissionDO> permissions, Long userId, Integer requiredLevel) {
        if (isCrmAdmin(userId)) {
            return true;
        }
        CrmPermissionDO directPermission = CollUtil.findOne(permissions,
                permission -> ObjUtil.equal(permission.getUserId(), userId));
        if (directPermission != null && satisfies(directPermission.getLevel(), requiredLevel)) {
            return true;
        }
        if (!CrmPermissionLevelEnum.isRead(requiredLevel)) {
            return false;
        }
        CrmOwnerReadScope readScope = resolveOwnerReadScope(userId);
        return permissions.stream().anyMatch(permission -> CrmPermissionLevelEnum.isOwner(permission.getLevel())
                && readScope.allows(permission.getUserId()));
    }

    public Set<Long> resolveReadableSubordinateOwnerUserIds(Long userId) {
        Set<Long> subordinateUserIds = convertSet(CollUtil.emptyIfNull(adminUserApi.getUserListBySubordinate(userId)),
                AdminUserRespDTO::getId);
        CrmOwnerReadScope readScope = resolveOwnerReadScope(userId);
        if (!readScope.all()) {
            subordinateUserIds.retainAll(readScope.ownerUserIds());
        }
        return subordinateUserIds;
    }

    /**
     * Dedicated manager command gate for putting a subordinate customer into the public pool.
     * It does not turn organization read scope into general write access: the caller must also
     * hold the existing customer-distribution action permission.
     */
    public boolean canPutOwnerCustomerIntoPool(Long operatorUserId, Long ownerUserId) {
        if (ObjUtil.equal(operatorUserId, ownerUserId) || isCrmAdmin(operatorUserId)) {
            return true;
        }
        return permissionCommonApi.hasAnyPermissions(operatorUserId, "crm:customer:distribute")
                && resolveOwnerReadScope(operatorUserId).allows(ownerUserId);
    }

    private static boolean satisfies(Integer actualLevel, Integer requiredLevel) {
        if (CrmPermissionLevelEnum.isOwner(actualLevel)) {
            return true;
        }
        if (CrmPermissionLevelEnum.isWrite(requiredLevel)) {
            return CrmPermissionLevelEnum.isWrite(actualLevel);
        }
        if (CrmPermissionLevelEnum.isRead(requiredLevel)) {
            return CrmPermissionLevelEnum.isRead(actualLevel) || CrmPermissionLevelEnum.isWrite(actualLevel);
        }
        return false;
    }

}
