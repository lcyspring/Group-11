package com.meession.etm.module.crm.service.clue;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import com.meession.etm.module.crm.controller.admin.clue.vo.publicpool.CrmCluePublicPageReqVO;
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
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.pool.CrmPoolPolicyProperties;
import com.meession.etm.module.crm.framework.pool.CrmPoolTimeProvider;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.LogRecordConstants.*;

@Service
@Validated
@Slf4j
public class CrmCluePublicPoolServiceImpl implements CrmCluePublicPoolService {

    @Resource
    private CrmClueMapper clueMapper;
    @Resource
    private CrmClueOwnerRecordMapper clueOwnerRecordMapper;
    @Resource
    private CrmClueOwnerCapacityGuardMapper ownerCapacityGuardMapper;
    @Resource
    private CrmCluePoolClaimCounterMapper claimCounterMapper;
    @Resource
    private CrmPermissionService permissionService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private CrmPoolPolicyProperties poolPolicyProperties;
    @Resource
    private CrmPoolTimeProvider poolTimeProvider;

    @Override
    public PageResult<CrmClueDO> getPublicPage(CrmCluePublicPageReqVO pageReqVO) {
        return clueMapper.selectPublicPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#reqVO.clueId",
            level = CrmPermissionLevelEnum.OWNER)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_PUT_PUBLIC_SUB_TYPE,
            bizNo = "{{#reqVO.clueId}}", success = CRM_CLUE_PUT_PUBLIC_SUCCESS)
    public void putCluePublic(CrmCluePublicPutReqVO reqVO, Long userId) {
        CrmClueDO clue = clueMapper.selectByIdForUpdate(reqVO.getClueId());
        if (clue == null) {
            throw exception(CLUE_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(clue.getTransformStatus())) {
            throw exception(CLUE_UPDATE_FAIL_TRANSFORMED);
        }
        if (!isOwned(clue) || !Objects.equals(clue.getOwnerUserId(), userId)) {
            throw exception(CLUE_PUBLIC_STATE_CHANGED, clue.getName());
        }
        putPublic(clue, CrmClueOwnerRecordSourceEnum.MANUAL_PUT_POOL.getSource(),
                reqVO.getReason().trim(), poolTimeProvider.now());
        LogRecordContext.putVariable("clueName", clue.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_CLAIM_PUBLIC_SUB_TYPE,
            bizNo = "{{#primaryClueId}}", success = "领取了 {{#clueIds.size()}} 条公共线索")
    public void claimPublicClues(List<Long> clueIds, Long userId) {
        takePublicClues(clueIds, userId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_ASSIGN_PUBLIC_SUB_TYPE,
            bizNo = "{{#primaryClueId}}", success = "分配了 {{#clueIds.size()}} 条公共线索")
    public void assignPublicClues(List<Long> clueIds, Long ownerUserId, Long operatorUserId) {
        takePublicClues(clueIds, ownerUserId, false);
    }

    private void takePublicClues(List<Long> clueIds, Long ownerUserId, boolean selfClaim) {
        List<Long> ids = normalizeIds(clueIds);
        // system_operate_log.biz_id is scalar. A batch action is anchored to the first
        // normalized clue while the action text keeps the complete batch size.
        LogRecordContext.putVariable("primaryClueId", ids.get(0));
        CrmPoolPolicyProperties.Clue policy = poolPolicyProperties.getClue();
        if (ids.size() > policy.getClaimBatchLimit()) {
            throw exception(CLUE_PUBLIC_BATCH_LIMIT, policy.getClaimBatchLimit());
        }
        adminUserApi.validateUser(ownerUserId);
        List<CrmClueDO> clues = new ArrayList<>(ids.size());
        for (Long clueId : ids) {
            CrmClueDO clue = clueMapper.selectByIdForUpdate(clueId);
            if (clue == null) {
                throw exception(CLUE_NOT_EXISTS);
            }
            if (!isPublic(clue)) {
                throw exception(CLUE_PUBLIC_STATE_CHANGED, clue.getName());
            }
            clues.add(clue);
        }
        validateOwnerCapacity(ownerUserId, clues.size());
        LocalDateTime now = poolTimeProvider.now();
        if (selfClaim) {
            validateRepeatClaimCooldown(clues, ownerUserId, now);
            if (claimCounterMapper.reserve(TenantContextHolder.getRequiredTenantId(), ownerUserId,
                    now.toLocalDate(), clues.size(), policy.getDailyClaimLimit()) == 0) {
                throw exception(CLUE_PUBLIC_DAILY_CLAIM_LIMIT, policy.getDailyClaimLimit());
            }
        }
        String source = selfClaim ? CrmClueOwnerRecordSourceEnum.SELF_CLAIM.getSource()
                : CrmClueOwnerRecordSourceEnum.MANAGER_ASSIGN.getSource();
        for (CrmClueDO clue : clues) {
            if (clueMapper.updateClaimedFromPublicPool(clue.getId(), ownerUserId, now) == 0) {
                throw exception(CLUE_PUBLIC_STATE_CHANGED, clue.getName());
            }
            permissionService.replaceOwnerPermission(CrmBizTypeEnum.CRM_CLUE.getType(), clue.getId(), ownerUserId);
            clueOwnerRecordMapper.insert(new CrmClueOwnerRecordDO().setClueId(clue.getId())
                    .setPreviousOwnerUserId(null).setNewOwnerUserId(ownerUserId)
                    .setType(CrmClueOwnerRecordTypeEnum.TAKE_POOL.getType()).setSource(source));
        }
    }

    @Override
    public int autoPutCluePublicPool() {
        CrmPoolPolicyProperties.Clue policy = poolPolicyProperties.getClue();
        if (!policy.isEnabled()) {
            return 0;
        }
        LocalDateTime now = poolTimeProvider.now();
        LocalDateTime expireBefore = now.minusDays(policy.getContactExpireDays());
        int scanSize = Math.min(policy.getAutoPoolBatchSize(), policy.getAutoPoolMaxBatchSize());
        long afterId = 0L;
        int count = 0;
        for (int batch = 0; batch < policy.getAutoPoolMaxBatches(); batch++) {
            List<CrmClueDO> candidates = clueMapper.selectListByAutoPool(afterId, expireBefore,
                    scanSize, policy.getAutoPoolMaxBatchSize());
            if (CollUtil.isEmpty(candidates)) {
                break;
            }
            afterId = candidates.get(candidates.size() - 1).getId();
            for (CrmClueDO candidate : candidates) {
                try {
                    if (getSelf().autoPutSingleClue(candidate.getId(), expireBefore, now)) {
                        count++;
                    }
                } catch (RuntimeException ex) {
                    log.error("[autoPutCluePublicPool][clueId({}) failed]", candidate.getId(), ex);
                }
            }
            if (candidates.size() < scanSize) {
                break;
            }
        }
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    protected boolean autoPutSingleClue(Long clueId, LocalDateTime expireBefore, LocalDateTime now) {
        CrmClueDO clue = clueMapper.selectByIdForUpdate(clueId);
        if (clue == null || !isOwned(clue) || Boolean.TRUE.equals(clue.getTransformStatus())) {
            return false;
        }
        LocalDateTime ownerTime = clue.getOwnerTime() != null ? clue.getOwnerTime() : clue.getCreateTime();
        LocalDateTime lastActivity = clue.getContactLastTime() != null
                && (ownerTime == null || clue.getContactLastTime().isAfter(ownerTime))
                ? clue.getContactLastTime() : ownerTime;
        if (lastActivity == null || lastActivity.isAfter(expireBefore)) {
            return false;
        }
        putPublic(clue, CrmClueOwnerRecordSourceEnum.AUTO_NO_FOLLOW_UP.getSource(),
                "超过配置天数未跟进", now);
        return true;
    }

    private void putPublic(CrmClueDO clue, String source, String reason, LocalDateTime now) {
        if (clueMapper.updateToPublicPool(clue.getId(), clue.getOwnerUserId(), now, source, reason) == 0) {
            throw exception(CLUE_PUBLIC_STATE_CHANGED, clue.getName());
        }
        permissionService.deletePermissionIfPresent(CrmBizTypeEnum.CRM_CLUE.getType(), clue.getId());
        clueOwnerRecordMapper.insert(new CrmClueOwnerRecordDO().setClueId(clue.getId())
                .setPreviousOwnerUserId(clue.getOwnerUserId()).setNewOwnerUserId(null)
                .setType(CrmClueOwnerRecordTypeEnum.PUT_POOL.getType()).setSource(source).setReason(reason));
    }

    private void validateOwnerCapacity(Long ownerUserId, int increment) {
        // The guard serializes increments for one owner. READ_COMMITTED is required on the
        // public entry points so a waiter recounts after the preceding transaction commits;
        // under MySQL REPEATABLE_READ it could otherwise reuse a stale snapshot and overbook.
        ownerCapacityGuardMapper.lockOwnerCapacity(TenantContextHolder.getRequiredTenantId(), ownerUserId);
        long current = clueMapper.selectOwnedCountByUserId(ownerUserId);
        int maxOwnedClues = poolPolicyProperties.getClue().getMaxOwnedClues();
        if (current + increment > maxOwnedClues) {
            throw exception(CLUE_OWNER_LIMIT_EXCEEDED, maxOwnedClues);
        }
    }

    private void validateRepeatClaimCooldown(List<CrmClueDO> clues, Long ownerUserId, LocalDateTime now) {
        int days = poolPolicyProperties.getClue().getRepeatClaimCooldownDays();
        if (days <= 0) {
            return;
        }
        LocalDateTime since = now.minusDays(days);
        for (CrmClueDO clue : clues) {
            if (clueOwnerRecordMapper.existsRecentSelfClaim(clue.getId(), ownerUserId, since)) {
                throw exception(CLUE_PUBLIC_REPEAT_CLAIM_COOLDOWN, days, clue.getName());
            }
        }
    }

    private static List<Long> normalizeIds(List<Long> clueIds) {
        if (CollUtil.isEmpty(clueIds)) {
            throw exception(CLUE_NOT_EXISTS);
        }
        List<Long> ids = new LinkedHashSet<>(clueIds).stream().filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder()).toList();
        if (ids.isEmpty()) {
            throw exception(CLUE_NOT_EXISTS);
        }
        return ids;
    }

    private static boolean isOwned(CrmClueDO clue) {
        return Objects.equals(clue.getPoolStatus(), CrmCluePoolStatusEnum.OWNED.getStatus())
                && clue.getOwnerUserId() != null;
    }

    private static boolean isPublic(CrmClueDO clue) {
        return Objects.equals(clue.getPoolStatus(), CrmCluePoolStatusEnum.PUBLIC.getStatus())
                && clue.getOwnerUserId() == null && !Boolean.TRUE.equals(clue.getTransformStatus());
    }

    private CrmCluePublicPoolServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }
}
