package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CrmMarketingBroadcastSaveReqVO {
    private Long id;
    private Long campaignId;
    @NotBlank private String name;
    @NotNull private Integer channel;
    private String smsTemplateCode;
    private String mailTemplateCode;
    private String templateParams;
    private LocalDateTime scheduledAt;
    private List<@Positive Long> customerIds = new ArrayList<>();
    private List<@Positive Long> contactIds = new ArrayList<>();
    @Valid
    @Size(max = 50)
    private List<CrmMarketingLinkSaveReqVO> links = new ArrayList<>();
}
