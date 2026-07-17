package com.meession.etm.module.crm.dal.mysql.visit;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.visit.vo.CrmCustomerVisitPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.visit.CrmCustomerVisitDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Mapper
public interface CrmCustomerVisitMapper extends BaseMapperX<CrmCustomerVisitDO> {
    default PageResult<CrmCustomerVisitDO> selectPage(Long userId, CrmCustomerVisitPageReqVO request) {
        return selectPage(request, new LambdaQueryWrapperX<CrmCustomerVisitDO>()
                .eq(CrmCustomerVisitDO::getApplicantUserId, userId)
                .eqIfPresent(CrmCustomerVisitDO::getCustomerId, request.getCustomerId())
                .eqIfPresent(CrmCustomerVisitDO::getAuditStatus, request.getAuditStatus())
                .eqIfPresent(CrmCustomerVisitDO::getResultStatus, request.getResultStatus())
                .orderByDesc(CrmCustomerVisitDO::getId));
    }

    default CrmCustomerVisitDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapper<CrmCustomerVisitDO>()
                .eq(CrmCustomerVisitDO::getId, id).last("FOR UPDATE"));
    }
}
