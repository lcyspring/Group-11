package com.meession.etm.module.erp.api.sale.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErpProductReferenceDTO {

    private Long id;
    private String name;
    private String barCode;
    private Long unitId;
    private Integer status;
}
