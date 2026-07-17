package com.meession.etm.module.infra.dal.dataobject.notification;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.module.infra.enums.notification.NotificationTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 消息通知表
 *
 * @author 密讯
 */
@TableName("infra_notification")
@KeySequence("infra_notification_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NotificationDO extends BaseDO {

    /**
     * 通知主键
     */
    @TableId
    private Long id;
    /**
     * 通知类型
     *
     * 枚举 {@link NotificationTypeEnum}
     */
    private Integer type;
    /**
     * 通知标题
     */
    private String title;
    /**
     * 通知内容
     */
    private String content;
    /**
     * 接收人用户编号
     */
    private Long receiverUserId;
    /**
     * 发送人用户编号
     */
    private Long senderUserId;
    /**
     * 是否已读
     */
    private Boolean readStatus;

}
