package com.meession.etm.module.crm.job.exporttask;

import com.meession.etm.module.crm.service.exporttask.CrmExportTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmExportTaskJobTest {

    @Mock private CrmExportTaskService service;
    @InjectMocks private CrmExportTaskJob job;

    @Test
    void reportsProcessedTenantBatchCount() {
        when(service.processTenantBatch()).thenReturn(3);

        assertEquals("CRM export tasks processed=3", job.execute(null));
    }
}
