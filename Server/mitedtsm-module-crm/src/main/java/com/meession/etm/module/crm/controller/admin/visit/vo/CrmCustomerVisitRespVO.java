package com.meession.etm.module.crm.controller.admin.visit.vo;

import com.meession.etm.module.crm.dal.dataobject.visit.CrmCustomerVisitDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CrmCustomerVisitRespVO extends CrmCustomerVisitDO {
    private String customerName;
    private String contactName;
}
