package com.meession.etm.module.crm.dal.mysql.receivable;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.receivable.vo.receivable.CrmReceivablePageReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.enums.receivable.CrmReceivableReferenceStatusEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * 回款 Mapper
 *
 * @author 赤焰
 */
@Mapper
public interface CrmReceivableMapper extends BaseMapperX<CrmReceivableDO> {

    /**
     * 锁定合同，串行化同一合同下的回款提交，避免并发提交导致回款金额超过合同金额。
     */
    @Select("SELECT id FROM crm_contract WHERE id = #{contractId} AND deleted = 0 FOR UPDATE")
    Long selectContractIdForUpdate(Long contractId);

    default CrmReceivableDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<CrmReceivableDO>()
                .eq(CrmReceivableDO::getId, id)
                .last("FOR UPDATE"));
    }

    default int updateAuditStatusIfProcessing(Long id, String processInstanceId, Integer auditStatus) {
        return update(new LambdaUpdateWrapper<CrmReceivableDO>()
                .eq(CrmReceivableDO::getId, id)
                .eq(CrmReceivableDO::getProcessInstanceId, processInstanceId)
                .eq(CrmReceivableDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus())
                .set(CrmReceivableDO::getAuditStatus, auditStatus));
    }

    default CrmReceivableDO selectByNo(String no) {
        return selectOne(CrmReceivableDO::getNo, no);
    }

    default PageResult<CrmReceivableDO> selectPageByCustomerId(CrmReceivablePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CrmReceivableDO>()
                .eq(CrmReceivableDO::getCustomerId, reqVO.getCustomerId()) // 必须传递
                .eqIfPresent(CrmReceivableDO::getNo, reqVO.getNo())
                .eqIfPresent(CrmReceivableDO::getContractId, reqVO.getContractId())
                .eqIfPresent(CrmReceivableDO::getPlanId, reqVO.getPlanId())
                .orderByDesc(CrmReceivableDO::getId));
    }

    default PageResult<CrmReceivableDO> selectPage(CrmReceivablePageReqVO pageReqVO, Long userId) {
        MPJLambdaWrapperX<CrmReceivableDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE.getType(),
                CrmReceivableDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmReceivableDO.class);
        appendPageFilter(query, pageReqVO);
        return selectJoinPage(pageReqVO, CrmReceivableDO.class, query);
    }

    static void appendPageFilter(MPJLambdaWrapperX<CrmReceivableDO> query,
                                 CrmReceivablePageReqVO pageReqVO) {
        query.eqIfPresent(CrmReceivableDO::getNo, pageReqVO.getNo())
                .eqIfPresent(CrmReceivableDO::getPlanId, pageReqVO.getPlanId())
                .eqIfPresent(CrmReceivableDO::getContractId, pageReqVO.getContractId())
                .eqIfPresent(CrmReceivableDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmReceivableDO::getAuditStatus, pageReqVO.getAuditStatus());
        appendReferenceStatusFilter(query, pageReqVO.getReferenceStatus());
        query.orderByDesc(CrmReceivableDO::getId);
    }

    static void appendReferenceStatusFilter(MPJLambdaWrapperX<CrmReceivableDO> query, Integer referenceStatus) {
        if (referenceStatus == null) {
            return;
        }
        query.leftJoin(CrmCustomerDO.class,
                        on -> on.eq(CrmCustomerDO::getId, CrmReceivableDO::getCustomerId))
                .leftJoin(CrmContractDO.class,
                        on -> on.eq(CrmContractDO::getId, CrmReceivableDO::getContractId)
                                .eq(CrmContractDO::getCustomerId, CrmReceivableDO::getCustomerId));
        if (CrmReceivableReferenceStatusEnum.VALID.getStatus().equals(referenceStatus)) {
            query.isNotNull(CrmCustomerDO::getId).isNotNull(CrmContractDO::getId);
        } else if (CrmReceivableReferenceStatusEnum.CUSTOMER_MISSING.getStatus().equals(referenceStatus)) {
            query.isNull(CrmCustomerDO::getId).isNotNull(CrmContractDO::getId);
        } else if (CrmReceivableReferenceStatusEnum.CONTRACT_INVALID.getStatus().equals(referenceStatus)) {
            query.isNotNull(CrmCustomerDO::getId).isNull(CrmContractDO::getId);
        } else if (CrmReceivableReferenceStatusEnum.BOTH_INVALID.getStatus().equals(referenceStatus)) {
            query.isNull(CrmCustomerDO::getId).isNull(CrmContractDO::getId);
        }
    }

    default Long selectCountByAudit(Long userId) {
        MPJLambdaWrapperX<CrmReceivableDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE.getType(),
                CrmReceivableDO::getId, userId, CrmSceneTypeEnum.OWNER.getType());
        // 未审核
        query.eq(CrmReceivableDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus());
        return selectCount(query);
    }

    default List<CrmReceivableDO> selectListByContractIdAndStatus(Long contractId, Collection<Integer> auditStatuses) {
        return selectList(new LambdaQueryWrapperX<CrmReceivableDO>()
                .eq(CrmReceivableDO::getContractId, contractId)
                .in(CrmReceivableDO::getAuditStatus, auditStatuses));
    }

    default Map<Long, BigDecimal> selectReceivablePriceMapByContractId(Collection<Long> contractIds) {
        if (CollUtil.isEmpty(contractIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<CrmReceivableDO>()
                .select("contract_id, SUM(price) AS total_price")
                .eq("audit_status", CrmAuditStatusEnum.APPROVE.getStatus())
                .groupBy("contract_id")
                .in("contract_id", contractIds));
        // 获得金额
        return convertMap(result, obj -> (Long) obj.get("contract_id"), obj -> (BigDecimal) obj.get("total_price"));
    }

    default Long selectCountByContractId(Long contractId) {
        return selectCount(CrmReceivableDO::getContractId, contractId);
    }

}
