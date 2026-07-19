package com.meession.etm.module.crm.service.clue;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmCluePageReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueSaveReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueTransferReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueTransformReqVO;
import com.meession.etm.module.crm.controller.admin.contact.vo.CrmContactSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueOwnerRecordDO;
import com.meession.etm.module.crm.dal.dataobject.followup.CrmFollowUpRecordDO;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueOwnerRecordMapper;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueOwnerCapacityGuardMapper;
import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import com.meession.etm.module.crm.enums.clue.CrmClueOwnerRecordSourceEnum;
import com.meession.etm.module.crm.enums.clue.CrmClueOwnerRecordTypeEnum;
import com.meession.etm.module.crm.enums.clue.CrmCluePoolStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.framework.pool.CrmPoolPolicyProperties;
import com.meession.etm.module.crm.framework.pool.CrmPoolTimeProvider;
import com.meession.etm.module.crm.service.activity.CrmActivityService;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.customer.bo.CrmCustomerCreateReqBO;
import com.meession.etm.module.crm.service.followup.CrmFollowUpRecordService;
import com.meession.etm.module.crm.service.followup.bo.CrmFollowUpCreateReqBO;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionTransferReqBO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.singleton;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.LogRecordConstants.*;
import static com.meession.etm.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;

/**
 * 线索 Service 实现类
 *
 * @author Wanwan
 */
@Service
@Validated
public class CrmClueServiceImpl implements CrmClueService {

