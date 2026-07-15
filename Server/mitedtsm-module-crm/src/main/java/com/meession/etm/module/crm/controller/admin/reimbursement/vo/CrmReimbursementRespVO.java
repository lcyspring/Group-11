package com.meession.etm.module.crm.controller.admin.reimbursement.vo;

import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmReimbursementDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CrmReimbursementRespVO extends CrmReimbursementDO {
    private String applicantUserName;
    private String customerName;
    private String contractNo;
    private String contractName;
    private List<CrmReimbursementItemRespVO> items;
}
