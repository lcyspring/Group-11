package com.meession.etm.module.crm.framework.permission;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceReqVO;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CRM_STATISTICS_SCOPE_DENIED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmStatisticsDataScopeServiceTest {

    @Mock
    private CrmAuthorizationService authorizationService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;
    @InjectMocks
    private CrmStatisticsDataScopeService service;

    @BeforeEach
    void setUpOrganizationTree() {
        lenient().when(deptApi.getDept(10L)).thenReturn(new DeptRespDTO().setId(10L));
        lenient().when(deptApi.getChildDeptList(10L)).thenReturn(List.of(new DeptRespDTO().setId(11L)));
        lenient().when(adminUserApi.getUserListByDeptIds(anyCollection())).thenReturn(List.of(user(7L), user(8L)));
    }

    @Test
    void allScopeCanQuerySelectedDepartment() {
        when(authorizationService.resolveOwnerReadScope(1L)).thenReturn(new CrmOwnerReadScope(true, Set.of()));

        assertDoesNotThrow(() -> service.validate(1L, request(null)));
    }

    @Test
    void selfScopeCanQuerySelf() {
        when(authorizationService.resolveOwnerReadScope(7L)).thenReturn(new CrmOwnerReadScope(false, Set.of(7L)));

        assertDoesNotThrow(() -> service.validate(7L, request(7L)));
    }

    @Test
    void selfScopeCannotQueryAnotherUser() {
        when(authorizationService.resolveOwnerReadScope(7L)).thenReturn(new CrmOwnerReadScope(false, Set.of(7L)));

        assertDenied(() -> service.validate(7L, request(8L)));
    }

    @Test
    void departmentScopeCanQueryFullyCoveredDepartmentTree() {
        when(authorizationService.resolveOwnerReadScope(7L))
                .thenReturn(new CrmOwnerReadScope(false, Set.of(7L, 8L)));

        assertDoesNotThrow(() -> service.validate(7L, request(null)));
    }

    @Test
    void parentDepartmentCannotIncludeUnauthorizedChildUsers() {
        when(authorizationService.resolveOwnerReadScope(7L)).thenReturn(new CrmOwnerReadScope(false, Set.of(7L)));

        assertDenied(() -> service.validate(7L, request(null)));
    }

    @Test
    void selectedUserMustBelongToSelectedDepartmentTreeEvenForAllScope() {
        when(authorizationService.resolveOwnerReadScope(1L)).thenReturn(new CrmOwnerReadScope(true, Set.of()));

        assertDenied(() -> service.validate(1L, request(99L)));
    }

    @Test
    void emptyDepartmentCanReturnEmptyStatistics() {
        when(adminUserApi.getUserListByDeptIds(anyCollection())).thenReturn(Collections.emptyList());
        when(authorizationService.resolveOwnerReadScope(7L)).thenReturn(new CrmOwnerReadScope(false, Set.of(7L)));

        assertDoesNotThrow(() -> service.validate(7L, request(null)));
    }

    @Test
    void organizationApiFailureFailsClosed() {
        when(deptApi.getChildDeptList(10L)).thenThrow(new IllegalStateException("IAM unavailable"));

        assertDenied(() -> service.validate(7L, request(null)));
    }

    @Test
    void missingLoginUserFailsClosed() {
        assertDenied(() -> service.validate(null, request(null)));
    }

    private static CrmStatisticsPerformanceReqVO request(Long userId) {
        return new CrmStatisticsPerformanceReqVO().setDeptId(10L).setUserId(userId);
    }

    private static AdminUserRespDTO user(Long id) {
        return new AdminUserRespDTO().setId(id);
    }

    private static void assertDenied(org.junit.jupiter.api.function.Executable executable) {
        ServiceException ex = assertThrows(ServiceException.class, executable);
        assertEquals(CRM_STATISTICS_SCOPE_DENIED.getCode(), ex.getCode());
    }

}
