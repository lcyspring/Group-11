package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

@Data
public class CrmMarketingBroadcastRespVO {
    private Long id; private Long campaignId; private String name; private Integer channel;
    private String smsTemplateCode; private String mailTemplateCode; private String templateParams; private Integer status;
    private Integer totalCount; private Integer validCount; private Integer suppressedCount;
    private Integer sentCount; private Integer failedCount; private Long reviewerUserId;
    private LocalDateTime reviewedAt; private String reviewComment; private LocalDateTime scheduledAt; private LocalDateTime sentAt;
    private String processInstanceId;
    private Long creatorUserId; private List<Long> customerIds; private List<Long> contactIds;
    private List<CrmMarketingLinkSaveReqVO> links;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}
