package com.meession.etm.module.infra.service.notification;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationCreateReqVO;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationPageReqVO;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationSendReqVO;
import com.meession.etm.module.infra.dal.dataobject.notification.NotificationDO;
import jakarta.validation.Valid;

/**
 * 消息通知 Service 接口
 *
 * @author 密讯
 */
public interface NotificationService {

    /**
     * 创建消息通知
     *
     * @param createReqVO 创建信息
     * @return 通知编号
     */
    Long createNotification(@Valid NotificationCreateReqVO createReqVO);

    /**
     * 更新消息通知已读状态
     *
     * @param id         通知编号
     * @param readStatus 已读状态
     */
    void updateNotificationReadStatus(Long id, Boolean readStatus);

    /**
     * 获得消息通知
     *
     * @param id 通知编号
     * @return 消息通知
     */
    NotificationDO getNotification(Long id);

    /**
     * 获得消息通知分页列表
     *
     * @param reqVO 分页条件
     * @return 分页列表
     */
    PageResult<NotificationDO> getNotificationPage(NotificationPageReqVO reqVO);

    /**
     * 获取未读消息数量
     *
     * @param receiverUserId 接收人用户编号
     * @return 未读数量
     */
    Long getUnreadCount(Long receiverUserId);

    /**
     * 批量发送消息通知
     *
     * @param sendReqVO 发送信息
     */
    void sendNotification(@Valid NotificationSendReqVO sendReqVO);

}
