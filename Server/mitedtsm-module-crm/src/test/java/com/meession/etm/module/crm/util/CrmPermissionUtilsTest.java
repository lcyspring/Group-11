package com.meession.etm.module.crm.util;

import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
