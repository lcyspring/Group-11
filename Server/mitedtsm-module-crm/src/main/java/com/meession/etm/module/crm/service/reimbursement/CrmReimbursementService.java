package com.meession.etm.module.crm.service.reimbursement;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmReimbursementPageReqVO;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmReimbursementSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmReimbursementActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmReimbursementDO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmReimbursementItemDO;

import java.util.List;

public interface CrmReimbursementService {
    String uploadAttachmentFile(Long reimbursementId, byte[] content, String fileName, String contentType);
    Long createReimbursement(CrmReimbursementSaveReqVO reqVO, Long userId);
    void updateReimbursement(CrmReimbursementSaveReqVO reqVO, Long userId);
    void deleteReimbursement(Long id, Long userId);
    void submitReimbursement(Long id, Long userId);
    void updateAuditStatus(Long id, String processInstanceId, Integer bpmResult);
    CrmReimbursementDO getReimbursement(Long id);
    PageResult<CrmReimbursementDO> getReimbursementPage(CrmReimbursementPageReqVO reqVO, Long userId);
    List<CrmReimbursementItemDO> getItems(Long reimbursementId);
    List<CrmReimbursementActionRecordDO> getActionRecords(Long reimbursementId);
}
