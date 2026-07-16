package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmCustomerCareRecordPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCareRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrmCustomerCareRecordMapper extends BaseMapperX<CrmCustomerCareRecordDO> {
    default PageResult<CrmCustomerCareRecordDO> selectPage(CrmCustomerCareRecordPageReqVO request) {
        return selectPage(request, new LambdaQueryWrapperX<CrmCustomerCareRecordDO>()
                .eqIfPresent(CrmCustomerCareRecordDO::getPlanId, request.getPlanId())
                .eqIfPresent(CrmCustomerCareRecordDO::getStatus, request.getStatus())
                .eqIfPresent(CrmCustomerCareRecordDO::getEventDate, request.getEventDate())
                .orderByDesc(CrmCustomerCareRecordDO::getId));
    }
}
