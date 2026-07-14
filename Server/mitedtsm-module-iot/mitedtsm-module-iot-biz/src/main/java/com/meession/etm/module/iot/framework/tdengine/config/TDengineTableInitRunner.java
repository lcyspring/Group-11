package com.meession.etm.module.iot.framework.tdengine.config;

import com.meession.etm.module.iot.service.device.message.IotDeviceMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * TDengine 表初始化的 Configuration
 *
 * @author alwayssuper
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TDengineTableInitRunner implements ApplicationRunner {

    private final IotDeviceMessageService deviceMessageService;

    @Value("${spring.datasource.dynamic.datasource.tdengine.url:}")
    private String tdengineUrl;

    @Override
    public void run(ApplicationArguments args) {
        if (tdengineUrl == null || tdengineUrl.isEmpty()) {
            log.info("[run][TDengine数据源未配置，跳过设备消息表结构初始化]");
            return;
        }
        try {
            deviceMessageService.defineDeviceMessageStable();
        } catch (Exception ex) {
            log.warn("[run][TDengine初始化设备消息表结构失败，IoT功能将不可用，跳过初始化]", ex);
        }
    }

}
