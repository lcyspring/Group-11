package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportUpdateReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkReportDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOAWorkReportMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_WORK_REPORT_NOT_EXISTS;

@Service
@Validated
public class BpmOAWorkReportServiceImpl implements BpmOAWorkReportService {

    @Resource
    private BpmOAWorkReportMapper workReportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkReport(Long userId, BpmOAWorkReportCreateReqVO createReqVO) {
        BpmOAWorkReportDO report = BeanUtils.toBean(createReqVO, BpmOAWorkReportDO.class)
                .setUserId(userId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        workReportMapper.insert(report);
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkReport(BpmOAWorkReportUpdateReqVO updateReqVO) {
        validateWorkReportExists(updateReqVO.getId());
        BpmOAWorkReportDO report = BeanUtils.toBean(updateReqVO, BpmOAWorkReportDO.class);
        workReportMapper.updateById(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkReport(Long id) {
        validateWorkReportExists(id);
        workReportMapper.deleteById(id);
    }

    private void validateWorkReportExists(Long id) {
        if (workReportMapper.selectById(id) == null) {
            throw exception(OA_WORK_REPORT_NOT_EXISTS);
        }
    }

    @Override
    public BpmOAWorkReportDO getWorkReport(Long id) {
        return workReportMapper.selectById(id);
    }

    @Override
    public PageResult<BpmOAWorkReportDO> getWorkReportPage(Long userId, BpmOAWorkReportPageReqVO pageReqVO) {
        return workReportMapper.selectPage(userId, pageReqVO);
    }

}