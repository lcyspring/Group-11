package com.meession.etm.module.crm.service.clue;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueSaveReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueTransferReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.publicpool.CrmCluePublicPutReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueOwnerRecordDO;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueOwnerRecordMapper;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueOwnerCapacityGuardMapper;
import com.meession.etm.module.crm.dal.mysql.clue.CrmCluePoolClaimCounterMapper;
import com.meession.etm.module.crm.enums.clue.CrmClueOwnerRecordSourceEnum;
import com.meession.etm.module.crm.enums.clue.CrmClueOwnerRecordTypeEnum;
import com.meession.etm.module.crm.enums.clue.CrmCluePoolStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.framework.pool.CrmPoolPolicyProperties;
import com.meession.etm.module.crm.framework.pool.CrmPoolTimeProvider;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmCluePublicPoolServiceImplTest {

    @Mock private CrmClueMapper clueMapper;
    @Mock private CrmClueOwnerRecordMapper clueOwnerRecordMapper;
    @Mock private CrmClueOwnerCapacityGuardMapper ownerCapacityGuardMapper;
    @Mock private CrmCluePoolClaimCounterMapper claimCounterMapper;
    @Mock private CrmPermissionService permissionService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private CrmPoolPolicyProperties poolPolicyProperties;
    @Mock private CrmPoolTimeProvider poolTimeProvider;
    @InjectMocks private CrmCluePublicPoolServiceImpl service;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 14, 0);
    private CrmPoolPolicyProperties.Clue policy;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        policy = new CrmPoolPolicyProperties.Clue();
        policy.setEnabled(true);
        policy.setContactExpireDays(30);
        policy.setMaxOwnedClues(100);
        policy.setDailyClaimLimit(10);
        policy.setRepeatClaimCooldownDays(30);
        policy.setClaimBatchLimit(100);
        policy.setAutoPoolBatchSize(500);
        policy.setAutoPoolMaxBatchSize(5000);
        policy.setAutoPoolMaxBatches(20);
        lenient().when(poolPolicyProperties.getClue()).thenReturn(policy);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void manualPutClearsAllPermissionsAndRecordsTrimmedReason() {
        CrmClueDO clue = ownedClue(20L, 7L);
        when(clueMapper.selectByIdForUpdate(20L)).thenReturn(clue);
        when(poolTimeProvider.now()).thenReturn(NOW);
        when(clueMapper.updateToPublicPool(20L, 7L, NOW,
                CrmClueOwnerRecordSourceEnum.MANUAL_PUT_POOL.getSource(), "暂不跟进")).thenReturn(1);

        service.putCluePublic(new CrmCluePublicPutReqVO().setClueId(20L).setReason("  暂不跟进  "), 7L);

        verify(permissionService).deletePermissionIfPresent(CrmBizTypeEnum.CRM_CLUE.getType(), 20L);
        ArgumentCaptor<CrmClueOwnerRecordDO> record = ArgumentCaptor.forClass(CrmClueOwnerRecordDO.class);
        verify(clueOwnerRecordMapper).insert(record.capture());
        assertEquals(CrmClueOwnerRecordTypeEnum.PUT_POOL.getType(), record.getValue().getType());
        assertEquals("暂不跟进", record.getValue().getReason());
    }

    @Test
    void selfClaimReservesQuotaAndRestoresOwnerPermission() {
        when(clueMapper.selectByIdForUpdate(20L)).thenReturn(publicClue(20L));
        when(clueMapper.selectOwnedCountByUserId(7L)).thenReturn(3L);
        when(poolTimeProvider.now()).thenReturn(NOW);
        when(claimCounterMapper.reserve(9L, 7L, NOW.toLocalDate(), 1, 10)).thenReturn(1);
        when(clueMapper.updateClaimedFromPublicPool(20L, 7L, NOW)).thenReturn(1);

        service.claimPublicClues(List.of(20L), 7L);

        verify(ownerCapacityGuardMapper).lockOwnerCapacity(9L, 7L);
        verify(permissionService).replaceOwnerPermission(CrmBizTypeEnum.CRM_CLUE.getType(), 20L, 7L);
        ArgumentCaptor<CrmClueOwnerRecordDO> record = ArgumentCaptor.forClass(CrmClueOwnerRecordDO.class);
        verify(clueOwnerRecordMapper).insert(record.capture());
        assertEquals(CrmClueOwnerRecordSourceEnum.SELF_CLAIM.getSource(), record.getValue().getSource());
    }

    @Test
    void repeatSelfClaimCooldownRejectsBeforeQuotaReservation() {
        when(clueMapper.selectByIdForUpdate(20L)).thenReturn(publicClue(20L));
        when(clueMapper.selectOwnedCountByUserId(7L)).thenReturn(0L);
        when(poolTimeProvider.now()).thenReturn(NOW);
        when(clueOwnerRecordMapper.existsRecentSelfClaim(20L, 7L, NOW.minusDays(30))).thenReturn(true);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.claimPublicClues(List.of(20L), 7L));

        assertEquals(CLUE_PUBLIC_REPEAT_CLAIM_COOLDOWN.getCode(), exception.getCode());
        verify(claimCounterMapper, never()).reserve(anyLong(), anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    void dailyQuotaRejectsWithoutChangingClue() {
        when(clueMapper.selectByIdForUpdate(20L)).thenReturn(publicClue(20L));
        when(clueMapper.selectOwnedCountByUserId(7L)).thenReturn(0L);
        when(poolTimeProvider.now()).thenReturn(NOW);
        when(claimCounterMapper.reserve(9L, 7L, NOW.toLocalDate(), 1, 10)).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.claimPublicClues(List.of(20L), 7L));

        assertEquals(CLUE_PUBLIC_DAILY_CLAIM_LIMIT.getCode(), exception.getCode());
        verify(clueMapper, never()).updateClaimedFromPublicPool(anyLong(), anyLong(), any());
    }

    @Test
    void managerAssignmentBypassesSelfClaimQuotaAndCooldown() {
        when(clueMapper.selectByIdForUpdate(20L)).thenReturn(publicClue(20L));
        when(clueMapper.selectOwnedCountByUserId(8L)).thenReturn(0L);
        when(poolTimeProvider.now()).thenReturn(NOW);
        when(clueMapper.updateClaimedFromPublicPool(20L, 8L, NOW)).thenReturn(1);

        service.assignPublicClues(List.of(20L), 8L, 99L);

        verify(claimCounterMapper, never()).reserve(anyLong(), anyLong(), any(), anyInt(), anyInt());
        verify(clueOwnerRecordMapper, never()).existsRecentSelfClaim(anyLong(), anyLong(), any());
        ArgumentCaptor<CrmClueOwnerRecordDO> record = ArgumentCaptor.forClass(CrmClueOwnerRecordDO.class);
        verify(clueOwnerRecordMapper).insert(record.capture());
        assertEquals(CrmClueOwnerRecordSourceEnum.MANAGER_ASSIGN.getSource(), record.getValue().getSource());
    }

    @Test
    void ownershipLimitProtectsClaimsAndAssignments() {
        when(clueMapper.selectByIdForUpdate(20L)).thenReturn(publicClue(20L));
        when(clueMapper.selectOwnedCountByUserId(7L)).thenReturn(100L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.assignPublicClues(List.of(20L), 7L, 99L));

        assertEquals(CLUE_OWNER_LIMIT_EXCEEDED.getCode(), exception.getCode());
        verify(clueMapper, never()).updateClaimedFromPublicPool(anyLong(), anyLong(), any());
    }

    @Test
    void claimBatchLimitComesFromYamlPolicy() {
        policy.setClaimBatchLimit(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.claimPublicClues(List.of(20L, 21L), 7L));

        assertEquals(CLUE_PUBLIC_BATCH_LIMIT.getCode(), exception.getCode());
        verify(clueMapper, never()).selectByIdForUpdate(anyLong());
    }

    @Test
    void capacityIncreasingCommandsUseReadCommittedIsolation() throws NoSuchMethodException {
        assertReadCommitted(CrmClueServiceImpl.class, "createClue", CrmClueSaveReqVO.class);
        assertReadCommitted(CrmClueServiceImpl.class, "transferClue", CrmClueTransferReqVO.class, Long.class);
        assertReadCommitted(CrmCluePublicPoolServiceImpl.class, "claimPublicClues", List.class, Long.class);
        assertReadCommitted(CrmCluePublicPoolServiceImpl.class, "assignPublicClues", List.class, Long.class,
                Long.class);
    }

    @Test
    void batchOperationLogUsesScalarBusinessNumber() throws NoSuchMethodException {
        assertEquals("{{#primaryClueId}}", CrmCluePublicPoolServiceImpl.class
                .getMethod("claimPublicClues", List.class, Long.class)
                .getAnnotation(LogRecord.class).bizNo());
        assertEquals("{{#primaryClueId}}", CrmCluePublicPoolServiceImpl.class
                .getMethod("assignPublicClues", List.class, Long.class, Long.class)
                .getAnnotation(LogRecord.class).bizNo());
    }

    @Test
    void autoPutRechecksExpiryAndTransformStateUnderLock() {
        CrmClueDO clue = ownedClue(20L, 7L).setOwnerTime(NOW.minusDays(40));
        when(clueMapper.selectByIdForUpdate(20L)).thenReturn(clue);
        when(clueMapper.updateToPublicPool(20L, 7L, NOW,
                CrmClueOwnerRecordSourceEnum.AUTO_NO_FOLLOW_UP.getSource(), "超过配置天数未跟进")).thenReturn(1);

        boolean changed = service.autoPutSingleClue(20L, NOW.minusDays(30), NOW);

        assertTrue(changed);
        verify(permissionService).deletePermissionIfPresent(CrmBizTypeEnum.CRM_CLUE.getType(), 20L);
    }

    @Test
    void transformedClueNeverEntersPublicPool() {
        when(clueMapper.selectByIdForUpdate(20L)).thenReturn(ownedClue(20L, 7L).setTransformStatus(true));

        boolean changed = service.autoPutSingleClue(20L, NOW.minusDays(30), NOW);

        assertFalse(changed);
        verify(clueMapper, never()).updateToPublicPool(anyLong(), anyLong(), any(), any(), any());
    }

    private static CrmClueDO publicClue(Long id) {
        return new CrmClueDO().setId(id).setName("公共线索-" + id).setTransformStatus(false)
                .setPoolStatus(CrmCluePoolStatusEnum.PUBLIC.getStatus()).setOwnerUserId(null);
    }

    private static CrmClueDO ownedClue(Long id, Long ownerUserId) {
        return new CrmClueDO().setId(id).setName("在管线索-" + id).setTransformStatus(false)
                .setPoolStatus(CrmCluePoolStatusEnum.OWNED.getStatus()).setOwnerUserId(ownerUserId);
    }

    private static void assertReadCommitted(Class<?> type, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Transactional transactional = type.getMethod(methodName, parameterTypes).getAnnotation(Transactional.class);
        assertEquals(Isolation.READ_COMMITTED, transactional.isolation());
    }
}
