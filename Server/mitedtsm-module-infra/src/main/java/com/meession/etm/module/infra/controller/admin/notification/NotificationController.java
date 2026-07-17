package com.meession.etm.module.infra.controller.admin.notification;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationCreateReqVO;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationPageReqVO;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationRespVO;
import com.meession.etm.module.infra.controller.admin.notification.vo.NotificationSendReqVO;
import com.meession.etm.module.infra.dal.dataobject.notification.NotificationDO;
import com.meession.etm.module.infra.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 消息通知")
@RestController
@RequestMapping("/infra/notification")
@Validated
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @PostMapping("/create")
    @Operation(summary = "创建消息通知")
    public CommonResult<Long> createNotification(@Valid @RequestBody NotificationCreateReqVO createReqVO) {
        return success(notificationService.createNotification(createReqVO));
    }

    @PutMapping("/update-read")
    @Operation(summary = "更新消息通知已读状态")
    public CommonResult<Boolean> updateNotificationReadStatus(@RequestParam("id") Long id,
                                                              @RequestParam("readStatus") Boolean readStatus) {
        notificationService.updateNotificationReadStatus(id, readStatus);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得消息通知")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<NotificationRespVO> getNotification(@RequestParam("id") Long id) {
        NotificationDO notification = notificationService.getNotification(id);
        return success(BeanUtils.toBean(notification, NotificationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取消息通知分页")
    public CommonResult<PageResult<NotificationRespVO>> getNotificationPage(@Valid NotificationPageReqVO pageReqVO) {
        PageResult<NotificationDO> page = notificationService.getNotificationPage(pageReqVO);
        return success(BeanUtils.toBean(page, NotificationRespVO.class));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取当前用户未读消息数量")
    public CommonResult<Long> getUnreadCount() {
        Long receiverUserId = getLoginUserId();
        return success(notificationService.getUnreadCount(receiverUserId));
    }

    @PostMapping("/send")
    @Operation(summary = "批量发送消息通知")
    public CommonResult<Boolean> sendNotification(@Valid @RequestBody NotificationSendReqVO sendReqVO) {
        notificationService.sendNotification(sendReqVO);
        return success(true);
    }

}
