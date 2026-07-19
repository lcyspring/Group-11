package com.meession.etm.module.crm.service.activity;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.activity.vo.*;
import com.meession.etm.module.crm.dal.dataobject.activity.CrmCallRecordDO;
import com.meession.etm.module.crm.dal.dataobject.activity.CrmClueConversionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.activity.CrmSmsRecordDO;
import com.meession.etm.module.crm.dal.dataobject.activity.CrmTaskActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.activity.CrmTaskDO;

import java.util.List;

public interface CrmActivityService {
    Long createTask(CrmTaskSaveReqVO reqVO, Long userId);
    void updateTask(CrmTaskSaveReqVO reqVO, Long userId);
    void startTask(CrmTaskActionReqVO reqVO, Long userId);
    void completeTask(CrmTaskActionReqVO reqVO, Long userId);
    void markTaskUnable(CrmTaskActionReqVO reqVO, Long userId);
    void cancelTask(CrmTaskActionReqVO reqVO, Long userId);
    PageResult<CrmTaskDO> getTaskPage(CrmTaskPageReqVO reqVO);
    List<CrmTaskActionRecordDO> getTaskActionRecords(Long taskId, Long userId);
    Long createCallRecord(CrmCallRecordSaveReqVO reqVO, Long userId);
    PageResult<CrmCallRecordDO> getCallRecordPage(CrmActivityPageReqVO reqVO);
    Long createSmsRecord(CrmSmsRecordSaveReqVO reqVO, Long userId);
    PageResult<CrmSmsRecordDO> getSmsRecordPage(CrmActivityPageReqVO reqVO);
    CrmClueConversionRecordDO getConversionRecord(Long clueId);
    CrmClueConversionRecordDO migrateClueActivities(Long clueId, Long customerId, Long primaryContactId,
                                                     int followUpCount, Long operatorUserId);
    int markOverdueTasks();
}
