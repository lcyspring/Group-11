package com.meession.etm.module.crm.service.permission;

import com.meession.etm.framework.common.biz.system.permission.PermissionCommonApi;
import com.meession.etm.module.crm.controller.admin.permission.vo.CrmPermissionSaveReqVO;
import com.meession.etm.module.crm.controller.admin.permission.vo.CrmPermissionUpdateReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.permission.CrmPermissionDO;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.dal.mysql.permission.CrmPermissionMapper;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CLUE_UPDATE_FAIL_TRANSFORMED;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CRM_PERMISSION_NOT_EXISTS;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CRM_EXPORT_PERMISSION_DENIED;

class CrmPermissionServiceImplTest {

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
    void exportAllowsSubordinateOwnerButRejectsUnscopedObject() {
        CrmPermissionServiceImpl service = exportService(List.of(new CrmPermissionDO().setBizId(10L)
                .setUserId(3L).setLevel(CrmPermissionLevelEnum.OWNER.getLevel())),
                List.of(new AdminUserRespDTO().setId(3L)));

        service.validateExportPermission(CrmBizTypeEnum.CRM_CUSTOMER.getType(), List.of(10L), 2L);
        assertServiceException(() -> service.validateExportPermission(
                CrmBizTypeEnum.CRM_CUSTOMER.getType(), List.of(10L, 11L), 2L), CRM_EXPORT_PERMISSION_DENIED, "客户");
    }

    private static CrmPermissionServiceImpl exportService(List<CrmPermissionDO> permissions,
                                                          List<AdminUserRespDTO> subordinates) {
        CrmPermissionServiceImpl service = new CrmPermissionServiceImpl();
        ReflectionTestUtils.setField(service, "permissionCommonApi", proxy(PermissionCommonApi.class,
                (proxy, method, args) -> false));
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> method.getName().equals("getUserListBySubordinate")
                        ? subordinates : null));
        ReflectionTestUtils.setField(service, "permissionMapper", proxy(CrmPermissionMapper.class,
                (proxy, method, args) -> method.getName().equals("selectByBizTypeAndBizIds")
                        ? permissions : null));
        return service;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
