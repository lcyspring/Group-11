package com.meession.etm.module.crm.controller.admin.visit.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CrmCustomerVisitPageReqVO extends PageParam {
    private Long customerId;
    private Integer auditStatus;
    private Integer resultStatus;
}
