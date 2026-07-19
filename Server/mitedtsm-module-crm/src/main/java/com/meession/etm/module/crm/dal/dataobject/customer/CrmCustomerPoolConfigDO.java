package com.meession.etm.module.crm.dal.dataobject.customer;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

/**
 * 客户公海配置 DO
 *
 * @author Wanwan
 */
@TableName(value = "crm_customer_pool_config")
@KeySequence("crm_customer_pool_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerPoolConfigDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 是否启用客户公海
     */
    private Boolean enabled;
    /**
     * 未跟进放入公海天数
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer contactExpireDays;
    /**
     * 未成交放入公海天数
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer dealExpireDays;
    /**
     * 是否开启提前提醒
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Boolean notifyEnabled;
    /**
     * 提前提醒天数
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer notifyDays;

    /** 每日自助领取上限。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer dailyClaimLimit;
    /** 同一用户重复领取同一客户的冷却天数。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer repeatClaimCooldownDays;
    /** 重点客户等级阈值。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer highValueLevelThreshold;
    /** 重点客户保护期倍数。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer highValueExpireMultiplier;
    /** 是否保护存在活跃商机的客户。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Boolean protectActiveBusiness;
    /** 是否保护存在未完结销售单据的客户。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Boolean protectActiveContract;
    /** 自动回收单批最大数量。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer autoPoolBatchSize;

}
