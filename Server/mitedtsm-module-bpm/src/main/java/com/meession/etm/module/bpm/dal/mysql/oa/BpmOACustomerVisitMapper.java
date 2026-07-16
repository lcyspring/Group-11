package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOACustomerVisitPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOACustomerVisitDO;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BpmOACustomerVisitMapper extends BaseMapperX<BpmOACustomerVisitDO> {

    default PageResult<BpmOACustomerVisitDO> selectPage(Long userId, BpmOACustomerVisitPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BpmOACustomerVisitDO>()
                .eqIfPresent(BpmOACustomerVisitDO::getUserId, userId)
                .eqIfPresent(BpmOACustomerVisitDO::getStatus, reqVO.getStatus())
                .likeIfPresent(BpmOACustomerVisitDO::getCustomerName, reqVO.getCustomerName())
                .likeIfPresent(BpmOACustomerVisitDO::getContactPerson, reqVO.getContactPerson())
                .likeIfPresent(BpmOACustomerVisitDO::getPurpose, reqVO.getPurpose())
                .betweenIfPresent(BpmOACustomerVisitDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(BpmOACustomerVisitDO::getId));
    }

}