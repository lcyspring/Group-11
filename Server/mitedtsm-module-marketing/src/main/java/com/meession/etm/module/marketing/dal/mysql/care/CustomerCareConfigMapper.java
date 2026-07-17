package com.meession.etm.module.marketing.dal.mysql.care;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.marketing.controller.admin.care.vo.CustomerCareConfigPageReqVO;
import com.meession.etm.module.marketing.dal.dataobject.care.CustomerCareConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 客户关怀配置 Mapper
 *
 * @author MITEDTSM
 */
@Mapper
public interface CustomerCareConfigMapper extends BaseMapperX<CustomerCareConfigDO> {

    default PageResult<CustomerCareConfigDO> selectPage(CustomerCareConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CustomerCareConfigDO>()
                .likeIfPresent(CustomerCareConfigDO::getName, reqVO.getName())
                .eqIfPresent(CustomerCareConfigDO::getScene, reqVO.getScene())
                .eqIfPresent(CustomerCareConfigDO::getChannel, reqVO.getChannel())
                .eqIfPresent(CustomerCareConfigDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(CustomerCareConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CustomerCareConfigDO::getId));
    }

    /**
     * 获取所有启用状态的配置
     */
    default List<CustomerCareConfigDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<CustomerCareConfigDO>()
                .eq(CustomerCareConfigDO::getStatus, 0)); // 0 = 启用
    }

    /**
     * 根据场景获取启用配置
     */
    default List<CustomerCareConfigDO> selectEnabledByScene(String scene) {
        return selectList(new LambdaQueryWrapperX<CustomerCareConfigDO>()
                .eq(CustomerCareConfigDO::getScene, scene)
                .eq(CustomerCareConfigDO::getStatus, 0));
    }

}
