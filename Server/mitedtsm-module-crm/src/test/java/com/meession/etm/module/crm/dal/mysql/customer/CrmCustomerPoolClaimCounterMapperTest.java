package com.meession.etm.module.crm.dal.mysql.customer;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmCustomerPoolClaimCounterMapperTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 7L;
    private static final LocalDate CLAIM_DATE = LocalDate.of(2026, 7, 15);

    @Test
    void reserveUpdatesExistingCounterWithinLimit() {
        CrmCustomerPoolClaimCounterMapper mapper = mapper();
        when(mapper.incrementExisting(TENANT_ID, USER_ID, CLAIM_DATE, 2, 10)).thenReturn(1);

        assertEquals(1, mapper.reserve(TENANT_ID, USER_ID, CLAIM_DATE, 2, 10));
        verify(mapper, never()).insertInitial(TENANT_ID, USER_ID, CLAIM_DATE, 2);
    }

    @Test
    void reserveInsertsFirstCounter() {
        CrmCustomerPoolClaimCounterMapper mapper = mapper();
        when(mapper.incrementExisting(TENANT_ID, USER_ID, CLAIM_DATE, 2, 10)).thenReturn(0);
        when(mapper.insertInitial(TENANT_ID, USER_ID, CLAIM_DATE, 2)).thenReturn(1);

        assertEquals(1, mapper.reserve(TENANT_ID, USER_ID, CLAIM_DATE, 2, 10));
    }

    @Test
    void reserveRetriesGuardedUpdateAfterConcurrentFirstInsert() {
        CrmCustomerPoolClaimCounterMapper mapper = mapper();
        when(mapper.incrementExisting(TENANT_ID, USER_ID, CLAIM_DATE, 2, 10)).thenReturn(0, 1);
        when(mapper.insertInitial(TENANT_ID, USER_ID, CLAIM_DATE, 2)).thenReturn(0);

        assertEquals(1, mapper.reserve(TENANT_ID, USER_ID, CLAIM_DATE, 2, 10));
        verify(mapper).insertInitial(TENANT_ID, USER_ID, CLAIM_DATE, 2);
    }

    @Test
    void reserveRejectsWhenGuardedUpdateAndInsertCannotReserve() {
        CrmCustomerPoolClaimCounterMapper mapper = mapper();
        when(mapper.incrementExisting(TENANT_ID, USER_ID, CLAIM_DATE, 2, 10)).thenReturn(0);
        when(mapper.insertInitial(TENANT_ID, USER_ID, CLAIM_DATE, 2)).thenReturn(0);

        assertEquals(0, mapper.reserve(TENANT_ID, USER_ID, CLAIM_DATE, 2, 10));
    }

    @Test
    void reserveRejectsInvalidIncrementBeforeDatabaseAccess() {
        CrmCustomerPoolClaimCounterMapper mapper = mapper();

        assertEquals(0, mapper.reserve(TENANT_ID, USER_ID, CLAIM_DATE, 0, 10));
        assertEquals(0, mapper.reserve(TENANT_ID, USER_ID, CLAIM_DATE, 11, 10));
        verify(mapper, never()).incrementExisting(TENANT_ID, USER_ID, CLAIM_DATE, 0, 10);
        verify(mapper, never()).insertInitial(TENANT_ID, USER_ID, CLAIM_DATE, 0);
    }

    private static CrmCustomerPoolClaimCounterMapper mapper() {
        return mock(CrmCustomerPoolClaimCounterMapper.class, Answers.CALLS_REAL_METHODS);
    }
}
