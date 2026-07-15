package com.meession.etm.module.crm.framework.permission;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.module.crm.controller.admin.statistics.vo.CrmStatisticsScopedReqVO;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CRM_STATISTICS_SCOPE_DENIED;

/** Validates untrusted department and owner filters used by CRM statistics endpoints. */
@Service
@Slf4j
public class CrmStatisticsDataScopeService {

    @Resource
    private CrmAuthorizationService authorizationService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    public void validate(Long loginUserId, CrmStatisticsScopedReqVO reqVO) {
        if (loginUserId == null || reqVO == null || reqVO.getDeptId() == null) {
            throw exception(CRM_STATISTICS_SCOPE_DENIED);
        }

        ResolvedSelection selection;
        try {
            selection = resolveSelection(loginUserId, reqVO.getDeptId());
        } catch (RuntimeException ex) {
            // IAM / organization integration failures must never degrade into an unrestricted query.
            log.warn("[validate][userId({}) deptId({}) CRM 统计范围解析失败]",
                    loginUserId, reqVO.getDeptId(), ex);
            throw exception(CRM_STATISTICS_SCOPE_DENIED);
        }

        Long selectedUserId = reqVO.getUserId();
        if (selectedUserId != null && !selection.departmentUserIds().contains(selectedUserId)) {
            throw exception(CRM_STATISTICS_SCOPE_DENIED);
        }
        if (selection.readScope().all()) {
            return;
        }
        if (selectedUserId != null) {
            if (!selection.readScope().allows(selectedUserId)) {
                throw exception(CRM_STATISTICS_SCOPE_DENIED);
            }
            return;
        }
        if (!selection.readScope().ownerUserIds().containsAll(selection.departmentUserIds())) {
            throw exception(CRM_STATISTICS_SCOPE_DENIED);
        }
    }

    private ResolvedSelection resolveSelection(Long loginUserId, Long deptId) {
        if (deptApi.getDept(deptId) == null) {
            throw new IllegalArgumentException("department does not exist");
        }
        Set<Long> deptIds = convertSet(CollUtil.emptyIfNull(deptApi.getChildDeptList(deptId)), DeptRespDTO::getId);
        deptIds.add(deptId);
        Set<Long> departmentUserIds = convertSet(
                CollUtil.emptyIfNull(adminUserApi.getUserListByDeptIds(deptIds)), AdminUserRespDTO::getId);
        CrmOwnerReadScope readScope = authorizationService.resolveOwnerReadScope(loginUserId);
        return new ResolvedSelection(readScope, new HashSet<>(departmentUserIds));
    }

    private record ResolvedSelection(CrmOwnerReadScope readScope, Set<Long> departmentUserIds) {
    }

}
