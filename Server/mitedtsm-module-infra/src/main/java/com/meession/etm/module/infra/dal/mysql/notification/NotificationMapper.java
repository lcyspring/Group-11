package com.meession.etm.module.infra.dal.mysql.notification;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationPageReqVO;
import com.meession.etm.module.infra.dal.dataobject.notification.NotificationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapperX<NotificationDO> {

    default PageResult<NotificationDO> selectPage(NotificationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<NotificationDO>()
                .eqIfPresent(NotificationDO::getType, reqVO.getType())
                .eqIfPresent(NotificationDO::getReadStatus, reqVO.getReadStatus())
                .likeIfPresent(NotificationDO::getTitle, reqVO.getTitle())
                .orderByDesc(NotificationDO::getId));
    }

    default Long selectUnreadCount(Long receiverUserId) {
        return selectCount(new LambdaQueryWrapperX<NotificationDO>()
                .eqIfPresent(NotificationDO::getReceiverUserId, receiverUserId)
                .eqIfPresent(NotificationDO::getReadStatus, false));
    }

}
