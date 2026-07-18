package com.meession.etm.module.crm.service.exporttask;

import cn.hutool.crypto.digest.DigestUtil;
import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.exporttask.CrmExportTaskDO;
import com.meession.etm.module.crm.dal.mysql.exporttask.CrmExportTaskMapper;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.framework.exporttask.CrmExportTaskProperties;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.infra.api.file.FileApi;
import com.meession.etm.module.infra.api.file.dto.FileRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.exporttask.CrmExportObjectType.CUSTOMER;
import static com.meession.etm.module.crm.enums.exporttask.CrmExportTaskStatusEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmExportTaskServiceImplTest {

    @Mock private CrmExportTaskMapper taskMapper;
    @Mock private CrmCustomerService customerService;
    @Mock private CrmPermissionService permissionService;
    @Mock private FileApi fileApi;
    @Mock private CrmExportTaskProvider provider;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock lock;

    private CrmExportTaskProperties properties;
    private CrmExportTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        properties = new CrmExportTaskProperties();
        properties.setEnabled(true);
        properties.setBatchSize(10);
        properties.setMaxBatchSize(100);
        properties.setMaxPendingPerUser(3);
        properties.setMaxRows(5000);
        properties.setRetentionHours(24);
        properties.setTokenTtlSeconds(300);
        service = new CrmExportTaskServiceImpl(taskMapper, customerService, permissionService, properties,
                fileApi, List.of(provider), redissonClient);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
        lenient().when(lock.tryLock()).thenReturn(true);
        lenient().when(lock.isHeldByCurrentThread()).thenReturn(true);
        lenient().when(provider.objectType()).thenReturn(CUSTOMER);
        lenient().when(provider.bizType()).thenReturn(CrmBizTypeEnum.CRM_CUSTOMER.getType());
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void createFreezesFilterAndAuthorizedObjectIdsWithoutGeneratingFile() {
        CrmCustomerPageReqVO filter = new CrmCustomerPageReqVO();
        filter.setName("Acme");
        filter.setPageNo(4);
        filter.setPageSize(20);
        List<CrmCustomerDO> customers = List.of(customer(11L), customer(13L));
        when(taskMapper.selectActiveCount(7L, List.of(QUEUED.getStatus(), RUNNING.getStatus()))).thenReturn(1L);
        when(customerService.getCustomerPage(filter, 7L)).thenReturn(new PageResult<>(customers, 2L));
        doAnswer(invocation -> {
            invocation.<CrmExportTaskDO>getArgument(0).setId(101L);
            return 1;
        }).when(taskMapper).insert(any(CrmExportTaskDO.class));

        assertEquals(101L, service.createCustomerTask(filter, 7L));

        ArgumentCaptor<CrmExportTaskDO> taskCaptor = ArgumentCaptor.forClass(CrmExportTaskDO.class);
        verify(taskMapper).insert(taskCaptor.capture());
        CrmExportTaskDO saved = taskCaptor.getValue();
        assertEquals(CUSTOMER, saved.getObjectType());
        assertEquals(7L, saved.getCreatorUserId());
        assertEquals(QUEUED.getStatus(), saved.getStatus());
        assertEquals(2, saved.getTotalCount());
        assertEquals(List.of(11L, 13L), JsonUtils.parseArray(saved.getObjectIdsSnapshot(), Long.class));
        CrmCustomerPageReqVO frozenFilter = JsonUtils.parseObject(saved.getFilterSnapshot(), CrmCustomerPageReqVO.class);
        assertEquals("Acme", frozenFilter.getName());
        assertEquals(1, frozenFilter.getPageNo());
        assertEquals(-1, frozenFilter.getPageSize());
        verify(permissionService).validateExportPermission(
                CrmBizTypeEnum.CRM_CUSTOMER.getType(), List.of(11L, 13L), 7L);
        verify(provider, never()).generate(anyList(), anyLong());
        verify(fileApi, never()).createFile(any(), any(), any(), any());
        verify(lock).unlock();
    }

    @Test
    void createRejectsUserCapacityBeforeQueryingCustomers() {
        when(taskMapper.selectActiveCount(7L, List.of(QUEUED.getStatus(), RUNNING.getStatus()))).thenReturn(3L);

        assertServiceException(() -> service.createCustomerTask(new CrmCustomerPageReqVO(), 7L),
                EXPORT_TASK_CAPACITY_EXCEEDED, 3);

        verify(customerService, never()).getCustomerPage(any(), anyLong());
        verify(taskMapper, never()).insert(any(CrmExportTaskDO.class));
    }

    @Test
    void createRejectsConfiguredRowLimit() {
        properties.setMaxRows(1);
        when(customerService.getCustomerPage(any(), eq(7L)))
                .thenReturn(new PageResult<>(List.of(customer(11L), customer(13L)), 2L));

        assertServiceException(() -> service.createCustomerTask(new CrmCustomerPageReqVO(), 7L),
                EXPORT_TASK_ROW_LIMIT, 2, 1);

        verify(permissionService, never()).validateExportPermission(anyInt(), anyCollection(), anyLong());
        verify(taskMapper, never()).insert(any(CrmExportTaskDO.class));
    }

    @Test
    void taskCannotBeReadByAnotherUser() {
        when(taskMapper.selectById(101L)).thenReturn(task(SUCCESS.getStatus()));

        assertServiceException(() -> service.getTask(101L, 8L), EXPORT_TASK_NOT_EXISTS);
    }

    @Test
    void queuedTaskTransitionsThroughRunningToSuccess() {
        CrmExportTaskDO task = task(RUNNING.getStatus());
        when(taskMapper.selectExpiredList(any(), eq(EXPIRED.getStatus()), eq(100))).thenReturn(List.of());
        when(taskMapper.selectQueuedIds(eq(10), eq(QUEUED.getStatus()), any())).thenReturn(List.of(101L));
        when(taskMapper.transition(eq(101L), eq(QUEUED.getStatus()), eq(RUNNING.getStatus()), any())).thenReturn(1);
        when(taskMapper.selectById(101L)).thenReturn(task);
        when(provider.generate(List.of(11L, 13L), 7L)).thenReturn(
                new CrmExportTaskProvider.ExportFile(new byte[]{1, 2}, "客户导出.xlsx", "application/xlsx"));
        when(fileApi.createFile(any(), eq("客户导出.xlsx"), eq("crm-protected/export/101"),
                eq("application/xlsx"))).thenReturn("http://file/export/101");
        when(taskMapper.markSuccess(eq(101L), eq(RUNNING.getStatus()), eq(SUCCESS.getStatus()),
                eq("http://file/export/101"), eq("客户导出.xlsx"), eq("application/xlsx"), any())).thenReturn(1);

        assertEquals(1, service.processTenantBatch());

        verify(provider).validateObjects(List.of(11L, 13L));
        verify(permissionService).validateExportPermission(
                CrmBizTypeEnum.CRM_CUSTOMER.getType(), List.of(11L, 13L), 7L);
        verify(taskMapper, never()).markFailure(anyLong(), anyInt(), anyInt(), anyString(), any());
    }

    @Test
    void executionPermissionChangeTransitionsTaskToFailed() {
        CrmExportTaskDO task = task(RUNNING.getStatus());
        when(taskMapper.selectExpiredList(any(), eq(EXPIRED.getStatus()), eq(100))).thenReturn(List.of());
        when(taskMapper.selectQueuedIds(eq(10), eq(QUEUED.getStatus()), any())).thenReturn(List.of(101L));
        when(taskMapper.transition(eq(101L), eq(QUEUED.getStatus()), eq(RUNNING.getStatus()), any())).thenReturn(1);
        when(taskMapper.selectById(101L)).thenReturn(task);
        doThrow(new ServiceException(CRM_EXPORT_PERMISSION_DENIED.getCode(), "permission changed"))
                .when(permissionService).validateExportPermission(anyInt(), anyCollection(), eq(7L));

        assertEquals(1, service.processTenantBatch());

        verify(taskMapper).markFailure(eq(101L), eq(RUNNING.getStatus()), eq(FAILED.getStatus()),
                contains("permission changed"), any());
        verify(provider, never()).generate(anyList(), anyLong());
        verify(fileApi, never()).createFile(any(), any(), any(), any());
    }

    @Test
    void linkageErrorDuringGenerationTransitionsTaskToFailedInsteadOfLeavingItRunning() {
        CrmExportTaskDO task = task(RUNNING.getStatus());
        when(taskMapper.selectExpiredList(any(), eq(EXPIRED.getStatus()), eq(100))).thenReturn(List.of());
        when(taskMapper.selectQueuedIds(eq(10), eq(QUEUED.getStatus()), any())).thenReturn(List.of(101L));
        when(taskMapper.transition(eq(101L), eq(QUEUED.getStatus()), eq(RUNNING.getStatus()), any())).thenReturn(1);
        when(taskMapper.selectById(101L)).thenReturn(task);
        when(provider.generate(List.of(11L, 13L), 7L))
                .thenThrow(new ExceptionInInitializerError("nested resource unavailable"));

        assertEquals(1, service.processTenantBatch());

        verify(taskMapper).markFailure(eq(101L), eq(RUNNING.getStatus()), eq(FAILED.getStatus()),
                contains("nested resource unavailable"), any());
        verify(taskMapper, never()).markSuccess(anyLong(), anyInt(), anyInt(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void issueTokenRevalidatesSnapshotAndStoresOnlyHash() {
        CrmExportTaskDO task = task(SUCCESS.getStatus()).setFileUrl("http://file/export/101");
        when(taskMapper.selectById(101L)).thenReturn(task);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        when(taskMapper.issueToken(eq(101L), eq(SUCCESS.getStatus()), hashCaptor.capture(), any())).thenReturn(1);

        var response = service.issueDownloadToken(101L, 7L);

        assertEquals(48, response.getToken().length());
        assertEquals(DigestUtil.sha256Hex(response.getToken()), hashCaptor.getValue());
        assertNotEquals(response.getToken(), hashCaptor.getValue());
        verify(provider).validateObjects(List.of(11L, 13L));
        verify(permissionService).validateExportPermission(
                CrmBizTypeEnum.CRM_CUSTOMER.getType(), List.of(11L, 13L), 7L);
    }

    @Test
    void downloadTokenCanBeConsumedOnlyOnce() {
        String token = "one-use-token";
        String hash = DigestUtil.sha256Hex(token);
        CrmExportTaskDO task = task(SUCCESS.getStatus()).setFileUrl("http://file/export/101")
                .setFileName("客户导出.xlsx").setContentType("application/xlsx")
                .setDownloadTokenHash(hash).setDownloadTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(taskMapper.selectById(101L)).thenReturn(task);
        FileRespDTO file = new FileRespDTO();
        file.setConfigId(2L);
        file.setPath("crm-protected/export/101/客户导出.xlsx");
        when(fileApi.getFileByUrl(task.getFileUrl())).thenReturn(file);
        when(fileApi.getFileContent(file.getConfigId(), file.getPath())).thenReturn(new byte[]{3, 4});
        when(taskMapper.consumeToken(eq(101L), eq(SUCCESS.getStatus()), eq(hash), any()))
                .thenReturn(1, 0);

        CrmExportTaskService.DownloadFile first = service.download(101L, token, 7L);
        assertArrayEquals(new byte[]{3, 4}, first.content());
        assertServiceException(() -> service.download(101L, token, 7L), EXPORT_TASK_TOKEN_INVALID);

        verify(taskMapper, times(2)).consumeToken(eq(101L), eq(SUCCESS.getStatus()), eq(hash), any());
    }

    @Test
    void expiredResultIsDeletedAndCannotIssueToken() {
        CrmExportTaskDO task = task(SUCCESS.getStatus()).setFileUrl("http://file/export/101")
                .setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(taskMapper.selectById(101L)).thenReturn(task);

        assertServiceException(() -> service.issueDownloadToken(101L, 7L), EXPORT_TASK_EXPIRED);

        verify(fileApi).deleteFileByUrl("http://file/export/101");
        verify(taskMapper).markExpired(101L, EXPIRED.getStatus());
        verify(taskMapper, never()).issueToken(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void schedulerExpiresAllDueStatesAndRemovesProtectedFiles() {
        CrmExportTaskDO expired = task(FAILED.getStatus()).setFileUrl("http://file/export/101")
                .setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(taskMapper.selectExpiredList(any(), eq(EXPIRED.getStatus()), eq(100))).thenReturn(List.of(expired));
        when(taskMapper.selectQueuedIds(eq(10), eq(QUEUED.getStatus()), any())).thenReturn(List.of());

        assertEquals(0, service.processTenantBatch());

        verify(fileApi).deleteFileByUrl("http://file/export/101");
        verify(taskMapper).markExpired(101L, EXPIRED.getStatus());
    }

    @Test
    void disabledFeatureRejectsSubmissionAndScheduler() {
        properties.setEnabled(false);

        assertServiceException(() -> service.createCustomerTask(new CrmCustomerPageReqVO(), 7L),
                EXPORT_TASK_DISABLED);
        assertServiceException(service::processTenantBatch, EXPORT_TASK_DISABLED);
        verifyNoInteractions(taskMapper, customerService, permissionService, fileApi);
    }

    private static CrmCustomerDO customer(Long id) {
        return new CrmCustomerDO().setId(id).setName("Customer-" + id);
    }

    private static CrmExportTaskDO task(Integer status) {
        return new CrmExportTaskDO().setId(101L).setObjectType(CUSTOMER).setCreatorUserId(7L)
                .setObjectIdsSnapshot("[11,13]").setFilterSnapshot("{}")
                .setStatus(status).setTotalCount(2)
                .setExpiresAt(LocalDateTime.now().plusHours(1));
    }
}
