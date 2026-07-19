package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 群发发送前置条件检查结果。
 * <p>
 * {@code ready} 表示任务可以进入发送阶段；record-only 模式不会阻止留痕发送，
 * 但通过 {@code realDeliveryEnabled} 明确告知调用方不会连接外部服务商。
 */
@Data
@Accessors(chain = true)
public class CrmMarketingSendReadinessRespVO {
    private boolean ready;
    private boolean realDeliveryEnabled;
    private String providerMode;
    private Integer validRecipientCount;
    private Integer validEmailRecipientCount;
    private Integer suppressedEmailRecipientCount;
    private boolean mailTemplateConfigured;
    private boolean mailTemplateEnabled;
    private boolean mailAccountConfigured;
    private List<String> missingTemplateParams = new ArrayList<>();
    private List<String> problems = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
