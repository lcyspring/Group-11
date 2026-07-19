package com.meession.etm.module.crm.controller.admin.fulfillment.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmErpProductMappingRespVO {

    private Long id;
    private Long crmProductId;
    private String crmProductName;
    private String crmProductNo;
    private Long erpProductId;
    private String erpProductName;
    private String erpProductBarCode;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
