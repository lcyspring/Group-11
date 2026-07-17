package com.meession.etm.module.infra.service.config;

import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.infra.controller.admin.config.vo.ConfigCategoryRespVO;
import com.meession.etm.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import com.meession.etm.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import com.meession.etm.module.infra.controller.admin.config.vo.NotificationConfigRespVO;
import com.meession.etm.module.infra.convert.config.ConfigConvert;
import com.meession.etm.module.infra.dal.dataobject.config.ConfigDO;
import com.meession.etm.module.infra.dal.mysql.config.ConfigMapper;
import com.meession.etm.module.infra.enums.config.ConfigCategoryEnum;
import com.meession.etm.module.infra.enums.config.ConfigTypeEnum;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.infra.enums.ErrorCodeConstants.*;

/**
 * 参数配置 Service 实现类
 */
@Service
@Slf4j
@Validated
public class ConfigServiceImpl implements ConfigService {

    /**
     * 通知配置相关的参数键
     */
    private static final String NOTIFICATION_EMAIL_ENABLED = "notification.email.enabled";
    private static final String NOTIFICATION_SMS_ENABLED = "notification.sms.enabled";
    private static final String NOTIFICATION_INAPP_ENABLED = "notification.inapp.enabled";
    private static final String NOTIFICATION_EMAIL_SMTP_HOST = "notification.email.smtp.host";
    private static final String NOTIFICATION_EMAIL_SMTP_PORT = "notification.email.smtp.port";
    private static final String NOTIFICATION_EMAIL_USERNAME = "notification.email.username";
    private static final String NOTIFICATION_SMS_PROVIDER = "notification.sms.provider";
    private static final String NOTIFICATION_SMS_API_KEY = "notification.sms.api.key";

    @Resource
    private ConfigMapper configMapper;