    @Resource
    private CrmClueMapper clueMapper;
    @Resource
    private CrmClueOwnerRecordMapper clueOwnerRecordMapper;
    @Resource
    private CrmClueOwnerCapacityGuardMapper ownerCapacityGuardMapper;
    @Resource
    private CrmPoolPolicyProperties poolPolicyProperties;
    @Resource
    private CrmPoolTimeProvider poolTimeProvider;

    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmContactService contactService;
    @Resource
    private CrmPermissionService crmPermissionService;
    @Resource
    private CrmFollowUpRecordService followUpRecordService;
    @Resource
    private CrmActivityService activityService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_CREATE_SUB_TYPE, bizNo = "{{#clue.id}}",
            success = CRM_CLUE_CREATE_SUCCESS)
    public Long createClue(CrmClueSaveReqVO createReqVO) {
        // 1.1 校验关联数据
        validateRelationDataExists(createReqVO);
        // 1.2 负责人可为空；未指定负责人时线索直接进入显式公共池。
        if (createReqVO.getOwnerUserId() != null) {
            adminUserApi.validateUser(createReqVO.getOwnerUserId());
            validateOwnerCapacity(createReqVO.getOwnerUserId(), 1);
        }

        // 2. 插入线索
        CrmClueDO clue = BeanUtils.toBean(createReqVO, CrmClueDO.class);
        LocalDateTime now = poolTimeProvider.now();
        boolean publicPool = clue.getOwnerUserId() == null;
        clue.setTransformStatus(false).setFollowUpStatus(false)
                .setPoolStatus(publicPool ? CrmCluePoolStatusEnum.PUBLIC.getStatus()
                        : CrmCluePoolStatusEnum.OWNED.getStatus())
                .setOwnerTime(publicPool ? null : now)
                .setPoolEntryTime(publicPool ? now : null)
                .setPoolPreviousOwnerUserId(null)
                .setPoolReason(publicPool ? CrmClueOwnerRecordSourceEnum.CREATE_UNASSIGNED.getSource() : null)
                .setPoolReasonDetail(null)
                .setPoolCycleCount(publicPool ? 1 : 0);
        clueMapper.insert(clue);

        // 3. 创建数据权限
        if (!publicPool) {
            CrmPermissionCreateReqBO createReqBO = new CrmPermissionCreateReqBO()
                    .setBizType(CrmBizTypeEnum.CRM_CLUE.getType()).setBizId(clue.getId())
                    .setUserId(clue.getOwnerUserId()).setLevel(CrmPermissionLevelEnum.OWNER.getLevel());
            crmPermissionService.createPermission(createReqBO);
        }
        clueOwnerRecordMapper.insert(new CrmClueOwnerRecordDO().setClueId(clue.getId())
                .setPreviousOwnerUserId(null).setNewOwnerUserId(clue.getOwnerUserId())
                .setType((publicPool ? CrmClueOwnerRecordTypeEnum.PUT_POOL
                        : CrmClueOwnerRecordTypeEnum.INITIAL_ASSIGN).getType())
                .setSource((publicPool ? CrmClueOwnerRecordSourceEnum.CREATE_UNASSIGNED
                        : CrmClueOwnerRecordSourceEnum.INITIAL_ASSIGN).getSource())
                .setReason(publicPool ? "创建时未指定负责人" : null));

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("clue", clue);
        return clue.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_CLUE_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void updateClue(CrmClueSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "线索编号不能为空");
        // 1.1 校验线索是否存在
        CrmClueDO oldClue = validateClueWritableForUpdate(updateReqVO.getId());
        // 1.2 校验关联数据
        validateRelationDataExists(updateReqVO);

        // 2. 更新线索
        CrmClueDO updateObj = BeanUtils.toBean(updateReqVO, CrmClueDO.class);
        // 负责人只能通过转移命令变更，禁止普通更新绕过权限和归属审计。
        updateObj.setOwnerUserId(oldClue.getOwnerUserId());
        clueMapper.updateById(updateObj);

        // 3. 记录操作日志上下文
        updateReqVO.setOwnerUserId(oldClue.getOwnerUserId()); // 避免操作日志出现“删除负责人”的情况
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldClue, CrmClueSaveReqVO.class));
        LogRecordContext.putVariable("clueName", oldClue.getName());
    }

    private void validateRelationDataExists(CrmClueSaveReqVO reqVO) {
        // 校验负责人
        if (Objects.nonNull(reqVO.getOwnerUserId()) &&
                Objects.isNull(adminUserApi.getUser(reqVO.getOwnerUserId()))) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_FOLLOW_UP_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CLUE_FOLLOW_UP_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void updateClueFollowUp(Long id, LocalDateTime contactNextTime, String contactLastContent) {
        // 校验线索是否存在
        CrmClueDO oldClue = validateClueWritableForUpdate(id);

        // 更新线索
        clueMapper.updateById(new CrmClueDO().setId(id).setFollowUpStatus(true).setContactNextTime(contactNextTime)
                .setContactLastTime(poolTimeProvider.now()).setContactLastContent(contactLastContent));

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("clueName", oldClue.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CLUE_DELETE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void deleteClue(Long id) {
        // 1. 校验存在
        CrmClueDO clue = validateClueWritableForUpdate(id);

        // 2. 删除
        clueMapper.deleteById(id);

        // 3. 删除数据权限
        crmPermissionService.deletePermission(CrmBizTypeEnum.CRM_CLUE.getType(), id);

        // 4. 删除跟进
        followUpRecordService.deleteFollowUpRecordByBiz(CrmBizTypeEnum.CRM_CLUE.getType(), id);

        // 5. 记录操作日志上下文
        LogRecordContext.putVariable("clueName", clue.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_TRANSFER_SUB_TYPE, bizNo = "{{#reqVO.id}}",
            success = CRM_CLUE_TRANSFER_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void transferClue(CrmClueTransferReqVO reqVO, Long userId) {
        // 1 校验线索是否存在
        CrmClueDO clue = validateClueWritableForUpdate(reqVO.getId());
        adminUserApi.validateUser(reqVO.getNewOwnerUserId());
        if (!Objects.equals(clue.getOwnerUserId(), reqVO.getNewOwnerUserId())) {
            validateOwnerCapacity(reqVO.getNewOwnerUserId(), 1);
        }

        // 2.1 数据权限转移
        crmPermissionService.transferPermission(new CrmPermissionTransferReqBO(userId, CrmBizTypeEnum.CRM_CLUE.getType(),
                        reqVO.getId(), reqVO.getNewOwnerUserId(), reqVO.getOldOwnerPermissionLevel()));
        // 2.2 设置新的负责人
        clueMapper.updateById(new CrmClueDO().setId(reqVO.getId()).setOwnerUserId(reqVO.getNewOwnerUserId())
                .setOwnerTime(poolTimeProvider.now()).setPoolStatus(CrmCluePoolStatusEnum.OWNED.getStatus()));
        clueOwnerRecordMapper.insert(new CrmClueOwnerRecordDO().setClueId(clue.getId())
                .setPreviousOwnerUserId(clue.getOwnerUserId()).setNewOwnerUserId(reqVO.getNewOwnerUserId())
                .setType(CrmClueOwnerRecordTypeEnum.TRANSFER.getType())
                .setSource(CrmClueOwnerRecordSourceEnum.TRANSFER.getSource()));

        // 3. 记录转移日志
        LogRecordContext.putVariable("clue", clue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_TRANSLATE_SUB_TYPE, bizNo = "{{#reqVO.id}}",
            success = CRM_CLUE_TRANSLATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void transformClue(CrmClueTransformReqVO reqVO, Long userId) {
        // 1.1 当前读并锁定线索，与更新、删除、转移及关联写操作串行化
        CrmClueDO clue = clueMapper.selectByIdForUpdate(reqVO.getId());
        if (clue == null) {
            throw exception(CLUE_NOT_EXISTS);
        }
        // 1.2 存在已经转化的
        if (Boolean.TRUE.equals(clue.getTransformStatus())) {
            throw exception(CLUE_TRANSFORM_FAIL_ALREADY);
        }
        if (!isOwned(clue)) {
            throw exception(CLUE_PUBLIC_CLAIM_REQUIRED);
        }

        // 1.3 原子抢占转换权，避免两个并发请求各自创建客户
        if (clueMapper.updateTransformStatusByIdAndTransformStatus(reqVO.getId(), Boolean.FALSE, Boolean.TRUE) == 0) {
            throw exception(CLUE_TRANSFORM_FAIL_ALREADY);
        }

        // 2.1 遍历线索(未转化的线索)，创建对应的客户
        Long customerId = customerService.createCustomer(BeanUtils.toBean(clue, CrmCustomerCreateReqBO.class), userId);
        // 2.2 创建首联系人。失败时由外层事务回滚客户及转换状态
        Long primaryContactId = contactService.createContact(new CrmContactSaveReqVO()
                .setName(reqVO.getContactName()).setMobile(reqVO.getContactMobile())
                .setCustomerId(customerId).setOwnerUserId(userId)
                .setTelephone(clue.getTelephone()).setWechat(clue.getWechat()).setEmail(clue.getEmail())
                .setAreaId(clue.getAreaId()).setDetailAddress(clue.getDetailAddress())
                .setMaster(false).setPrimaryContact(true), userId);
        // 2.3 记录转换后的客户；转换状态已在上方原子更新
        clueMapper.updateById(new CrmClueDO().setId(reqVO.getId()).setCustomerId(customerId));
        // 2.4 复制跟进记录
        List<CrmFollowUpRecordDO> followUpRecords = followUpRecordService.getFollowUpRecordByBiz(
                CrmBizTypeEnum.CRM_CLUE.getType(), singleton(clue.getId()));
        if (CollUtil.isNotEmpty(followUpRecords)) {
            followUpRecordService.createFollowUpRecordBatch(convertList(followUpRecords, record ->
                    BeanUtils.toBean(record, CrmFollowUpCreateReqBO.class)
                            .setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType()).setBizId(customerId)));
        }
        // 2.5 任务、通话和短信改绑至客户，并保存唯一转换审计。该调用要求复用当前事务，
        // 任一活动迁移失败都会回滚客户、首联系人、跟进复制及线索转换状态。
        activityService.migrateClueActivities(clue.getId(), customerId, primaryContactId,
                followUpRecords == null ? 0 : followUpRecords.size(), userId);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("clueName", clue.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void validateClueWritable(Long id) {
        validateClueWritableForUpdate(id);
    }

    private CrmClueDO validateClueWritableForUpdate(Long id) {
        CrmClueDO clue = clueMapper.selectByIdForUpdate(id);
        if (clue == null) {
            throw exception(CLUE_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(clue.getTransformStatus())) {
            throw exception(CLUE_UPDATE_FAIL_TRANSFORMED);
        }
        if (!isOwned(clue)) {
            throw exception(CLUE_PUBLIC_CLAIM_REQUIRED);
        }
        return clue;
    }

    private void validateOwnerCapacity(Long ownerUserId, int increment) {
        // The capacity guard makes all increments for one owner wait in order. These callers
        // use READ_COMMITTED so the count after the wait observes the preceding commit instead
        // of a MySQL REPEATABLE_READ snapshot created by user/object validation.
        ownerCapacityGuardMapper.lockOwnerCapacity(TenantContextHolder.getRequiredTenantId(), ownerUserId);
        long current = clueMapper.selectOwnedCountByUserId(ownerUserId);
        int maxOwnedClues = poolPolicyProperties.getClue().getMaxOwnedClues();
        if (current + increment > maxOwnedClues) {
            throw exception(CLUE_OWNER_LIMIT_EXCEEDED, maxOwnedClues);
        }
    }

    private static boolean isOwned(CrmClueDO clue) {
        return Objects.equals(clue.getPoolStatus(), CrmCluePoolStatusEnum.OWNED.getStatus())
                && clue.getOwnerUserId() != null;
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#id", level = CrmPermissionLevelEnum.READ)
    public CrmClueDO getClue(Long id) {
        return clueMapper.selectById(id);
    }

    @Override
    public PageResult<CrmClueDO> getCluePage(CrmCluePageReqVO pageReqVO, Long userId) {
        return clueMapper.selectPage(pageReqVO, userId);
    }

    @Override
    public Long getFollowClueCount(Long userId) {
        return clueMapper.selectCountByFollow(userId);
    }

}
