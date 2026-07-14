package com.meession.etm.module.crm.framework.contract;

import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.SIGN_OFFLINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalRecordCrmContractSignProviderTest {

    private final LocalRecordCrmContractSignProvider provider = new LocalRecordCrmContractSignProvider();

    @Test
    void exposesOnlyOfflineCapability() {
        assertEquals(Set.of(SIGN_OFFLINE), provider.getSupportedMethods());
    }

    @Test
    void recordsStableLocalRequestWithoutInventingExternalId() {
        CrmContractSignProvider.Result result = provider.sign(new CrmContractDO().setId(7L), "request-7");

        assertEquals("local-record", result.providerCode());
        assertEquals("request-7", result.requestId());
        assertNull(result.externalSigningId());
    }
}
