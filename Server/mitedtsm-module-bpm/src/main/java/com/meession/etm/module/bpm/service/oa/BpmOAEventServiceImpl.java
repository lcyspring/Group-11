package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAEventSaveReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAEventDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOAEventMapper;
import com.meession.etm.module.system.api.notify.NotifyMessageSendApi;
import com.meession.etm.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_EVENT_NOT_EXISTS;

@Service
public class BpmOAEventServiceImpl implements BpmOAEventService {
    @Resource private BpmOAEventMapper mapper;
    @Resource private NotifyMessageSendApi notifyMessageSendApi;
    @Override public Long create(Long userId, BpmOAEventSaveReqVO req) {
        BpmOAEventDO row = BeanUtils.toBean(req, BpmOAEventDO.class).setUserId(userId).setStatus(0);
        mapper.insert(row); return row.getId();
    }
    @Override public void update(Long userId, BpmOAEventSaveReqVO req) {
        BpmOAEventDO old = require(userId, req.getId());
        mapper.updateById(BeanUtils.toBean(req, BpmOAEventDO.class).setId(old.getId()).setUserId(userId));
    }
    @Override public void delete(Long userId, Long id) { require(userId, id); mapper.deleteById(id); }
    @Override public BpmOAEventDO get(Long userId, Long id) { return require(userId, id); }
    @Override public List<BpmOAEventDO> list(Long userId, LocalDateTime from, LocalDateTime to) { return mapper.selectByUserId(userId, from, to); }
    @Override public int remindDue(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        LocalDateTime now = LocalDateTime.now();
        int sent = 0;
        for (BpmOAEventDO event : mapper.selectDueReminders(now, safeLimit)) {
            if (mapper.claimReminder(event.getId()) == 0) continue;
            try {
                notifyMessageSendApi.sendSingleMessageToAdmin(new NotifySendSingleToUserReqDTO()
                        .setUserId(event.getUserId()).setTemplateCode("bpm-oa-event-reminder")
                        .setTemplateParams(Map.of("title", event.getTitle(), "startTime", event.getStartTime(),
                                "location", event.getLocation() == null ? "" : event.getLocation())));
                mapper.markReminderSent(event.getId(), LocalDateTime.now());
                sent++;
            } catch (RuntimeException ex) {
                String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                mapper.releaseReminder(event.getId(), error.substring(0, Math.min(error.length(), 1000)));
            }
        }
        return sent;
    }
    private BpmOAEventDO require(Long userId, Long id) {
        BpmOAEventDO row = mapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) throw exception(OA_EVENT_NOT_EXISTS);
        return row;
    }
}
