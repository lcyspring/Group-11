package com.meession.etm.module.crm.service.refund;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.refund.vo.CrmReceivableRefundPageReqVO;
import com.meession.etm.module.crm.controller.admin.refund.vo.CrmReceivableRefundSaveReqVO;
import com.meession.etm.module.crm.controller.admin.refund.vo.CrmReceivableRefundSourceSummaryRespVO;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundDO;

import java.util.List;

public interface CrmReceivableRefundService {
    Long createRefund(CrmReceivableRefundSaveReqVO reqVO, Long userId);
    void updateRefund(CrmReceivableRefundSaveReqVO reqVO, Long userId);
    void deleteRefund(Long id, Long userId);
    void submitRefund(Long id, Long userId);
    void updateRefundAuditStatus(Long id, String processInstanceId, Integer bpmResult);
    CrmReceivableRefundDO getRefund(Long id);
    PageResult<CrmReceivableRefundDO> getRefundPage(CrmReceivableRefundPageReqVO reqVO, Long userId);
    CrmReceivableRefundSourceSummaryRespVO getSourceSummary(Long receivableId, Long excludeRefundId);
    List<CrmReceivableRefundActionRecordDO> getActionRecords(Long refundId);
}
