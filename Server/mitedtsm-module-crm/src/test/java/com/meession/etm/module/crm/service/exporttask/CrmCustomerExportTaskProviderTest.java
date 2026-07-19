package com.meession.etm.module.crm.service.exporttask;

import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.service.customer.CrmCustomerResponseAssembler;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.EXPORT_TASK_OBJECT_CHANGED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmCustomerExportTaskProviderTest {

    @Mock private CrmCustomerService customerService;
    @Mock private CrmCustomerResponseAssembler responseAssembler;
    @InjectMocks private CrmCustomerExportTaskProvider provider;

    @Test
    void generatePreservesFrozenObjectOrderAndBuildsWorkbookInMemory() {
        CrmCustomerDO first = new CrmCustomerDO().setId(11L).setName("First");
        CrmCustomerDO second = new CrmCustomerDO().setId(13L).setName("Second");
        when(customerService.getCustomerList(List.of(13L, 11L))).thenReturn(List.of(first, second));
        CrmCustomerRespVO firstRow = new CrmCustomerRespVO();
        firstRow.setId(13L);
        CrmCustomerRespVO secondRow = new CrmCustomerRespVO();
        secondRow.setId(11L);
        when(responseAssembler.buildDetailList(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(firstRow, secondRow));

        CrmExportTaskProvider.ExportFile file = provider.generate(List.of(13L, 11L), 7L);

        ArgumentCaptor<List<CrmCustomerDO>> ordered = ArgumentCaptor.forClass(List.class);
        verify(responseAssembler).buildDetailList(ordered.capture());
        assertEquals(List.of(13L, 11L), ordered.getValue().stream().map(CrmCustomerDO::getId).toList());
        assertEquals("客户导出.xlsx", file.fileName());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.contentType());
        assertTrue(file.content().length > 100);
        assertArrayEquals(new byte[]{'P', 'K'}, new byte[]{file.content()[0], file.content()[1]});
    }

    @Test
    void validationRejectsDeletedObject() {
        when(customerService.getCustomerList(List.of(11L, 13L)))
                .thenReturn(List.of(new CrmCustomerDO().setId(11L)));

        assertServiceException(() -> provider.validateObjects(List.of(11L, 13L)), EXPORT_TASK_OBJECT_CHANGED);
    }

    @Test
    void generationRejectsDuplicateOrMissingSnapshotObject() {
        CrmCustomerDO duplicate = new CrmCustomerDO().setId(11L);
        when(customerService.getCustomerList(List.of(11L, 13L))).thenReturn(List.of(duplicate, duplicate));

        assertServiceException(() -> provider.generate(List.of(11L, 13L), 7L), EXPORT_TASK_OBJECT_CHANGED);
    }
}
