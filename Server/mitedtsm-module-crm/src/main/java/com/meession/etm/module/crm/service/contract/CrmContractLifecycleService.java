package com.meession.etm.module.crm.service.contract;

import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractAttachmentCreateReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractSignReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractSignVoidReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAttachmentDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractChangeRecordDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractSigningDO;

import java.util.List;

public interface CrmContractLifecycleService {
    Long createAttachment(CrmContractAttachmentCreateReqVO req, Long userId);
    void deleteAttachment(Long contractId, Long attachmentId);
    Long sign(CrmContractSignReqVO req, Long userId);
    void voidSign(CrmContractSignVoidReqVO req, Long userId);
    CrmContractSigningDO getSigning(Long contractId);
    List<CrmContractAttachmentDO> getAttachments(Long contractId);
    List<CrmContractChangeRecordDO> getChangeRecords(Long contractId);

    List<Integer> getSupportedSignMethods();

    int getCurrentVersion(Long contractId);

    void recordChange(Long contractId, Integer actionType, Integer contractVersion, Long userId, String reason);
}
