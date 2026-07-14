package com.meession.etm.module.crm.dal.mysql.business;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.business.vo.business.CrmBusinessPageReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsFunnelReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsBusinessStagePageReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsBusinessStageReqVO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessStatusDO;
import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

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

    default int updateStatusIfUnchanged(Long id, Long oldStatusId, Integer oldEndStatus,
                                        Long newStatusId, Integer newEndStatus, String endRemark) {
        LambdaUpdateWrapper<CrmBusinessDO> update = new LambdaUpdateWrapper<CrmBusinessDO>()
                .eq(CrmBusinessDO::getId, id)
                .set(CrmBusinessDO::getStatusId, newStatusId)
                .set(CrmBusinessDO::getEndStatus, newEndStatus)
                .set(CrmBusinessDO::getEndRemark, endRemark);
        if (oldStatusId == null) {
            update.isNull(CrmBusinessDO::getStatusId);
        } else {
            update.eq(CrmBusinessDO::getStatusId, oldStatusId);
        }
        if (oldEndStatus == null) {
            update.isNull(CrmBusinessDO::getEndStatus);
        } else {
            update.eq(CrmBusinessDO::getEndStatus, oldEndStatus);
        }
        return update(update);
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

    default PageResult<CrmBusinessDO> selectForecastPage(CrmStatisticsFunnelReqVO pageVO) {
        return selectPage(pageVO, new LambdaQueryWrapperX<CrmBusinessDO>()
                .in(CrmBusinessDO::getOwnerUserId, pageVO.getUserIds())
                .betweenIfPresent(CrmBusinessDO::getDealTime, pageVO.getTimes())
                .isNull(CrmBusinessDO::getEndStatus)
                .isNotNull(CrmBusinessDO::getDealTime)
                .orderByAsc(CrmBusinessDO::getDealTime)
                .orderByAsc(CrmBusinessDO::getId));
    }

    default PageResult<CrmBusinessDO> selectStagePage(CrmStatisticsBusinessStagePageReqVO pageVO,
                                                       Integer stageSort) {
        MPJLambdaWrapperX<CrmBusinessDO> query = new MPJLambdaWrapperX<>();
        query.selectAll(CrmBusinessDO.class)
                .leftJoin(CrmBusinessStatusDO.class, on -> on
                        .eq(CrmBusinessStatusDO::getId, CrmBusinessDO::getStatusId)
                        .eq(CrmBusinessStatusDO::getTypeId, CrmBusinessDO::getStatusTypeId))
                .eq(CrmBusinessDO::getStatusTypeId, pageVO.getStatusTypeId())
                .in(CrmBusinessDO::getOwnerUserId, pageVO.getUserIds())
                .between(CrmBusinessDO::getCreateTime, pageVO.getTimes()[0], pageVO.getTimes()[1])
                .and(scope -> scope
                        .and(active -> active.isNull(CrmBusinessDO::getEndStatus)
                                .ge(CrmBusinessStatusDO::getSort, stageSort))
                        .or()
                        .eq(CrmBusinessDO::getEndStatus, CrmBusinessEndStatusEnum.WIN.getStatus()))
                .orderByAsc(CrmBusinessDO::getDealTime)
                .orderByAsc(CrmBusinessDO::getId);
        return selectJoinPage(pageVO, CrmBusinessDO.class, query);
    }

    default PageResult<CrmBusinessDO> selectWonPage(CrmStatisticsBusinessStageReqVO pageVO) {
        return selectPage(pageVO, new LambdaQueryWrapperX<CrmBusinessDO>()
                .eq(CrmBusinessDO::getStatusTypeId, pageVO.getStatusTypeId())
                .eq(CrmBusinessDO::getEndStatus, CrmBusinessEndStatusEnum.WIN.getStatus())
                .in(CrmBusinessDO::getOwnerUserId, pageVO.getUserIds())
                .between(CrmBusinessDO::getCreateTime, pageVO.getTimes()[0], pageVO.getTimes()[1])
                .orderByAsc(CrmBusinessDO::getDealTime)
                .orderByAsc(CrmBusinessDO::getId));
    }

}
