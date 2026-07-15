package com.meession.etm.module.crm.controller.admin.customer.vo.garbage;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - CRM 客户垃圾池分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmCustomerGarbagePageReqVO extends PageParam {

    @Schema(description = "客户名称")
    private String name;
    @Schema(description = "手机")
    private String mobile;
    @Schema(description = "客户等级")
    private Integer level;
    @Schema(description = "客户来源")
    private Integer source;
}
