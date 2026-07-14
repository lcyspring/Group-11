package com.meession.etm.module.crm.dal.mysql.receivable;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmReceivableMapperTest {

    @Test
    void receivedAmountIncludesOnlyApprovedReceivables() {
        AtomicReference<QueryWrapper<CrmReceivableDO>> captured = new AtomicReference<>();
        CrmReceivableMapper mapper = (CrmReceivableMapper) Proxy.newProxyInstance(
                CrmReceivableMapper.class.getClassLoader(), new Class<?>[]{CrmReceivableMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("selectMaps")) {
                        @SuppressWarnings("unchecked")
                        QueryWrapper<CrmReceivableDO> query = (QueryWrapper<CrmReceivableDO>) args[0];
                        captured.set(query);
                        return Collections.<Map<String, Object>>emptyList();
                    }
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                });

        mapper.selectReceivablePriceMapByContractId(List.of(99L));

        assertTrue(captured.get().getSqlSegment().contains("audit_status ="));
        assertTrue(captured.get().getParamNameValuePairs().containsValue(CrmAuditStatusEnum.APPROVE.getStatus()));
        assertFalse(captured.get().getParamNameValuePairs().containsValue(CrmAuditStatusEnum.DRAFT.getStatus()));
        assertFalse(captured.get().getParamNameValuePairs().containsValue(CrmAuditStatusEnum.PROCESS.getStatus()));
    }

}
