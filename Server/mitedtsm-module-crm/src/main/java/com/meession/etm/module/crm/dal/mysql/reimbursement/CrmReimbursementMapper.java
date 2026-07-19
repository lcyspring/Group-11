package com.meession.etm.module.crm.dal.mysql.reimbursement;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmReimbursementPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmReimbursementDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrmReimbursementMapper extends BaseMapperX<CrmReimbursementDO> {
    default CrmReimbursementDO selectByNo(String no) {
        return selectOne(CrmReimbursementDO::getNo, no);
    }

    default CrmReimbursementDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<CrmReimbursementDO>()
                .eq(CrmReimbursementDO::getId, id).last("FOR UPDATE"));
    }

    default int updateAuditStatusIfProcessing(Long id, String processInstanceId, Integer auditStatus) {
        return update(new LambdaUpdateWrapper<CrmReimbursementDO>()
                .eq(CrmReimbursementDO::getId, id)
                .eq(CrmReimbursementDO::getProcessInstanceId, processInstanceId)
                .eq(CrmReimbursementDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus())
                .set(CrmReimbursementDO::getAuditStatus, auditStatus));
    }

    default int updateContentIfVersion(CrmReimbursementDO update, Integer expectedVersion) {
        return update(new LambdaUpdateWrapper<CrmReimbursementDO>()
                .eq(CrmReimbursementDO::getId, update.getId())
                .eq(CrmReimbursementDO::getVersion, expectedVersion)
                .set(CrmReimbursementDO::getCustomerId, update.getCustomerId())
                .set(CrmReimbursementDO::getContractId, update.getContractId())
                .set(CrmReimbursementDO::getTotalAmount, update.getTotalAmount())
                .set(CrmReimbursementDO::getExpenseStartDate, update.getExpenseStartDate())
                .set(CrmReimbursementDO::getExpenseEndDate, update.getExpenseEndDate())
                .set(CrmReimbursementDO::getReason, update.getReason())
                .set(CrmReimbursementDO::getRemark, update.getRemark())
                .set(CrmReimbursementDO::getAuditStatus, update.getAuditStatus())
                .set(CrmReimbursementDO::getVersion, expectedVersion + 1));
    }

    default int submitIfDraftAndVersion(Long id, Integer expectedVersion, String processInstanceId) {
        return update(new LambdaUpdateWrapper<CrmReimbursementDO>()
                .eq(CrmReimbursementDO::getId, id)
                .eq(CrmReimbursementDO::getVersion, expectedVersion)
                .eq(CrmReimbursementDO::getAuditStatus, CrmAuditStatusEnum.DRAFT.getStatus())
                .set(CrmReimbursementDO::getProcessInstanceId, processInstanceId)
                .set(CrmReimbursementDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus())
                .set(CrmReimbursementDO::getVersion, expectedVersion + 1));
    }

    default PageResult<CrmReimbursementDO> selectPage(CrmReimbursementPageReqVO reqVO, Long userId) {
        MPJLambdaWrapperX<CrmReimbursementDO> query = new MPJLambdaWrapperX<>();
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_REIMBURSEMENT.getType(),
                CrmReimbursementDO::getId, userId, reqVO.getSceneType());
        query.selectAll(CrmReimbursementDO.class)
                .likeIfPresent(CrmReimbursementDO::getNo, reqVO.getNo())
                .eqIfPresent(CrmReimbursementDO::getApplicantUserId, reqVO.getApplicantUserId())
                .eqIfPresent(CrmReimbursementDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(CrmReimbursementDO::getContractId, reqVO.getContractId())
                .eqIfPresent(CrmReimbursementDO::getAuditStatus, reqVO.getAuditStatus())
                .betweenIfPresent(CrmReimbursementDO::getExpenseStartDate, reqVO.getExpenseDate())
                .orderByDesc(CrmReimbursementDO::getId);
        return selectJoinPage(reqVO, CrmReimbursementDO.class, query);
    }
}
