package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkRequestDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOAWorkRequestMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class BpmOAWorkRequestServiceImplTest {

    @Mock
    private BpmOAWorkRequestMapper mapper;
    @InjectMocks
    private BpmOAWorkRequestServiceImpl service;

    @Test
    void approverWithControllerPermissionCanReadAnotherUsersRequest() {
        BpmOAWorkRequestDO request = new BpmOAWorkRequestDO().setId(7L).setUserId(2L);
        when(mapper.selectById(7L)).thenReturn(request);

        assertSame(request, service.get(1L, 7L));
    }

    @Test
    void missingRequestIsRejected() {
        when(mapper.selectById(8L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.get(1L, 8L));
    }

    @Test
    void approvalStatusRecordsCompletionTime() {
        service.updateStatus(9L, 2);
        ArgumentCaptor<BpmOAWorkRequestDO> captor = ArgumentCaptor.forClass(BpmOAWorkRequestDO.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(9L, captor.getValue().getId());
        assertEquals(2, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getApprovedTime());
    }
}
