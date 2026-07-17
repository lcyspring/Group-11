package com.meession.etm.module.infra.job;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统健康检查定时任务
 *
 * 检查系统各项指标（数据库连接、Redis连接等）
 *
 * @author 密讯
 */
@Component
@Slf4j
public class SystemHealthCheckJobHandler implements JobHandler {

    @Resource
    private DataSource dataSource;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @TenantIgnore
    public String execute(String param) {
        List<String> results = new ArrayList<>();
        // 1. 检查数据库连接
        results.add(checkDatabase());
        // 2. 检查 Redis 连接
        results.add(checkRedis());
        // 3. 汇总结果
        String summary = String.join("; ", results);
        log.info("[execute][系统健康检查完成：{}]", summary);
        return summary;
    }

    /**
     * 检查数据库连接
     *
     * @return 检查结果
     */
    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(3);
            if (valid) {
                log.info("[checkDatabase][数据库连接正常]");
                return "数据库连接正常";
            } else {
                log.error("[checkDatabase][数据库连接异常]");
                return "数据库连接异常";
            }
        } catch (Exception e) {
            log.error("[checkDatabase][数据库连接异常]", e);
            return "数据库连接异常";
        }
    }

    /**
     * 检查 Redis 连接
     *
     * @return 检查结果
     */
    private String checkRedis() {
        try {
            Long dbSize = stringRedisTemplate.execute(RedisServerCommands::dbSize);
            log.info("[checkRedis][Redis 连接正常，key 数量 ({})]", dbSize);
            return String.format("Redis 连接正常，key 数量 %s", dbSize);
        } catch (Exception e) {
            log.error("[checkRedis][Redis 连接异常]", e);
            return "Redis 连接异常";
        }
    }

}
