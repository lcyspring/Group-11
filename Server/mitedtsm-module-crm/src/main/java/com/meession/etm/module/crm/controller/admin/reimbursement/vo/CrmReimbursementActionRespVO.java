package com.meession.etm.module.crm.controller.admin.reimbursement.vo;

import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmReimbursementActionRecordDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CrmReimbursementActionRespVO extends CrmReimbursementActionRecordDO {
    private String operatorUserName;
}
