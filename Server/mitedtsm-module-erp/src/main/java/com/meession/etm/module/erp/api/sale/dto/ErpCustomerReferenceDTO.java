package com.meession.etm.module.erp.api.sale.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErpCustomerReferenceDTO {

    private Long id;
    private String name;
    private Integer status;
}
