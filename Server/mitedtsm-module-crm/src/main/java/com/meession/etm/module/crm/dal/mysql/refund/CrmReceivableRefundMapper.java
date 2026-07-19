package com.meession.etm.module.crm.dal.mysql.refund;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.refund.vo.CrmReceivableRefundPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CrmReceivableRefundMapper extends BaseMapperX<CrmReceivableRefundDO> {

    default CrmReceivableRefundDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<CrmReceivableRefundDO>()
                .eq(CrmReceivableRefundDO::getId, id).last("FOR UPDATE"));
    }

    default CrmReceivableRefundDO selectByNo(String no) {
        return selectOne(CrmReceivableRefundDO::getNo, no);
    }

    default List<CrmReceivableRefundDO> selectListByReceivableIdAndStatuses(
            Long receivableId, Collection<Integer> statuses) {
        return selectList(new LambdaQueryWrapperX<CrmReceivableRefundDO>()
                .eq(CrmReceivableRefundDO::getReceivableId, receivableId)
                .in(CrmReceivableRefundDO::getAuditStatus, statuses));
    }

    default int updateAuditStatusIfProcessing(Long id, String processInstanceId, Integer auditStatus) {
        return update(new LambdaUpdateWrapper<CrmReceivableRefundDO>()
                .eq(CrmReceivableRefundDO::getId, id)
                .eq(CrmReceivableRefundDO::getProcessInstanceId, processInstanceId)
                .eq(CrmReceivableRefundDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus())
                .set(CrmReceivableRefundDO::getAuditStatus, auditStatus));
    }

    default PageResult<CrmReceivableRefundDO> selectPage(CrmReceivableRefundPageReqVO reqVO, Long userId) {
        MPJLambdaWrapperX<CrmReceivableRefundDO> query = new MPJLambdaWrapperX<>();
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE_REFUND.getType(),
                CrmReceivableRefundDO::getId, userId, reqVO.getSceneType());
        query.selectAll(CrmReceivableRefundDO.class)
                .likeIfPresent(CrmReceivableRefundDO::getNo, reqVO.getNo())
                .eqIfPresent(CrmReceivableRefundDO::getReceivableId, reqVO.getReceivableId())
                .eqIfPresent(CrmReceivableRefundDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(CrmReceivableRefundDO::getContractId, reqVO.getContractId())
                .eqIfPresent(CrmReceivableRefundDO::getType, reqVO.getType())
                .eqIfPresent(CrmReceivableRefundDO::getAuditStatus, reqVO.getAuditStatus())
                .betweenIfPresent(CrmReceivableRefundDO::getRefundTime, reqVO.getRefundTime())
                .orderByDesc(CrmReceivableRefundDO::getId);
        return selectJoinPage(reqVO, CrmReceivableRefundDO.class, query);
    }
}
