package com.meession.etm.module.crm.controller.admin.marketing.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class CrmMarketingBroadcastPageReqVO extends PageParam {
    private String name;
    private Integer channel;
    private Integer status;
    private Long campaignId;
}
