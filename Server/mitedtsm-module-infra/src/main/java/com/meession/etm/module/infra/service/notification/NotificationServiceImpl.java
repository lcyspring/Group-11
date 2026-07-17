package com.meession.etm.module.infra.service.notification;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.security.core.util.SecurityFrameworkUtils;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationCreateReqVO;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationPageReqVO;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationSendReqVO;
import com.meession.etm.module.infra.dal.dataobject.notification.NotificationDO;
import com.meession.etm.module.infra.dal.mysql.notification.NotificationMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.infra.enums.ErrorCodeConstants.NOTIFICATION_NOT_EXISTS;

/**
 * 消息通知 Service 实现类
 *
 * @author 密讯
 */
@Service
@Slf4j
@Validated
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Override
    public Long createNotification(NotificationCreateReqVO createReqVO) {
        // 插入消息通知
        NotificationDO notification = BeanUtils.toBean(createReqVO, NotificationDO.class);
        notification.setReadStatus(false); // 默认未读
        notification.setSenderUserId(SecurityFrameworkUtils.getLoginUserId()); // 设置发送人为当前登录用户
        notificationMapper.insert(notification);
        return notification.getId();
    }

    @Override
    public void updateNotificationReadStatus(Long id, Boolean readStatus) {
        // 校验存在
        validateNotificationExists(id);
        // 更新已读状态
        NotificationDO updateObj = new NotificationDO();
        updateObj.setId(id);
        updateObj.setReadStatus(readStatus);
        notificationMapper.updateById(updateObj);
    }

    @Override
    public NotificationDO getNotification(Long id) {
        return notificationMapper.selectById(id);
    }

    @Override
    public PageResult<NotificationDO> getNotificationPage(NotificationPageReqVO reqVO) {
        return notificationMapper.selectPage(reqVO);
    }

    @Override
    public Long getUnreadCount(Long receiverUserId) {
        return notificationMapper.selectUnreadCount(receiverUserId);
    }

    @Override
    public void sendNotification(NotificationSendReqVO sendReqVO) {
        Long senderUserId = SecurityFrameworkUtils.getLoginUserId();
        // 批量构建消息通知
        List<NotificationDO> notifications = sendReqVO.getReceiverUserIds().stream()
                .map(receiverUserId -> {
                    NotificationDO notification = BeanUtils.toBean(sendReqVO, NotificationDO.class);
                    notification.setReceiverUserId(receiverUserId);
                    notification.setSenderUserId(senderUserId);
                    notification.setReadStatus(false); // 默认未读
                    return notification;
                })
                .toList();
        // 批量插入
        notificationMapper.insertBatch(notifications);
    }

    private void validateNotificationExists(Long id) {
        if (id == null) {
            return;
        }
        NotificationDO notification = notificationMapper.selectById(id);
        if (Objects.isNull(notification)) {
            throw exception(NOTIFICATION_NOT_EXISTS);
        }
    }

}
