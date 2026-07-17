package com.meession.etm.module.crm.service.workreport;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.workreport.vo.*;
import com.meession.etm.module.crm.dal.dataobject.workreport.CrmWorkReportDO;
import com.meession.etm.module.crm.dal.mysql.workreport.CrmWorkReportMapper;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
public class CrmWorkReportServiceImpl implements CrmWorkReportService {
    public static final int TYPE_DAY = 1, TYPE_WEEK = 2, TYPE_MONTH = 3;
    public static final int STATUS_DRAFT = 0, STATUS_SUBMITTED = 1;
    @Resource private CrmWorkReportMapper mapper;
    @Resource private AdminUserApi adminUserApi;

    @Override @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, CrmWorkReportSaveReqVO req) {
        Period period = period(req.getReportType(), req.getReportDate());
        if (mapper.selectByAuthorAndPeriod(userId, req.getReportType(), period.start()) != null) {
            throw exception(WORK_REPORT_PERIOD_DUPLICATE);
        }
        List<Long> receivers = normalizedReceivers(userId, req.getReceiverUserIds());
        CrmWorkReportDO row = BeanUtils.toBean(req, CrmWorkReportDO.class)
                .setAuthorUserId(userId).setPeriodStart(period.start()).setPeriodEnd(period.end())
                .setReceiverUserIds(receivers).setStatus(STATUS_DRAFT);
        mapper.insert(row);
        return row.getId();
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, CrmWorkReportSaveReqVO req) {
        CrmWorkReportDO old = editable(userId, req.getId());
        Period period = period(req.getReportType(), req.getReportDate());
        CrmWorkReportDO duplicate = mapper.selectByAuthorAndPeriod(userId, req.getReportType(), period.start());
        if (duplicate != null && !Objects.equals(duplicate.getId(), old.getId())) throw exception(WORK_REPORT_PERIOD_DUPLICATE);
        List<Long> receivers = normalizedReceivers(userId, req.getReceiverUserIds());
        mapper.updateById(BeanUtils.toBean(req, CrmWorkReportDO.class)
                .setPeriodStart(period.start()).setPeriodEnd(period.end()).setReceiverUserIds(receivers));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, Long id) {
        editable(userId, id);
        mapper.updateById(new CrmWorkReportDO().setId(id).setStatus(STATUS_SUBMITTED).setSubmitTime(LocalDateTime.now()));
    }

    @Override public void delete(Long userId, Long id) { editable(userId, id); mapper.deleteById(id); }

    @Override public CrmWorkReportDO get(Long userId, Long id) {
        CrmWorkReportDO row = mapper.selectById(id);
        if (row == null || !(Objects.equals(row.getAuthorUserId(), userId)
                || Optional.ofNullable(row.getReceiverUserIds()).orElse(List.of()).contains(userId))) {
            throw exception(WORK_REPORT_NOT_EXISTS);
        }
        return row;
    }

    @Override public PageResult<CrmWorkReportDO> page(Long userId, CrmWorkReportPageReqVO req) {
        return mapper.selectPage(userId, req);
    }

    private CrmWorkReportDO editable(Long userId, Long id) {
        CrmWorkReportDO row = mapper.selectById(id);
        if (row == null || !Objects.equals(row.getAuthorUserId(), userId)) throw exception(WORK_REPORT_NOT_EXISTS);
        if (!Integer.valueOf(STATUS_DRAFT).equals(row.getStatus())) throw exception(WORK_REPORT_SUBMITTED_IMMUTABLE);
        return row;
    }

    private void validateReceivers(Long author, List<Long> receivers) {
        List<Long> ids = distinct(receivers).stream().filter(id -> !Objects.equals(id, author)).toList();
        if (ids.isEmpty()) throw exception(WORK_REPORT_RECEIVER_REQUIRED);
        adminUserApi.validateUserList(ids);
    }
    private List<Long> normalizedReceivers(Long author, List<Long> receivers) {
        validateReceivers(author, receivers);
        return distinct(receivers).stream().filter(id -> !Objects.equals(id, author)).toList();
    }
    private List<Long> distinct(List<Long> values) { return values == null ? List.of() : values.stream().filter(Objects::nonNull).distinct().toList(); }

    private Period period(Integer type, LocalDate date) {
        if (date == null) throw exception(WORK_REPORT_TYPE_INVALID);
        if (Integer.valueOf(TYPE_DAY).equals(type)) return new Period(date, date);
        if (Integer.valueOf(TYPE_WEEK).equals(type)) {
            LocalDate start = date.with(DayOfWeek.MONDAY); return new Period(start, start.plusDays(6));
        }
        if (Integer.valueOf(TYPE_MONTH).equals(type)) {
            return new Period(date.withDayOfMonth(1), date.with(TemporalAdjusters.lastDayOfMonth()));
        }
        throw exception(WORK_REPORT_TYPE_INVALID);
    }
    private record Period(LocalDate start, LocalDate end) {}
}
