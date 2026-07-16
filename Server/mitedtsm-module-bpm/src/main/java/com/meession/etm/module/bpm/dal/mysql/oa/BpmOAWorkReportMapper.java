package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkReportDO;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BpmOAWorkReportMapper extends BaseMapperX<BpmOAWorkReportDO> {

    default PageResult<BpmOAWorkReportDO> selectPage(Long userId, BpmOAWorkReportPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BpmOAWorkReportDO>()
                .eqIfPresent(BpmOAWorkReportDO::getUserId, userId)
                .eqIfPresent(BpmOAWorkReportDO::getStatus, reqVO.getStatus())
                .eqIfPresent(BpmOAWorkReportDO::getType, reqVO.getType())
                .eqIfPresent(BpmOAWorkReportDO::getReportDate, reqVO.getReportDate())
                .orderByDesc(BpmOAWorkReportDO::getId));
    }

}