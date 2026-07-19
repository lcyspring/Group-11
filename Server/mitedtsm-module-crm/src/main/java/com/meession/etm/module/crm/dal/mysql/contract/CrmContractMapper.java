package com.meession.etm.module.crm.dal.mysql.contract;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.contract.vo.contract.CrmContractPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractConfigDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Set;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * CRM 合同 Mapper
 *
 * @author dhb52
 */
@Mapper
public interface CrmContractMapper extends BaseMapperX<CrmContractDO> {

    default CrmContractDO selectByNo(String no) {
        return selectOne(CrmContractDO::getNo, no);
    }

    default CrmContractDO selectFirstByBusinessId(Long businessId) {
        return selectOne(new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getBusinessId, businessId)
                .orderByAsc(CrmContractDO::getId)
                .last("LIMIT 1"));
    }

    default CrmContractDO selectBySourceBusinessIdForUpdate(Long businessId) {
        return selectOne(new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getSourceBusinessId, businessId)
                .last("FOR UPDATE"));
    }

    default CrmContractDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getId, id)
                .last("FOR UPDATE"));
    }

    default int updateAuditStatusIfProcessing(Long id, String processInstanceId, Integer auditStatus) {
        return update(new LambdaUpdateWrapper<CrmContractDO>()
                .eq(CrmContractDO::getId, id)
                .eq(CrmContractDO::getProcessInstanceId, processInstanceId)
                .eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus())
                .set(CrmContractDO::getAuditStatus, auditStatus));
    }

    default PageResult<CrmContractDO> selectPageByCustomerId(CrmContractPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getCustomerId, pageReqVO.getCustomerId())
                .likeIfPresent(CrmContractDO::getNo, pageReqVO.getNo())
                .likeIfPresent(CrmContractDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmContractDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmContractDO::getBusinessId, pageReqVO.getBusinessId())
                .orderByDesc(CrmContractDO::getId));
    }

    default PageResult<CrmContractDO> selectPageByBusinessId(CrmContractPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getBusinessId, pageReqVO.getBusinessId())
                .likeIfPresent(CrmContractDO::getNo, pageReqVO.getNo())
                .likeIfPresent(CrmContractDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmContractDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmContractDO::getBusinessId, pageReqVO.getBusinessId())
                .orderByDesc(CrmContractDO::getId));
    }

    default PageResult<CrmContractDO> selectPage(CrmContractPageReqVO pageReqVO, Long userId, CrmContractConfigDO config) {
        MPJLambdaWrapperX<CrmContractDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                CrmContractDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmContractDO.class)
                .likeIfPresent(CrmContractDO::getNo, pageReqVO.getNo())
                .likeIfPresent(CrmContractDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmContractDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmContractDO::getBusinessId, pageReqVO.getBusinessId())
                .eqIfPresent(CrmContractDO::getAuditStatus, pageReqVO.getAuditStatus())
                .orderByDesc(CrmContractDO::getId);

        // Backlog: 即将到期的合同
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        LocalDateTime endOfToday = LocalDateTimeUtil.endOfDay(LocalDateTime.now());
        if (CrmContractPageReqVO.EXPIRY_TYPE_ABOUT_TO_EXPIRE.equals(pageReqVO.getExpiryType())) { // 即将到期
            query.eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                    .between(CrmContractDO::getEndTime, beginOfToday, endOfToday.plusDays(config.getNotifyDays()));
        } else if (CrmContractPageReqVO.EXPIRY_TYPE_EXPIRED.equals(pageReqVO.getExpiryType())) { // 已到期
            query.eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                    .lt(CrmContractDO::getEndTime, endOfToday);
        }
        return selectJoinPage(pageReqVO, CrmContractDO.class, query);
    }

    default Long selectCountByContactId(Long contactId) {
        return selectCount(CrmContractDO::getSignContactId, contactId);
    }

    default Long selectCountByBusinessId(Long businessId) {
        return selectCount(CrmContractDO::getBusinessId, businessId);
    }

    default Long selectCountByAudit(Long userId) {
        MPJLambdaWrapperX<CrmContractDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                CrmContractDO::getId, userId, CrmSceneTypeEnum.OWNER.getType());
        // 未审核
        query.eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus());
        return selectCount(query);
    }

    default Long selectCountByRemind(Long userId, CrmContractConfigDO config) {
        MPJLambdaWrapperX<CrmContractDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                CrmContractDO::getId, userId, CrmSceneTypeEnum.OWNER.getType());
        // 即将到期
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        LocalDateTime endOfToday = LocalDateTimeUtil.endOfDay(LocalDateTime.now());
        query.eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus()) // 必须审批通过！
                .between(CrmContractDO::getEndTime, beginOfToday, endOfToday.plusDays(config.getNotifyDays()));
        return selectCount(query);
    }

    default List<CrmContractDO> selectListByCustomerIdOwnerUserId(Long customerId, Long ownerUserId) {
        return selectList(new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getCustomerId, customerId)
                .eq(CrmContractDO::getOwnerUserId, ownerUserId));
    }

    default List<CrmContractDO> selectReceivableCandidateList(Long customerId) {
        return selectList(new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                .eqIfPresent(CrmContractDO::getCustomerId, customerId)
                .orderByDesc(CrmContractDO::getId));
    }

    default Set<Long> selectProtectedCustomerIds(Collection<Long> customerIds, Collection<Integer> auditStatuses,
                                                  LocalDateTime now) {
        if (customerIds == null || customerIds.isEmpty() || auditStatuses == null || auditStatuses.isEmpty()) {
            return Set.of();
        }
        return convertSet(selectList(new LambdaQueryWrapperX<CrmContractDO>()
                .select(CrmContractDO::getCustomerId)
                .in(CrmContractDO::getCustomerId, customerIds)
                .in(CrmContractDO::getAuditStatus, auditStatuses)
                .and(scope -> scope.ne(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                        .or(approved -> approved.eq(CrmContractDO::getAuditStatus,
                                        CrmAuditStatusEnum.APPROVE.getStatus())
                                .and(valid -> valid.isNull(CrmContractDO::getEndTime)
                                        .or().ge(CrmContractDO::getEndTime, now))))
                .groupBy(CrmContractDO::getCustomerId)), CrmContractDO::getCustomerId);
    }

    default boolean existsProtectedByCustomerId(Long customerId, Collection<Integer> auditStatuses,
                                                 LocalDateTime now) {
        return selectProtectedCustomerIds(List.of(customerId), auditStatuses, now).contains(customerId);
    }

}