    @Override
    public Long createConfig(ConfigSaveReqVO createReqVO) {
        // 校验参数配置 key 的唯一性
        validateConfigKeyUnique(null, createReqVO.getKey());

        // 插入参数配置
        ConfigDO config = ConfigConvert.INSTANCE.convert(createReqVO);
        config.setType(ConfigTypeEnum.CUSTOM.getType());
        configMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateConfig(ConfigSaveReqVO updateReqVO) {
        // 校验自己存在
        validateConfigExists(updateReqVO.getId());
        // 校验参数配置 key 的唯一性
        validateConfigKeyUnique(updateReqVO.getId(), updateReqVO.getKey());

        // 更新参数配置
        ConfigDO updateObj = ConfigConvert.INSTANCE.convert(updateReqVO);
        configMapper.updateById(updateObj);
    }

    @Override
    public void deleteConfig(Long id) {
        // 校验配置存在
        ConfigDO config = validateConfigExists(id);
        // 内置配置，不允许删除
        if (ConfigTypeEnum.SYSTEM.getType().equals(config.getType())) {
            throw exception(CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
        }
        // 删除
        configMapper.deleteById(id);
    }

    @Override
    public void deleteConfigList(List<Long> ids) {
        // 校验是否有内置配置
        List<ConfigDO> configs = configMapper.selectByIds(ids);
        configs.forEach(config -> {
            if (ConfigTypeEnum.SYSTEM.getType().equals(config.getType())) {
                throw exception(CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
            }
        });

        // 批量删除
        configMapper.deleteByIds(ids);
    }

    @Override
    public ConfigDO getConfig(Long id) {
        return configMapper.selectById(id);
    }

    @Override
    public ConfigDO getConfigByKey(String key) {
        return configMapper.selectByKey(key);
    }

    @Override
    public PageResult<ConfigDO> getConfigPage(ConfigPageReqVO pageReqVO) {
        return configMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ConfigCategoryRespVO> getConfigCategoryList() {
        List<ConfigCategoryRespVO> list = new ArrayList<>();
        for (ConfigCategoryEnum categoryEnum : ConfigCategoryEnum.values()) {
            ConfigCategoryRespVO respVO = new ConfigCategoryRespVO();
            respVO.setCategory(categoryEnum.getCategory());
            respVO.setCategoryName(categoryEnum.getCategoryName());
            Long count = configMapper.selectCount(ConfigDO::getCategory, categoryEnum.getCategoryName());
            respVO.setConfigCount(count != null ? count.intValue() : 0);
            list.add(respVO);
        }
        return list;
    }

    @Override
    public PageResult<ConfigDO> getConfigPageByCategory(Integer category, Integer pageNo, Integer pageSize) {
        ConfigCategoryEnum categoryEnum = ConfigCategoryEnum.valueOf(category);
        if (categoryEnum == null) {
            return PageResult.empty();
        }
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo);
        pageParam.setPageSize(pageSize);
        return configMapper.selectPage(pageParam, new LambdaQueryWrapperX<ConfigDO>()
                .eq(ConfigDO::getCategory, categoryEnum.getCategoryName())
                .orderByDesc(ConfigDO::getId));
    }

    @Override
    public NotificationConfigRespVO getNotificationConfig() {
        NotificationConfigRespVO respVO = new NotificationConfigRespVO();
        respVO.setEmailEnabled(getBooleanConfig(NOTIFICATION_EMAIL_ENABLED, false));
        respVO.setSmsEnabled(getBooleanConfig(NOTIFICATION_SMS_ENABLED, false));
        respVO.setInAppEnabled(getBooleanConfig(NOTIFICATION_INAPP_ENABLED, true));
        respVO.setEmailSmtpHost(getStringConfig(NOTIFICATION_EMAIL_SMTP_HOST, ""));
        respVO.setEmailSmtpPort(getIntConfig(NOTIFICATION_EMAIL_SMTP_PORT, 465));
        respVO.setEmailUsername(getStringConfig(NOTIFICATION_EMAIL_USERNAME, ""));
        respVO.setSmsProvider(getStringConfig(NOTIFICATION_SMS_PROVIDER, ""));
        respVO.setSmsApiKey(getStringConfig(NOTIFICATION_SMS_API_KEY, ""));
        return respVO;
    }

    @Override
    public void updateNotificationConfig(NotificationConfigRespVO reqVO) {
        String category = ConfigCategoryEnum.NOTIFICATION.getCategoryName();
        saveNotificationConfig(NOTIFICATION_EMAIL_ENABLED, "邮件通知开关",
                reqVO.getEmailEnabled() != null ? reqVO.getEmailEnabled().toString() : "false", category);
        saveNotificationConfig(NOTIFICATION_SMS_ENABLED, "短信通知开关",
                reqVO.getSmsEnabled() != null ? reqVO.getSmsEnabled().toString() : "false", category);
        saveNotificationConfig(NOTIFICATION_INAPP_ENABLED, "站内信通知开关",
                reqVO.getInAppEnabled() != null ? reqVO.getInAppEnabled().toString() : "true", category);
        saveNotificationConfig(NOTIFICATION_EMAIL_SMTP_HOST, "邮件 SMTP 主机",
                reqVO.getEmailSmtpHost(), category);
        saveNotificationConfig(NOTIFICATION_EMAIL_SMTP_PORT, "邮件 SMTP 端口",
                reqVO.getEmailSmtpPort() != null ? reqVO.getEmailSmtpPort().toString() : "", category);
        saveNotificationConfig(NOTIFICATION_EMAIL_USERNAME, "邮件用户名",
                reqVO.getEmailUsername(), category);
        saveNotificationConfig(NOTIFICATION_SMS_PROVIDER, "短信服务商",
                reqVO.getSmsProvider(), category);
        saveNotificationConfig(NOTIFICATION_SMS_API_KEY, "短信 API Key",
                reqVO.getSmsApiKey(), category);
    }

    private String getStringConfig(String key, String defaultValue) {
        ConfigDO config = configMapper.selectByKey(key);
        return config != null && config.getValue() != null ? config.getValue() : defaultValue;
    }

    private Boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getStringConfig(key, null);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    private Integer getIntConfig(String key, Integer defaultValue) {
        String value = getStringConfig(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void saveNotificationConfig(String key, String name, String value, String category) {
        if (value == null) {
            value = "";
        }
        ConfigDO config = configMapper.selectByKey(key);
        if (config == null) {
            ConfigDO newConfig = new ConfigDO();
            newConfig.setCategory(category);
            newConfig.setName(name);
            newConfig.setConfigKey(key);
            newConfig.setValue(value);
            newConfig.setType(ConfigTypeEnum.CUSTOM.getType());
            newConfig.setVisible(true);
            configMapper.insert(newConfig);
        } else {
            ConfigDO updateObj = new ConfigDO();
            updateObj.setId(config.getId());
            updateObj.setValue(value);
            configMapper.updateById(updateObj);
        }
    }

    @VisibleForTesting
    public ConfigDO validateConfigExists(Long id) {
        if (id == null) {
            return null;
        }
        ConfigDO config = configMapper.selectById(id);
        if (config == null) {
            throw exception(CONFIG_NOT_EXISTS);
        }
        return config;
    }

    @VisibleForTesting
    public void validateConfigKeyUnique(Long id, String key) {
        ConfigDO config = configMapper.selectByKey(key);
        if (config == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的参数配置
        if (id == null) {
            throw exception(CONFIG_KEY_DUPLICATE);
        }
        if (!config.getId().equals(id)) {
            throw exception(CONFIG_KEY_DUPLICATE);
        }
    }

}
