package com.meession.etm.module.crm.dal.mysql.receivable;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.receivable.vo.plan.CrmReceivablePlanPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 回款计划 Mapper
 *
 * @author 密讯
 */
@Mapper
public interface CrmReceivablePlanMapper extends BaseMapperX<CrmReceivablePlanDO> {

    default CrmReceivablePlanDO selectByIdForUpdate(Long id) {
        return selectOne(new MPJLambdaWrapperX<CrmReceivablePlanDO>()
                .eq(CrmReceivablePlanDO::getId, id)
                .last("FOR UPDATE"));
    }

    @Select("SELECT id FROM crm_contract WHERE id = #{contractId} AND deleted = 0 FOR UPDATE")
    Long selectContractIdForUpdate(Long contractId);

    default List<CrmReceivablePlanDO> selectListByContractId(Long contractId) {
        return selectList(CrmReceivablePlanDO::getContractId, contractId);
    }

    default CrmReceivablePlanDO selectMaxPeriodByContractId(Long contractId) {
        return selectOne(new MPJLambdaWrapperX<CrmReceivablePlanDO>()
                .eq(CrmReceivablePlanDO::getContractId, contractId)
                .orderByDesc(CrmReceivablePlanDO::getPeriod)
                .last("LIMIT 1"));
    }

    default PageResult<CrmReceivablePlanDO> selectPageByCustomerId(CrmReceivablePlanPageReqVO reqVO) {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();
        if (Objects.nonNull(reqVO.getContractNo())) { // 根据合同编号检索
            query.innerJoin(CrmContractDO.class, on -> on.like(CrmContractDO::getNo, reqVO.getContractNo())
                    .eq(CrmContractDO::getId, CrmReceivablePlanDO::getContractId));
        }
        query.eq(CrmReceivablePlanDO::getCustomerId, reqVO.getCustomerId()) // 必须传递
                .eqIfPresent(CrmReceivablePlanDO::getContractId, reqVO.getContractId())
                .orderByDesc(CrmReceivablePlanDO::getPeriod);
        return selectJoinPage(reqVO, CrmReceivablePlanDO.class, query);
    }

    default PageResult<CrmReceivablePlanDO> selectPage(CrmReceivablePlanPageReqVO pageReqVO, Long userId) {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE_PLAN.getType(),
                CrmReceivablePlanDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmReceivablePlanDO.class)
                .eqIfPresent(CrmReceivablePlanDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmReceivablePlanDO::getContractId, pageReqVO.getContractId())
                .orderByDesc(CrmReceivablePlanDO::getPeriod);
        if (Objects.nonNull(pageReqVO.getContractNo())) { // 根据合同编号检索
            query.innerJoin(CrmContractDO.class, on -> on.like(CrmContractDO::getNo, pageReqVO.getContractNo())
                    .eq(CrmContractDO::getId, CrmReceivablePlanDO::getContractId));
        }

        // Backlog: 回款提醒类型
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        if (pageReqVO.getRemindType() != null) {
            query.leftJoin(CrmReceivableDO.class,
                    on -> on.eq(CrmReceivableDO::getId, CrmReceivablePlanDO::getReceivableId));
        }
        if (CrmReceivablePlanPageReqVO.REMIND_TYPE_NEEDED.equals(pageReqVO.getRemindType())) { // 待回款
            // 查询条件：未生效回款 + 提醒时间 <= 今天
            appendPendingReminderCondition(query, beginOfToday);
        } else if (CrmReceivablePlanPageReqVO.REMIND_TYPE_EXPIRED.equals(pageReqVO.getRemindType())) { // 已逾期
            // 查询条件：未生效回款 + 计划回款日早于今天
            appendOverdueCondition(query, beginOfToday);
        } else if (CrmReceivablePlanPageReqVO.REMIND_TYPE_RECEIVED.equals(pageReqVO.getRemindType())) { // 已回款
            appendReceivedCondition(query);
        }
        return selectJoinPage(pageReqVO, CrmReceivablePlanDO.class, query);
    }

    static void appendPendingReminderCondition(MPJLambdaWrapperX<CrmReceivablePlanDO> query,
                                               LocalDateTime beginOfToday) {
        appendNotReceivedCondition(query);
        query.le(CrmReceivablePlanDO::getRemindTime, beginOfToday);
    }

    static void appendOverdueCondition(MPJLambdaWrapperX<CrmReceivablePlanDO> query,
                                       LocalDateTime beginOfToday) {
        appendNotReceivedCondition(query);
        query.lt(CrmReceivablePlanDO::getReturnTime, beginOfToday);
    }

    static void appendReceivedCondition(MPJLambdaWrapperX<CrmReceivablePlanDO> query) {
        query.eq(CrmReceivableDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus());
    }

    private static void appendNotReceivedCondition(
            MPJLambdaWrapperX<CrmReceivablePlanDO> query) {
        query.and(condition -> condition.isNull(CrmReceivableDO::getAuditStatus)
                .or().ne(CrmReceivableDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus()));
    }

    default Long selectReceivablePlanCountByRemind(Long userId) {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE_PLAN.getType(),
                CrmReceivablePlanDO::getId, userId, CrmSceneTypeEnum.OWNER.getType());
        // 未生效回款 + 已到提醒日；与待办默认列表保持同一口径
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        query.leftJoin(CrmReceivableDO.class,
                on -> on.eq(CrmReceivableDO::getId, CrmReceivablePlanDO::getReceivableId));
        appendPendingReminderCondition(query, beginOfToday);
        return selectCount(query);
    }

}
