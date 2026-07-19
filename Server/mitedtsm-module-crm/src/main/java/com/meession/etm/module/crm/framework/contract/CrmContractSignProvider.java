package com.meession.etm.module.crm.framework.contract;

import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;

import java.util.Set;

public interface CrmContractSignProvider {

    record Result(String providerCode, String requestId, String externalSigningId) {
    }

    Set<Integer> getSupportedMethods();

    Result sign(CrmContractDO contract, String requestId);

    Result voidSign(CrmContractDO contract, String requestId);
}
