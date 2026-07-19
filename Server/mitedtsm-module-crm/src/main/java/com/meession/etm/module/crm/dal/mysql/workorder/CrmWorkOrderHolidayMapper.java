package com.meession.etm.module.crm.dal.mysql.workorder;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderHolidayDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

@Mapper
public interface CrmWorkOrderHolidayMapper extends BaseMapperX<CrmWorkOrderHolidayDO> {
    default CrmWorkOrderHolidayDO selectByDate(LocalDate date) {
        return selectOne(new LambdaQueryWrapperX<CrmWorkOrderHolidayDO>()
                .eq(CrmWorkOrderHolidayDO::getHolidayDate, date));
    }
}
