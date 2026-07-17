package com.meession.etm.module.infra.service.config;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.infra.controller.admin.config.vo.ConfigCategoryRespVO;
import com.meession.etm.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import com.meession.etm.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import com.meession.etm.module.infra.controller.admin.config.vo.NotificationConfigRespVO;
import com.meession.etm.module.infra.dal.dataobject.config.ConfigDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 参数配置 Service 接口
 *
 * @author 密讯
 */
public interface ConfigService {

    /**
     * 创建参数配置
     *
     * @param createReqVO 创建信息
     * @return 配置编号
     */
    Long createConfig(@Valid ConfigSaveReqVO createReqVO);

    /**
     * 更新参数配置
     *
     * @param updateReqVO 更新信息
     */
    void updateConfig(@Valid ConfigSaveReqVO updateReqVO);

    /**
     * 删除参数配置
     *
     * @param id 配置编号
     */
    void deleteConfig(Long id);

    /**
     * 批量删除参数配置
     *
     * @param ids 配置编号列表
     */
    void deleteConfigList(List<Long> ids);

    /**
     * 获得参数配置
     *
     * @param id 配置编号
     * @return 参数配置
     */
    ConfigDO getConfig(Long id);

    /**
     * 根据参数键，获得参数配置
     *
     * @param key 配置键
     * @return 参数配置
     */
    ConfigDO getConfigByKey(String key);

    /**
     * 获得参数配置分页列表
     *
     * @param reqVO 分页条件
     * @return 分页列表
     */
    PageResult<ConfigDO> getConfigPage(ConfigPageReqVO reqVO);

    /**
     * 获得配置分类列表
     *
     * @return 配置分类列表
     */
    List<ConfigCategoryRespVO> getConfigCategoryList();

    /**
     * 分页查询指定分类的配置
     *
     * @param category 分类编号
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页列表
     */
    PageResult<ConfigDO> getConfigPageByCategory(Integer category, Integer pageNo, Integer pageSize);

    /**
     * 获得通知配置
     *
     * @return 通知配置
     */
    NotificationConfigRespVO getNotificationConfig();

    /**
     * 更新通知配置
     *
     * @param reqVO 通知配置
     */
    void updateNotificationConfig(NotificationConfigRespVO reqVO);

}
