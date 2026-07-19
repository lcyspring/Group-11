package com.meession.etm.module.crm.util;

import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmPermissionUtilsTest {

    @Test
    void missingSceneDefaultsNormalUserToOwnerScope() {
        assertEquals(CrmSceneTypeEnum.OWNER.getType(),
                CrmPermissionUtils.resolveDefaultSceneType(false, false));
    }

    @Test
    void missingSceneKeepsCrmAdminUnrestricted() {
        assertNull(CrmPermissionUtils.resolveDefaultSceneType(false, true));
    }

    @Test
    void explicitSceneIsNotOverridden() {
        assertNull(CrmPermissionUtils.resolveDefaultSceneType(true, false));
    }

    @Test
    void crmAdminBypassesOnlyWhenNoSceneWasSelected() {
        assertTrue(CrmPermissionUtils.shouldBypassPermissionCondition(null, true));
        assertFalse(CrmPermissionUtils.shouldBypassPermissionCondition(CrmSceneTypeEnum.INVOLVED.getType(), true));
        assertFalse(CrmPermissionUtils.shouldBypassPermissionCondition(CrmSceneTypeEnum.OWNER.getType(), true));
        assertFalse(CrmPermissionUtils.shouldBypassPermissionCondition(null, false));
    }
}
