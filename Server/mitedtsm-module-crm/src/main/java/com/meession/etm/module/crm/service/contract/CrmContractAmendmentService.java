package com.meession.etm.module.crm.service.contract;

import com.meession.etm.module.crm.controller.admin.contract.vo.amendment.CrmContractAmendmentSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAmendmentDO;

import java.util.List;

public interface CrmContractAmendmentService {

    Long createAmendment(CrmContractAmendmentSaveReqVO req, Long userId);

    void updateAmendment(CrmContractAmendmentSaveReqVO req, Long userId);

    void submitAmendment(Long contractId, Long id, Long userId);

    void updateAuditStatus(Long id, String processInstanceId, Integer bpmResult);

    CrmContractAmendmentDO getAmendment(Long contractId, Long id);

    List<CrmContractAmendmentDO> getAmendmentList(Long contractId);
}
