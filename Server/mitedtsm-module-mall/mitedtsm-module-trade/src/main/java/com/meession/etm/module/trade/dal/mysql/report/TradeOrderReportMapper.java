package com.meession.etm.module.trade.dal.mysql.report;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderDailyReportRespVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportReqVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportRespVO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TradeOrderReportMapper extends BaseMapperX<TradeOrderDO> {

    @Select("""
            SELECT
                COUNT(*) as totalCount,
                SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as paidCount,
                SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as deliveryCount,
                SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as receiveCount,
                SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END) as cancelCount,
                COALESCE(SUM(totalPrice), 0) as totalAmount,
                COALESCE(SUM(CASE WHEN payStatus = true THEN payPrice ELSE 0 END), 0) as paidAmount,
                COALESCE(SUM(refundPrice), 0) as refundAmount
            FROM trade_order
            WHERE 1=1
            <if test="reqVO.startTime != null">AND createTime >= #{reqVO.startTime}</if>
            <if test="reqVO.endTime != null">AND createTime <= #{reqVO.endTime}</if>
            <if test="reqVO.status != null">AND status = #{reqVO.status}</if>
            <if test="reqVO.type != null">AND type = #{reqVO.type}</if>
            <if test="reqVO.userId != null">AND userId = #{reqVO.userId}</if>
            """)
    TradeOrderReportRespVO selectReport(@Param("reqVO") TradeOrderReportReqVO reqVO);

    @Select("""
            SELECT
                DATE(createTime) as date,
                COUNT(*) as orderCount,
                SUM(totalPrice) as orderAmount,
                SUM(CASE WHEN payStatus = true THEN payPrice ELSE 0 END) as paidAmount,
                SUM(refundPrice) as refundAmount
            FROM trade_order
            WHERE 1=1
            <if test="reqVO.startTime != null">AND createTime >= #{reqVO.startTime}</if>
            <if test="reqVO.endTime != null">AND createTime <= #{reqVO.endTime}</if>
            GROUP BY DATE(createTime)
            ORDER BY date DESC
            """)
    List<TradeOrderDailyReportRespVO> selectDailyReport(@Param("reqVO") TradeOrderReportReqVO reqVO);

    default Integer selectOrderCountByStatus(Integer status) {
        return selectCount(new LambdaQueryWrapperX<TradeOrderDO>()
                .eqIfPresent(TradeOrderDO::getStatus, status));
    }

    default Long selectTotalAmount(TradeOrderReportReqVO reqVO) {
        return selectSum(TradeOrderDO::getTotalPrice, new LambdaQueryWrapperX<TradeOrderDO>()
                .betweenIfPresent(TradeOrderDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
                .eqIfPresent(TradeOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TradeOrderDO::getType, reqVO.getType())
                .eqIfPresent(TradeOrderDO::getUserId, reqVO.getUserId()));
    }

}