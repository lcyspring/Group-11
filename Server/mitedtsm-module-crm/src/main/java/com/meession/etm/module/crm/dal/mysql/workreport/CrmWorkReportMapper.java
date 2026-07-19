package com.meession.etm.module.crm.dal.mysql.workreport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.workreport.vo.CrmWorkReportPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.workreport.CrmWorkReportDO;
import org.apache.ibatis.annotations.Mapper;
import java.time.LocalDate;

@Mapper
public interface CrmWorkReportMapper extends BaseMapperX<CrmWorkReportDO> {
    default PageResult<CrmWorkReportDO> selectPage(Long userId, CrmWorkReportPageReqVO req) {
        LambdaQueryWrapperX<CrmWorkReportDO> query = new LambdaQueryWrapperX<CrmWorkReportDO>()
                .eqIfPresent(CrmWorkReportDO::getReportType, req.getReportType())
                .eqIfPresent(CrmWorkReportDO::getStatus, req.getStatus())
                .betweenIfPresent(CrmWorkReportDO::getReportDate, req.getReportDate())
                .orderByDesc(CrmWorkReportDO::getReportDate).orderByDesc(CrmWorkReportDO::getId);
        if (Boolean.TRUE.equals(req.getReceived())) {
            query.apply("FIND_IN_SET({0}, receiver_user_ids) > 0", userId);
        } else {
            query.eq(CrmWorkReportDO::getAuthorUserId, userId);
        }
        return selectPage(req, query);
    }

    default CrmWorkReportDO selectByAuthorAndPeriod(Long authorId, Integer type, LocalDate start) {
        return selectOne(new LambdaQueryWrapper<CrmWorkReportDO>()
                .eq(CrmWorkReportDO::getAuthorUserId, authorId)
                .eq(CrmWorkReportDO::getReportType, type)
                .eq(CrmWorkReportDO::getPeriodStart, start));
    }
}
