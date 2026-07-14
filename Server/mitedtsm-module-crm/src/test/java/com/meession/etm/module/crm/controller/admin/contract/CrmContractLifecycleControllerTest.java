package com.meession.etm.module.crm.controller.admin.contract;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractLifecycleRespVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractChangeRecordDO;
import com.meession.etm.module.crm.service.contract.CrmContractLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmContractLifecycleControllerTest {

    @Mock
    private CrmContractLifecycleService service;
    @InjectMocks
    private CrmContractLifecycleController controller;

    @Test
    void getReturnsSummaryWithoutPersistedSnapshots() {
        CrmContractChangeRecordDO record = new CrmContractChangeRecordDO()
                .setId(9L)
                .setContractId(7L)
                .setSequenceNo(1)
                .setContractVersion(1)
                .setActionType(1)
                .setContractSnapshot("{\"name\":\"sensitive contract\"}")
                .setProductSnapshot("[{\"name\":\"sensitive product\"}]");
        when(service.getAttachments(7L)).thenReturn(Collections.emptyList());
        when(service.getChangeRecords(7L)).thenReturn(List.of(record));
        when(service.getSupportedSignMethods()).thenReturn(List.of(1));

        CommonResult<CrmContractLifecycleRespVO> result = controller.get(7L);
        String json = JsonUtils.toJsonString(result);

        assertEquals(List.of(1), result.getData().getSupportedSignMethods());
        assertEquals(1, result.getData().getChangeRecords().size());
        assertFalse(json.contains("contractSnapshot"));
        assertFalse(json.contains("productSnapshot"));
        assertFalse(json.contains("sensitive contract"));
        assertFalse(json.contains("sensitive product"));
    }
}
