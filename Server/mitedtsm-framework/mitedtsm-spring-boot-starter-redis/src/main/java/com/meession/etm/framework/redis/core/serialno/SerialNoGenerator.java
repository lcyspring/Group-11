package com.meession.etm.framework.redis.core.serialno;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 统一编号生成器
 * <p>
 * 基于 Redis 的分布式编号生成服务，支持多种编号规则
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 生成订单编号：ORD-202607-0001
 * String orderNo = serialNoGenerator.generate("ORD-", DatePattern.NORM_MONTH_PATTERN, 4);
 *
 * // 生成合同编号：HT20260714000001
 * String contractNo = serialNoGenerator.generate("HT", DatePattern.PURE_DATE_PATTERN, 6);
 *
 * // 生成工单编号：WO-202607-0001
 * String workOrderNo = serialNoGenerator.generate("WO-", DatePattern.NORM_MONTH_PATTERN, 4);
 * </pre>
 *
 * @author jxq
 * @since 2026-07-14
 */
@Component
public class SerialNoGenerator {

    private static final String REDIS_KEY_PREFIX = "mitedtsm:serial_no:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成编号
     *
     * @param prefix      前缀（如 "ORD-"、"HT"、"WO-"）
     * @param dateFormat  日期格式（如 DatePattern.NORM_MONTH_PATTERN、DatePattern.PURE_DATE_PATTERN）
     * @param serialDigits 序号位数（如 4 表示 0001-9999，6 表示 000001-999999）
     * @return 生成的编号
     */
    public String generate(String prefix, String dateFormat, int serialDigits) {
        // 构建编号前缀：前缀 + 日期
        String dateStr = DateUtil.format(LocalDateTime.now(), dateFormat);
        String noPrefix = prefix + dateStr;

        // 构建 Redis Key
        String redisKey = REDIS_KEY_PREFIX + noPrefix;

        // 原子递增序号
        Long serialNo = stringRedisTemplate.opsForValue().increment(redisKey);

        // 设置过期时间（根据日期格式动态计算）
        Duration expireDuration = calculateExpireDuration(dateFormat);
        stringRedisTemplate.expire(redisKey, expireDuration);

        // 格式化序号（补零）
        String formattedSerialNo = String.format("%0" + serialDigits + "d", serialNo);

        return noPrefix + formattedSerialNo;
    }

    /**
     * 生成编号（使用默认序号位数 6 位）
     *
     * @param prefix     前缀
     * @param dateFormat 日期格式
     * @return 生成的编号
     */
    public String generate(String prefix, String dateFormat) {
        return generate(prefix, dateFormat, 6);
    }

    /**
     * 生成编号（使用默认日期格式 yyyyMMdd 和序号位数 6 位）
     *
     * @param prefix 前缀
     * @return 生成的编号
     */
    public String generate(String prefix) {
        return generate(prefix, DatePattern.PURE_DATE_PATTERN, 6);
    }

    /**
     * 根据日期格式计算过期时间
     * <p>
     * 按月格式：过期时间为 2 天（确保跨月后自动清理）<br>
     * 按天格式：过期时间为 1 天<br>
     * 按秒格式：过期时间为 1 分钟
     * </p>
     *
     * @param dateFormat 日期格式
     * @return 过期时间
     */
    private Duration calculateExpireDuration(String dateFormat) {
        if (dateFormat.contains("MM") && !dateFormat.contains("dd")) {
            // 按月格式（如 yyyy-MM、yyyyMM）
            return Duration.ofDays(2);
        } else if (dateFormat.contains("dd") && !dateFormat.contains("HH")) {
            // 按天格式（如 yyyy-MM-dd、yyyyMMdd）
            return Duration.ofDays(1);
        } else {
            // 按秒或更细粒度格式
            return Duration.ofMinutes(1);
        }
    }

    /**
     * 获取当前序号（不递增，仅查询）
     *
     * @param prefix     前缀
     * @param dateFormat 日期格式
     * @return 当前序号，如果不存在返回 0
     */
    public Long getCurrentSerialNo(String prefix, String dateFormat) {
        String dateStr = DateUtil.format(LocalDateTime.now(), dateFormat);
        String noPrefix = prefix + dateStr;
        String redisKey = REDIS_KEY_PREFIX + noPrefix;

        String value = stringRedisTemplate.opsForValue().get(redisKey);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
