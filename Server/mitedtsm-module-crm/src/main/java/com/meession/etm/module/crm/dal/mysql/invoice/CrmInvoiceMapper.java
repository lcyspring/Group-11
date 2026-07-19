package com.meession.etm.module.crm.dal.mysql.invoice;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.invoice.vo.CrmInvoicePageReqVO;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.invoice.CrmInvoiceStatusEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface CrmInvoiceMapper extends BaseMapperX<CrmInvoiceDO> {

    default CrmInvoiceDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<CrmInvoiceDO>()
                .eq(CrmInvoiceDO::getId, id).last("FOR UPDATE"));
    }

    default CrmInvoiceDO selectByNo(String no) {
        return selectOne(CrmInvoiceDO::getNo, no);
    }

    default CrmInvoiceDO selectByInvoiceNo(String invoiceNo) {
        return selectOne(CrmInvoiceDO::getInvoiceNo, invoiceNo);
    }

    default List<CrmInvoiceDO> selectEffectiveListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<CrmInvoiceDO>()
                .eq(CrmInvoiceDO::getContractId, contractId)
                .ne(CrmInvoiceDO::getStatus, CrmInvoiceStatusEnum.DRAFT.getStatus())
                .ne(CrmInvoiceDO::getStatus, CrmInvoiceStatusEnum.VOIDED.getStatus()));
    }

    default List<CrmInvoiceDO> selectActiveRedList(Long originalInvoiceId) {
        return selectList(new LambdaQueryWrapperX<CrmInvoiceDO>()
                .eq(CrmInvoiceDO::getOriginalInvoiceId, originalInvoiceId)
                .ne(CrmInvoiceDO::getStatus, CrmInvoiceStatusEnum.VOIDED.getStatus()));
    }

    default List<CrmInvoiceDO> selectListByIds(Collection<Long> ids) {
        return ids == null || ids.isEmpty() ? Collections.emptyList() : selectByIds(ids);
    }

    default PageResult<CrmInvoiceDO> selectPage(CrmInvoicePageReqVO reqVO, Long userId) {
        MPJLambdaWrapperX<CrmInvoiceDO> query = new MPJLambdaWrapperX<>();
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_INVOICE.getType(),
                CrmInvoiceDO::getId, userId, reqVO.getSceneType());
        query.selectAll(CrmInvoiceDO.class)
                .likeIfPresent(CrmInvoiceDO::getNo, reqVO.getNo())
                .likeIfPresent(CrmInvoiceDO::getInvoiceNo, reqVO.getInvoiceNo())
                .eqIfPresent(CrmInvoiceDO::getContractId, reqVO.getContractId())
                .eqIfPresent(CrmInvoiceDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(CrmInvoiceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CrmInvoiceDO::getType, reqVO.getType())
                .eqIfPresent(CrmInvoiceDO::getDirection, reqVO.getDirection())
                .betweenIfPresent(CrmInvoiceDO::getInvoiceDate, reqVO.getInvoiceDate())
                .orderByDesc(CrmInvoiceDO::getId);
        return selectJoinPage(reqVO, CrmInvoiceDO.class, query);
    }
}
