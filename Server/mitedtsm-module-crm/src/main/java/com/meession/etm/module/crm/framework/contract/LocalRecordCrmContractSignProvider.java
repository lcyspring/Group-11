package com.meession.etm.module.crm.framework.contract;

import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.SIGN_OFFLINE;

@Component
@ConditionalOnProperty(prefix = "mitedtsm.crm.contract-sign", name = "provider", havingValue = "local-record")
public class LocalRecordCrmContractSignProvider implements CrmContractSignProvider {

    @Override
    public Set<Integer> getSupportedMethods() {
        return Set.of(SIGN_OFFLINE);
    }

    @Override
    public Result sign(CrmContractDO contract, String requestId) {
        return new Result("local-record", requestId, null);
    }

    @Override
    public Result voidSign(CrmContractDO contract, String requestId) {
        return new Result("local-record", requestId, null);
    }
}
