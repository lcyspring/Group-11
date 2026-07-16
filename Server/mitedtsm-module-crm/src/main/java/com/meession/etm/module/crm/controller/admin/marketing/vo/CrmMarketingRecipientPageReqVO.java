package com.meession.etm.module.crm.controller.admin.marketing.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class CrmMarketingRecipientPageReqVO extends PageParam {
    @NotNull private Long broadcastId;
    private Integer status;
}
