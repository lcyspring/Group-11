package com.meession.etm.module.marketing.dal.mysql.care;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
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
