package com.meession.etm.module.marketing.service.care;

/**
 * 客户关怀 Service 接口
 *
 * @author MITEDTSM
 */
public interface CustomerCareService {

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
