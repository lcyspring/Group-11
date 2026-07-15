package com.meession.etm.module.crm.framework.permission.core.aop;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.enums.clue.CrmCluePoolStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolStatusEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_GARBAGE_ADMIN_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmPermissionAspectGarbageTest {

    @Mock
    private CrmAuthorizationService authorizationService;
    @Mock
    private CrmCustomerMapper customerMapper;
    @Mock
    private CrmClueMapper clueMapper;

    private CrmPermissionAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new CrmPermissionAspect();
        ReflectionTestUtils.setField(aspect, "crmAuthorizationService", authorizationService);
        ReflectionTestUtils.setField(aspect, "crmCustomerMapper", customerMapper);
        ReflectionTestUtils.setField(aspect, "crmClueMapper", clueMapper);
    }

    @Test
    void noTeamPermissionDoesNotExposeGarbageCustomer() {
        when(customerMapper.selectById(20L)).thenReturn(new CrmCustomerDO().setId(20L)
                .setPoolStatus(CrmCustomerPoolStatusEnum.GARBAGE.getStatus()));

        ServiceException exception = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                aspect, "validatePermission", CrmBizTypeEnum.CRM_CUSTOMER.getType(), 20L,
                Collections.emptyList(), CrmPermissionLevelEnum.READ.getLevel()));

        assertEquals(CUSTOMER_GARBAGE_ADMIN_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void explicitPublicCustomerRemainsReadableWithoutTeamPermission() {
        when(customerMapper.selectById(20L)).thenReturn(new CrmCustomerDO().setId(20L)
                .setPoolStatus(CrmCustomerPoolStatusEnum.PUBLIC.getStatus()));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(aspect, "validatePermission",
                CrmBizTypeEnum.CRM_CUSTOMER.getType(), 20L, Collections.emptyList(),
                CrmPermissionLevelEnum.READ.getLevel()));
    }

    @Test
    void explicitPublicClueIsReadableWithoutTeamPermission() {
        when(clueMapper.selectById(30L)).thenReturn(new CrmClueDO().setId(30L)
                .setPoolStatus(CrmCluePoolStatusEnum.PUBLIC.getStatus())
                .setTransformStatus(false).setOwnerUserId(null));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(aspect, "validatePermission",
                CrmBizTypeEnum.CRM_CLUE.getType(), 30L, Collections.emptyList(),
                CrmPermissionLevelEnum.READ.getLevel()));
    }

    @Test
    void orphanOwnedClueIsNotMistakenForPublicClue() {
        when(clueMapper.selectById(30L)).thenReturn(new CrmClueDO().setId(30L)
                .setPoolStatus(CrmCluePoolStatusEnum.OWNED.getStatus())
                .setTransformStatus(false).setOwnerUserId(null));

        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(aspect, "validatePermission",
                CrmBizTypeEnum.CRM_CLUE.getType(), 30L, Collections.emptyList(),
                CrmPermissionLevelEnum.READ.getLevel()));
    }
}
