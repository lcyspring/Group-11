package com.meession.etm.module.crm.service.business;

import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessStatusRecordDO;

import java.util.List;

public interface CrmBusinessStatusRecordService {

    void createRecord(CrmBusinessDO oldBusiness, CrmBusinessDO newBusiness, Long operatorId, String remark);

    List<CrmBusinessStatusRecordDO> getRecordListByBusinessId(Long businessId);

}