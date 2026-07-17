package com.meession.etm.module.marketing.service.care;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.marketing.controller.admin.care.vo.CustomerCareConfigPageReqVO;
import com.meession.etm.module.marketing.controller.admin.care.vo.CustomerCareConfigSaveReqVO;
import com.meession.etm.module.marketing.dal.dataobject.care.CustomerCareConfigDO;
import jakarta.validation.Valid;

/**
 * 客户关怀 Service 接口
 *
 * @author MITEDTSM
 */
public interface CustomerCareService {

    /**
     * 创建客户关怀模板配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCareConfig(@Valid CustomerCareConfigSaveReqVO createReqVO);

    /**
     * 更新客户关怀模板配置
     *
     * @param updateReqVO 更新信息
     */
    void updateCareConfig(@Valid CustomerCareConfigSaveReqVO updateReqVO);

    /**
     * 删除客户关怀模板配置
     *
     * @param id 编号
     */
    void deleteCareConfig(Long id);

    /**
     * 获得客户关怀模板配置
     *
     * @param id 编号
     * @return 客户关怀配置
     */
    CustomerCareConfigDO getCareConfig(Long id);

    /**
     * 获得客户关怀模板配置分页
     *
     * @param pageReqVO 分页查询
     * @return 客户关怀配置分页
     */
    PageResult<CustomerCareConfigDO> getCareConfigPage(CustomerCareConfigPageReqVO pageReqVO);

    /**
     * 执行生日关怀：扫描今日生日的会员并发送关怀消息
     *
     * @return 发送数量
     */
    int executeBirthdayCare();

    /**
     * 执行节日关怀：检查今日是否命中节日配置，命中则发送关怀消息
     *
     * @return 发送数量
     */
    int executeHolidayCare();

}
