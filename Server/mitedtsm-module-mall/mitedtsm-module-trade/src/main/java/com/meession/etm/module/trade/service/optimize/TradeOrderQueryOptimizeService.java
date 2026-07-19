package com.meession.etm.module.trade.service.optimize;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单查询优化服务
 */
@Slf4j
@Service
public class TradeOrderQueryOptimizeService {

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    /**
     * 分页查询优化 - 避免大分页
     */
    public IPage<TradeOrderDO> selectPageOptimized(Integer pageNo, Integer pageSize, Long userId) {
        pageNo = Math.max(1, pageNo);
        pageSize = Math.min(100, pageSize);

        Page<TradeOrderDO> page = new Page<>(pageNo, pageSize);
        page.setSearchCount(false);

        LambdaQueryWrapperX<TradeOrderDO> wrapper = new LambdaQueryWrapperX<TradeOrderDO>()
                .eqIfPresent(TradeOrderDO::getUserId, userId)
                .orderByDesc(TradeOrderDO::getId);

        return tradeOrderMapper.selectPage(page, wrapper);
    }

    /**
     * 游标查询 - 避免深度分页
     */
    public List<TradeOrderDO> selectByCursor(Long lastId, Integer limit, Long userId) {
        limit = Math.min(1000, limit);

        LambdaQueryWrapperX<TradeOrderDO> wrapper = new LambdaQueryWrapperX<TradeOrderDO>()
                .gt(TradeOrderDO::getId, lastId)
                .eqIfPresent(TradeOrderDO::getUserId, userId)
                .orderByAsc(TradeOrderDO::getId)
                .last("LIMIT " + limit);

        return tradeOrderMapper.selectList(wrapper);
    }

    /**
     * 索引覆盖查询 - 只查询索引字段
     */
    public List<Long> selectOrderIdsByUserId(Long userId) {
        LambdaQueryWrapperX<TradeOrderDO> wrapper = new LambdaQueryWrapperX<TradeOrderDO>()
                .select(TradeOrderDO::getId)
                .eq(TradeOrderDO::getUserId, userId);

        return tradeOrderMapper.selectObjs(wrapper);
    }

}