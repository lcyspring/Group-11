package com.meession.etm.module.crm.dal.mysql.business;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.business.vo.business.CrmBusinessPageReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsFunnelReqVO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 商机 Mapper
 *
 * @author ljlleo
 */
@Mapper
public interface CrmBusinessMapper extends BaseMapperX<CrmBusinessDO> {

    default int updateOwnerUserIdById(Long id, Long ownerUserId) {
        return update(new LambdaUpdateWrapper<CrmBusinessDO>()
                .eq(CrmBusinessDO::getId, id)
                .set(CrmBusinessDO::getOwnerUserId, ownerUserId));
    }

    default PageResult<CrmBusinessDO> selectPageByCustomerId(CrmBusinessPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmBusinessDO>()
                .eq(CrmBusinessDO::getCustomerId, pageReqVO.getCustomerId()) // 指定客户编号
                .likeIfPresent(CrmBusinessDO::getName, pageReqVO.getName())
                .orderByDesc(CrmBusinessDO::getId));
    }

    default PageResult<CrmBusinessDO> selectPageByContactId(CrmBusinessPageReqVO pageReqVO, Collection<Long> businessIds) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmBusinessDO>()
                .in(CrmBusinessDO::getId, businessIds) // 指定商机编号
                .likeIfPresent(CrmBusinessDO::getName, pageReqVO.getName())
                .orderByDesc(CrmBusinessDO::getId));
    }

    default PageResult<CrmBusinessDO> selectPage(CrmBusinessPageReqVO pageReqVO, Long userId) {
        MPJLambdaWrapperX<CrmBusinessDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_BUSINESS.getType(),
                CrmBusinessDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmBusinessDO.class)
                .likeIfPresent(CrmBusinessDO::getName, pageReqVO.getName())
                .orderByDesc(CrmBusinessDO::getId);
        return selectJoinPage(pageReqVO, CrmBusinessDO.class, query);
    }

    default Long selectCountByStatusTypeId(Long statusTypeId) {
        return selectCount(CrmBusinessDO::getStatusTypeId, statusTypeId);
    }

    default List<CrmBusinessDO> selectListByCustomerIdOwnerUserId(Long customerId, Long ownerUserId) {
        return selectList(new LambdaQueryWrapperX<CrmBusinessDO>()
                .eq(CrmBusinessDO::getCustomerId, customerId)
                .eq(CrmBusinessDO::getOwnerUserId, ownerUserId));
    }

    default PageResult<CrmBusinessDO> selectPage(CrmStatisticsFunnelReqVO pageVO) {
        return selectPage(pageVO, new LambdaQueryWrapperX<CrmBusinessDO>()
                .in(CrmBusinessDO::getOwnerUserId, pageVO.getUserIds())
                .betweenIfPresent(CrmBusinessDO::getCreateTime, pageVO.getTimes()));
    }

    default Long selectTotalDealAmountByCustomerId(Long customerId) {
        return selectSum(CrmBusinessDO::getTotalPrice,
                new LambdaQueryWrapperX<CrmBusinessDO>()
                        .eq(CrmBusinessDO::getCustomerId, customerId));
    }

    default Map<Long, Long> selectBusinessCountMapByCustomerIds(Collection<Long> customerIds) {
        return selectMaps(new LambdaQueryWrapperX<CrmBusinessDO>()
                        .select(CrmBusinessDO::getCustomerId, "COUNT(*) as count")
                        .in(CrmBusinessDO::getCustomerId, customerIds)
                        .groupBy(CrmBusinessDO::getCustomerId))
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((Number) row.get("customerId")).longValue(),
                        row -> ((Number) row.get("count")).longValue()
                ));
    }

    default Map<Long, Long> selectTotalDealAmountMapByCustomerIds(Collection<Long> customerIds) {
        return selectMaps(new LambdaQueryWrapperX<CrmBusinessDO>()
                        .select(CrmBusinessDO::getCustomerId, "SUM(total_price) as totalAmount")
                        .in(CrmBusinessDO::getCustomerId, customerIds)
                        .groupBy(CrmBusinessDO::getCustomerId))
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((Number) row.get("customerId")).longValue(),
                        row -> {
                            Object totalAmount = row.get("totalAmount");
                            return totalAmount != null ? ((Number) totalAmount).longValue() : 0L;
                        }
                ));
    }

}
