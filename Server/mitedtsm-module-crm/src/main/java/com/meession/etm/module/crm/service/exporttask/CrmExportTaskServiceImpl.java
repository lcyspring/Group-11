package com.meession.etm.module.crm.service.exporttask;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerPageReqVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportDownloadTokenRespVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportTaskPageReqVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportTaskRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.exporttask.CrmExportTaskDO;
import com.meession.etm.module.crm.dal.mysql.exporttask.CrmExportTaskMapper;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.framework.exporttask.CrmExportTaskProperties;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.infra.api.file.FileApi;
import com.meession.etm.module.infra.api.file.dto.FileRespDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.pojo.PageParam.PAGE_SIZE_NONE;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.exporttask.CrmExportObjectType.CUSTOMER;
import static com.meession.etm.module.crm.enums.exporttask.CrmExportTaskStatusEnum.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrmExportTaskServiceImpl implements CrmExportTaskService {
    private final CrmExportTaskMapper taskMapper;
    private final CrmCustomerService customerService;
    private final CrmPermissionService permissionService;
    private final CrmExportTaskProperties properties;
    private final FileApi fileApi;
    private final List<CrmExportTaskProvider> providers;
    private final RedissonClient redissonClient;

    @Override
    public Long createCustomerTask(CrmCustomerPageReqVO filter, Long userId) {
        requireEnabled();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RLock lock = redissonClient.getLock("crm:export-task:create:" + tenantId + ":" + userId);
        boolean acquired = false;
        try {
            acquired = lock.tryLock();
            if (!acquired) {
                throw exception(EXPORT_TASK_CAPACITY_EXCEEDED, properties.getMaxPendingPerUser());
            }
            long active = taskMapper.selectActiveCount(userId, List.of(QUEUED.getStatus(), RUNNING.getStatus()));
            if (active >= properties.getMaxPendingPerUser()) {
                throw exception(EXPORT_TASK_CAPACITY_EXCEEDED, properties.getMaxPendingPerUser());
            }
            filter.setPageNo(1);
            filter.setPageSize(PAGE_SIZE_NONE);
            List<CrmCustomerDO> customers = customerService.getCustomerPage(filter, userId).getList();
            if (customers.size() > properties.getMaxRows()) {
                throw exception(EXPORT_TASK_ROW_LIMIT, customers.size(), properties.getMaxRows());
            }
            List<Long> objectIds = convertList(customers, CrmCustomerDO::getId);
            permissionService.validateExportPermission(CrmBizTypeEnum.CRM_CUSTOMER.getType(), objectIds, userId);
            CrmExportTaskDO task = new CrmExportTaskDO().setObjectType(CUSTOMER).setCreatorUserId(userId)
                    .setFilterSnapshot(JsonUtils.toJsonString(filter))
                    .setObjectIdsSnapshot(JsonUtils.toJsonString(objectIds))
                    .setStatus(QUEUED.getStatus()).setTotalCount(objectIds.size())
                    .setExpiresAt(LocalDateTime.now().plusHours(properties.getRetentionHours()));
            taskMapper.insert(task);
            return task.getId();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public PageResult<CrmExportTaskRespVO> getTaskPage(CrmExportTaskPageReqVO request, Long userId) {
        PageResult<CrmExportTaskDO> page = taskMapper.selectPage(request, userId);
        return new PageResult<>(convertList(page.getList(), this::toResponse), page.getTotal());
    }

    @Override
    public CrmExportTaskRespVO getTask(Long id, Long userId) {
        return toResponse(requireOwner(id, userId));
    }

    @Override
    public CrmExportDownloadTokenRespVO issueDownloadToken(Long id, Long userId) {
        CrmExportTaskDO task = requireDownloadable(id, userId);
        validateSnapshot(task);
        String token = RandomUtil.randomString(48);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(properties.getTokenTtlSeconds());
        if (taskMapper.issueToken(id, SUCCESS.getStatus(), sha256(token), expiresAt) == 0) {
            throw exception(EXPORT_TASK_STATUS_INVALID);
        }
        return new CrmExportDownloadTokenRespVO(token, expiresAt);
    }

    @Override
    public DownloadFile download(Long id, String token, Long userId) {
        CrmExportTaskDO task = requireDownloadable(id, userId);
        String hash = sha256(token);
        if (task.getDownloadTokenHash() == null || task.getDownloadTokenExpiresAt() == null
                || !task.getDownloadTokenExpiresAt().isAfter(LocalDateTime.now())
                || !MessageDigest.isEqual(task.getDownloadTokenHash().getBytes(StandardCharsets.UTF_8),
                hash.getBytes(StandardCharsets.UTF_8))) {
            throw exception(EXPORT_TASK_TOKEN_INVALID);
        }
        validateSnapshot(task);
        FileRespDTO file = fileApi.getFileByUrl(task.getFileUrl());
        byte[] content = fileApi.getFileContent(file.getConfigId(), file.getPath());
        if (taskMapper.consumeToken(id, SUCCESS.getStatus(), hash, LocalDateTime.now()) == 0) {
            throw exception(EXPORT_TASK_TOKEN_INVALID);
        }
        return new DownloadFile(content, task.getFileName(), task.getContentType());
    }

    @Override
    public int processTenantBatch() {
        requireEnabled();
        LocalDateTime now = LocalDateTime.now();
        expireTasks(now);
        int processed = 0;
        for (Long id : taskMapper.selectQueuedIds(properties.getBatchSize(), QUEUED.getStatus(), now)) {
            if (taskMapper.transition(id, QUEUED.getStatus(), RUNNING.getStatus(), LocalDateTime.now()) == 0) {
                continue;
            }
            processOne(id);
            processed++;
        }
        return processed;
    }

    private void processOne(Long id) {
        CrmExportTaskDO task = taskMapper.selectById(id);
        String fileUrl = null;
        try {
            List<Long> ids = parseIds(task);
            CrmExportTaskProvider provider = provider(task.getObjectType());
            provider.validateObjects(ids);
            permissionService.validateExportPermission(provider.bizType(), ids, task.getCreatorUserId());
            CrmExportTaskProvider.ExportFile file = provider.generate(ids, task.getCreatorUserId());
            fileUrl = fileApi.createFile(file.content(), file.fileName(),
                    "crm-protected/export/" + task.getId(), file.contentType());
            if (taskMapper.markSuccess(task.getId(), RUNNING.getStatus(), SUCCESS.getStatus(), fileUrl,
                    file.fileName(), file.contentType(), LocalDateTime.now()) == 0) {
                throw new IllegalStateException("export task state changed while saving result");
            }
        } catch (RuntimeException | LinkageError ex) {
            if (fileUrl != null) {
                deleteFileQuietly(fileUrl);
            }
            taskMapper.markFailure(task.getId(), RUNNING.getStatus(), FAILED.getStatus(),
                    StrUtil.maxLength(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), 1000),
                    LocalDateTime.now());
            log.error("[processOne][CRM export task {} failed]", id, ex);
        }
    }

    private void expireTasks(LocalDateTime now) {
        for (CrmExportTaskDO task : taskMapper.selectExpiredList(now, EXPIRED.getStatus(),
                properties.getMaxBatchSize())) {
            if (task.getFileUrl() != null) {
                deleteFileQuietly(task.getFileUrl());
            }
            taskMapper.markExpired(task.getId(), EXPIRED.getStatus());
        }
    }

    private CrmExportTaskDO requireOwner(Long id, Long userId) {
        CrmExportTaskDO task = taskMapper.selectById(id);
        if (task == null || !userId.equals(task.getCreatorUserId())) {
            throw exception(EXPORT_TASK_NOT_EXISTS);
        }
        return task;
    }

    private CrmExportTaskDO requireDownloadable(Long id, Long userId) {
        CrmExportTaskDO task = requireOwner(id, userId);
        if (!task.getExpiresAt().isAfter(LocalDateTime.now())) {
            if (task.getFileUrl() != null) {
                deleteFileQuietly(task.getFileUrl());
            }
            taskMapper.markExpired(task.getId(), EXPIRED.getStatus());
            throw exception(EXPORT_TASK_EXPIRED);
        }
        if (!Objects.equals(SUCCESS.getStatus(), task.getStatus()) || task.getFileUrl() == null) {
            throw exception(EXPORT_TASK_STATUS_INVALID);
        }
        return task;
    }

    private void validateSnapshot(CrmExportTaskDO task) {
        List<Long> ids = parseIds(task);
        CrmExportTaskProvider provider = provider(task.getObjectType());
        provider.validateObjects(ids);
        permissionService.validateExportPermission(provider.bizType(), ids, task.getCreatorUserId());
    }

    private List<Long> parseIds(CrmExportTaskDO task) {
        try {
            return JsonUtils.parseObject(task.getObjectIdsSnapshot(), new TypeReference<List<Long>>() { });
        } catch (RuntimeException ex) {
            throw exception(EXPORT_TASK_OBJECT_CHANGED);
        }
    }

    private CrmExportTaskProvider provider(String objectType) {
        return providers.stream().filter(item -> item.objectType().equals(objectType)).findFirst()
                .orElseThrow(() -> exception(EXPORT_TASK_PROVIDER_NOT_EXISTS, objectType));
    }

    private CrmExportTaskRespVO toResponse(CrmExportTaskDO task) {
        CrmExportTaskRespVO response = BeanUtils.toBean(task, CrmExportTaskRespVO.class);
        response.setDownloadAvailable(Objects.equals(SUCCESS.getStatus(), task.getStatus())
                && task.getFileUrl() != null && task.getExpiresAt().isAfter(LocalDateTime.now()));
        return response;
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw exception(EXPORT_TASK_DISABLED);
        }
    }

    private void deleteFileQuietly(String url) {
        try {
            fileApi.deleteFileByUrl(url);
        } catch (RuntimeException ex) {
            log.warn("[deleteFileQuietly][failed to delete expired export file {}]", url, ex);
        }
    }

    private static String sha256(String value) {
        return cn.hutool.crypto.digest.DigestUtil.sha256Hex(value);
    }
}
