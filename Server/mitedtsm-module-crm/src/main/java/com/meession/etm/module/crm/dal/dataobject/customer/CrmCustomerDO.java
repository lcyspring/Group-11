package com.meession.etm.module.crm.dal.dataobject.customer;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.module.crm.enums.DictTypeConstants;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CRM 客户 DO
 *
 * @author Wanwan
 */
@TableName(value = "crm_customer")
@KeySequence("crm_customer_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 客户名称
     */
    private String name;
    /**
     * 上级客户编号
     *
     * 关联 {@link CrmCustomerDO#getId()} 字段
     */
    private Long parentCustomerId;

    /**
     * 跟进状态
     */
    private Boolean followUpStatus;
    /**
     * 最后跟进时间
     */
    private LocalDateTime contactLastTime;
    /**
     * 最后跟进内容
     */
    private String contactLastContent;
    /**
     * 下次联系时间
     */
    private LocalDateTime contactNextTime;

    /**
     * 负责人的用户编号
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long ownerUserId;
    /**
     * 成为负责人的时间
     */
    private LocalDateTime ownerTime;

    /** Current pool state: 0 owned, 1 public pool, 2 garbage pool. */
    private Integer poolStatus;
    /** Time of the current public/garbage pool entry. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime poolEntryTime;
    /** Owner immediately before the current pool entry. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long poolPreviousOwnerUserId;
    /** Machine-readable reason for the current pool entry. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String poolReason;
    /** Number of times the customer has entered the public pool. */
    private Integer poolCycleCount;
    /** Time at which the customer entered the garbage pool. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime garbageTime;
    /** Required reason for the current garbage-pool state. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String garbageReason;

    /**
     * 锁定状态
     */
    private Boolean lockStatus;
    /**
     * 成交状态
     */
    private Boolean dealStatus;
    /**
     * 客户生命周期状态：10 潜在、20 意向、30 成交、40 流失。
     */
    private Integer lifecycleStatus;
    /** 生命周期状态最后变更时间。 */
    private LocalDateTime lifecycleStatusChangeTime;
    /** 当前为流失客户时的流失原因。 */
    private String lifecycleLostReason;

    /**
     * 手机
     */
    private String mobile;
    /**
     * 电话
     */
    private String telephone;
    /**
     * QQ
     */
    private String qq;
    /**
     * wechat
     */
    private String wechat;
    /**
     * email
     */
    private String email;
    /**
     * 所在地
     *
     * 关联 {@link com.meession.etm.framework.ip.core.Area#getId()} 字段
     */
    private Integer areaId;
    /**
     * 详细地址
     */
    private String detailAddress;
    /**
     * 所属行业
     *
     * 对应字典 {@link DictTypeConstants#CRM_CUSTOMER_INDUSTRY}
     */
    private Integer industryId;
    /**
     * 客户等级
     *
     * 对应字典 {@link DictTypeConstants#CRM_CUSTOMER_LEVEL}
     */
    private Integer level;
    /**
     * 客户来源
     *
     * 对应字典 {@link DictTypeConstants#CRM_CUSTOMER_SOURCE}
     */
    private Integer source;
    /**
     * 备注
     */
    private String remark;

}
