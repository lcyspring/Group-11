package com.meession.etm.module.crm.dal.redis.no;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.meession.etm.module.crm.dal.redis.RedisKeyConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Crm 订单序号的 Redis DAO
 *
 * @author HUIHUI
 */
@Repository
public class CrmNoRedisDAO {

    /**
     * 合同 {@link com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO}
     */
    public static final String CONTRACT_NO_PREFIX = "HT";

    /**
     * 回款 {@link com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO}
     */
    public static final String RECEIVABLE_PREFIX = "HK";

    /** 发票申请号前缀。 */
    public static final String INVOICE_PREFIX = "FP";

    /** 客服工单编号前缀。 */
    public static final String WORK_ORDER_PREFIX = "W-";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成序号，使用当前日期，格式为 {PREFIX} + yyyyMMdd + 6 位自增
     * 例如说：QTRK 202109 000001 （没有中间空格）
     *
     * @param prefix 前缀
     * @return 序号
     */
    public String generate(String prefix) {
        // 递增序号
        String noPrefix = prefix + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String key = RedisKeyConstants.NO + noPrefix;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        // 设置过期时间
        stringRedisTemplate.expire(key, Duration.ofDays(1L));
        return noPrefix + String.format("%06d", no);
    }

    /**
     * 生成按月递增的编号，格式为 {PREFIX}yyyyMM-NNNN。
     */
    public String generateMonthly(String prefix) {
        String noPrefix = prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = RedisKeyConstants.NO + noPrefix;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, Duration.ofDays(32L));
        return noPrefix + "-" + String.format("%04d", no);
    }

}
