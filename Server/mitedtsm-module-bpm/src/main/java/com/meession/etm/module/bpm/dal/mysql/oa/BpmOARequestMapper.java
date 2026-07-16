package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOARequestDO;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BpmOARequestMapper extends BaseMapperX<BpmOARequestDO> {

    default PageResult<BpmOARequestDO> selectPage(Long userId, BpmOARequestPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BpmOARequestDO>()
                .eqIfPresent(BpmOARequestDO::getUserId, userId)
                .eqIfPresent(BpmOARequestDO::getStatus, reqVO.getStatus())
                .eqIfPresent(BpmOARequestDO::getType, reqVO.getType())
                .likeIfPresent(BpmOARequestDO::getTitle, reqVO.getTitle())
                .betweenIfPresent(BpmOARequestDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(BpmOARequestDO::getId));
    }

}