package com.meession.etm.module.crm.controller.admin.fulfillment.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmErpCustomerMappingRespVO {

    private Long id;
    private Long crmCustomerId;
    private String crmCustomerName;
    private Long erpCustomerId;
    private String erpCustomerName;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
