package com.meession.etm.framework.redis.core.serialno;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SerialNoGenerator 单元测试
 *
 * @author jxq
 * @since 2026-07-14
 */
@DisplayName("统一编号生成器测试")
class SerialNoGeneratorTest {

    private SerialNoGenerator serialNoGenerator;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        serialNoGenerator = new SerialNoGenerator();
        ReflectionTestUtils.setField(serialNoGenerator, "stringRedisTemplate", stringRedisTemplate);
    }

    @Test
    @DisplayName("生成订单编号 - 按月格式 ORD-2026-070001")
    void testGenerateOrderNo() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // 生成订单编号：ORD- + 2026-07 + 0001
        String orderNo = serialNoGenerator.generate("ORD-", DatePattern.NORM_MONTH_PATTERN, 4);

        // 验证格式
        assertNotNull(orderNo);
        assertTrue(orderNo.startsWith("ORD-"));
        // 日期部分
        String expectedDate = DateUtil.format(LocalDateTime.now(), DatePattern.NORM_MONTH_PATTERN);
        assertTrue(orderNo.contains(expectedDate));
        // 序号部分（4位补零）
        assertTrue(orderNo.endsWith("0001"));

        // 验证 Redis 调用
        verify(valueOperations, times(1)).increment(anyString());
        verify(stringRedisTemplate, times(1)).expire(anyString(), eq(Duration.ofDays(2)));
    }

    @Test
    @DisplayName("生成合同编号 - 按天格式 HT20260714000001")
    void testGenerateContractNo() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // 生成合同编号：HT + 20260714 + 000001
        String contractNo = serialNoGenerator.generate("HT", DatePattern.PURE_DATE_PATTERN, 6);

        // 验证格式
        assertNotNull(contractNo);
        assertTrue(contractNo.startsWith("HT"));
        assertTrue(contractNo.endsWith("000001"));
        // HT(2) + yyyyMMdd(8) + 000001(6) = 16
        assertEquals(16, contractNo.length());

        // 验证 Redis 调用
        verify(valueOperations, times(1)).increment(anyString());
        verify(stringRedisTemplate, times(1)).expire(anyString(), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("生成工单编号 - 按月格式 WO-2026-070005")
    void testGenerateWorkOrderNo() {
        when(valueOperations.increment(anyString())).thenReturn(5L);

        String workOrderNo = serialNoGenerator.generate("WO-", DatePattern.NORM_MONTH_PATTERN, 4);

        assertNotNull(workOrderNo);
        assertTrue(workOrderNo.startsWith("WO-"));
        assertTrue(workOrderNo.endsWith("0005"));

        verify(valueOperations, times(1)).increment(anyString());
    }

    @Test
    @DisplayName("生成编号 - 使用默认参数")
    void testGenerateWithDefaults() {
        when(valueOperations.increment(anyString())).thenReturn(10L);

        // 使用默认参数：前缀 + yyyyMMdd + 6位序号
        String no = serialNoGenerator.generate("TEST");

        assertNotNull(no);
        assertTrue(no.startsWith("TEST"));
        assertTrue(no.endsWith("000010"));

        verify(valueOperations, times(1)).increment(anyString());
        verify(stringRedisTemplate, times(1)).expire(anyString(), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("生成编号 - 序号递增")
    void testGenerateIncrement() {
        when(valueOperations.increment(anyString()))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(3L);

        String no1 = serialNoGenerator.generate("SEQ-", DatePattern.NORM_MONTH_PATTERN, 4);
        String no2 = serialNoGenerator.generate("SEQ-", DatePattern.NORM_MONTH_PATTERN, 4);
        String no3 = serialNoGenerator.generate("SEQ-", DatePattern.NORM_MONTH_PATTERN, 4);

        // 验证序号递增（末尾4位）
        assertTrue(no1.endsWith("0001"));
        assertTrue(no2.endsWith("0002"));
        assertTrue(no3.endsWith("0003"));

        // 验证前缀一致
        String prefix = no1.substring(0, no1.length() - 4);
        assertTrue(no2.startsWith(prefix));
        assertTrue(no3.startsWith(prefix));

        verify(valueOperations, times(3)).increment(anyString());
    }

    @Test
    @DisplayName("获取当前序号 - 序号存在")
    void testGetCurrentSerialNoExists() {
        when(valueOperations.get(anyString())).thenReturn("5");

        Long currentNo = serialNoGenerator.getCurrentSerialNo("TEST-", DatePattern.NORM_MONTH_PATTERN);

        assertEquals(5L, currentNo);
        verify(valueOperations, times(1)).get(anyString());
    }

    @Test
    @DisplayName("获取当前序号 - 序号不存在")
    void testGetCurrentSerialNoNotExists() {
        when(valueOperations.get(anyString())).thenReturn(null);

        Long currentNo = serialNoGenerator.getCurrentSerialNo("TEST-", DatePattern.NORM_MONTH_PATTERN);

        assertEquals(0L, currentNo);
        verify(valueOperations, times(1)).get(anyString());
    }

    @Test
    @DisplayName("验证过期时间计算 - 按月格式 2天")
    void testCalculateExpireDurationMonth() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        serialNoGenerator.generate("TEST-", DatePattern.NORM_MONTH_PATTERN, 4);

        verify(stringRedisTemplate, times(1)).expire(anyString(), eq(Duration.ofDays(2)));
    }

    @Test
    @DisplayName("验证过期时间计算 - 按天格式 1天")
    void testCalculateExpireDurationDay() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        serialNoGenerator.generate("TEST-", DatePattern.PURE_DATE_PATTERN, 6);

        verify(stringRedisTemplate, times(1)).expire(anyString(), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("验证过期时间计算 - 按秒格式 1分钟")
    void testCalculateExpireDurationSecond() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        serialNoGenerator.generate("TEST-", DatePattern.PURE_DATETIME_PATTERN, 4);

        verify(stringRedisTemplate, times(1)).expire(anyString(), eq(Duration.ofMinutes(1)));
    }
}
