package com.meession.etm.module.marketing.dal.dataobject.log;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 营销发送记录关联 DO
 * <p>
 * 极简关联表，campaign_id + system_log_id + channel，
 * 不复制 system_sms_log / system_mail_log 的任何字段。
 *
 * @author MITEDTSM
 */
@TableName("marketing_send_record")
@KeySequence("marketing_send_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class MarketingSendRecordDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 营销活动编号
     */
    private Long campaignId;

    /**
     * 渠道：SMS / MAIL
     */
    private String channel;

    /**
     * system_sms_log.id 或 system_mail_log.id
     */
    private Long systemLogId;

}
