package com.meession.etm.module.crm.controller.admin.customer.vo.pool;

import com.meession.etm.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmCustomerPoolLogPageReqVO extends PageParam {

    private Long customerId;

    private String customerName;

    private Integer operationType;

    private Long operationUserId;

    private LocalDateTime[] createTime;

}
