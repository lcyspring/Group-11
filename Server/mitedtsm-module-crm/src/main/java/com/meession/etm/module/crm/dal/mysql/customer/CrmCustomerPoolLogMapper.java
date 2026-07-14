package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolLogDO;
import com.meession.etm.module.crm.controller.admin.customer.vo.pool.CrmCustomerPoolLogPageReqVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmCustomerPoolLogMapper extends BaseMapperX<CrmCustomerPoolLogDO> {

    default PageResult<CrmCustomerPoolLogDO> selectPage(CrmCustomerPoolLogPageReqVO pageVO) {
        LambdaQueryWrapperX<CrmCustomerPoolLogDO> query = new LambdaQueryWrapperX<>();
        query.eqIfPresent(CrmCustomerPoolLogDO::getCustomerId, pageVO.getCustomerId())
                .likeIfPresent(CrmCustomerPoolLogDO::getCustomerName, pageVO.getCustomerName())
                .eqIfPresent(CrmCustomerPoolLogDO::getOperationType, pageVO.getOperationType())
                .eqIfPresent(CrmCustomerPoolLogDO::getOperationUserId, pageVO.getOperationUserId())
                .betweenIfPresent(CrmCustomerPoolLogDO::getCreateTime, pageVO.getCreateTime())
                .orderByDesc(CrmCustomerPoolLogDO::getCreateTime);
        return selectPage(pageVO, query);
    }

    default List<CrmCustomerPoolLogDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerPoolLogDO>()
                .eq(CrmCustomerPoolLogDO::getCustomerId, customerId)
                .orderByDesc(CrmCustomerPoolLogDO::getCreateTime));
    }

}
