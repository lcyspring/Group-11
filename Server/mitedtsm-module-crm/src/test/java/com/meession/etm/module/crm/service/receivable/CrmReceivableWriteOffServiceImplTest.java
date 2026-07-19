package com.meession.etm.module.crm.service.receivable;

import com.meession.etm.module.crm.controller.admin.receivable.vo.writeoff.CrmReceivableWriteOffCreateReqVO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableWriteOffAmountDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableWriteOffDO;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableWriteOffMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmReceivableWriteOffServiceImplTest {
    @Mock private CrmReceivableMapper receivableMapper;
    @Mock private CrmReceivableWriteOffMapper mapper;
    @InjectMocks private CrmReceivableWriteOffServiceImpl service;

    @Test
    void approvedReceivableCanBeWrittenOffWithinRemainingAmount() {
        when(receivableMapper.selectByIdForUpdate(1L)).thenReturn(receivable(20));
        when(mapper.selectActiveAmount(1L)).thenReturn(new BigDecimal("5"));
        when(mapper.selectByReferenceNo("bank-1")).thenReturn(null);
        service.create(new CrmReceivableWriteOffCreateReqVO().setReceivableId(1L).setAmount(new BigDecimal("15"))
                .setWriteOffTime(LocalDateTime.now()).setSourceType(2).setReferenceNo("bank-1"));
        verify(mapper).insert(any(CrmReceivableWriteOffDO.class));
    }

    @Test
    void rejectsUnapprovedAndOverAmount() {
        when(receivableMapper.selectByIdForUpdate(1L)).thenReturn(receivable(20).setAuditStatus(0));
        assertThrows(RuntimeException.class, () -> service.create(request(1, "1")));
        when(receivableMapper.selectByIdForUpdate(1L)).thenReturn(receivable(20));
        when(mapper.selectActiveAmount(1L)).thenReturn(new BigDecimal("19"));
        assertThrows(RuntimeException.class, () -> service.create(request(2, "2")));
    }

    @Test
    void reverseKeepsRecordAndRejectsSecondReverse() {
        CrmReceivableWriteOffDO row = new CrmReceivableWriteOffDO().setId(9L).setReceivableId(1L).setStatus(0);
        when(receivableMapper.selectByIdForUpdate(1L)).thenReturn(receivable(20));
        when(mapper.selectByIdForUpdate(9L)).thenReturn(row);
        service.reverse(1L, 9L);
        verify(mapper).updateById(any(CrmReceivableWriteOffDO.class));
        row.setStatus(10);
        assertThrows(RuntimeException.class, () -> service.reverse(1L, 9L));
    }

    @Test
    void trimsBlankReferenceAndMapsConcurrentDuplicateToBusinessError() {
        when(receivableMapper.selectByIdForUpdate(1L)).thenReturn(receivable(20));
        when(mapper.selectActiveAmount(1L)).thenReturn(BigDecimal.ZERO);
        when(mapper.selectByReferenceNo("bank-1")).thenReturn(null);
        doThrow(new org.springframework.dao.DuplicateKeyException("uk_reference_tenant"))
                .when(mapper).insert(any(CrmReceivableWriteOffDO.class));
        assertThrows(RuntimeException.class, () -> service.create(request(1, "  bank-1  ")));
        verify(mapper).selectByReferenceNo("bank-1");
    }

    @Test
    void aggregatesActiveAmountsForReceivableList() {
        when(mapper.selectActiveAmounts(List.of(1L, 2L))).thenReturn(List.of(
                new CrmReceivableWriteOffAmountDO().setReceivableId(1L).setAmount(new BigDecimal("1.20")),
                new CrmReceivableWriteOffAmountDO().setReceivableId(2L).setAmount(new BigDecimal("2.30"))));
        Map<Long, BigDecimal> result = service.getWrittenOffAmountMap(List.of(1L, 2L));
        assertEquals(new BigDecimal("1.20"), result.get(1L));
        assertEquals(new BigDecimal("2.30"), result.get(2L));
    }

    private static CrmReceivableDO receivable(int amount) {
        return new CrmReceivableDO().setId(1L).setPrice(BigDecimal.valueOf(amount))
                .setAuditStatus(20);
    }
    private static CrmReceivableWriteOffCreateReqVO request(int amount, String ref) {
        return new CrmReceivableWriteOffCreateReqVO().setReceivableId(1L).setAmount(BigDecimal.valueOf(amount))
                .setWriteOffTime(LocalDateTime.now()).setSourceType(1).setReferenceNo(ref);
    }
}
