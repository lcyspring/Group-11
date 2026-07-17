package com.meession.etm.module.iot.framework.tdengine.config;

import com.meession.etm.module.iot.service.device.message.IotDeviceMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 初始化设备消息表
            deviceMessageService.defineDeviceMessageStable();
        } catch (Exception ex) {
            // 初始化失败时打印警告消息，继续运行（CRM等非IoT模块不受影响）
            log.warn("[run][TDengine初始化设备消息表结构失败，IoT相关功能将不可用，系统继续运行]", ex);
        }
    }

}
