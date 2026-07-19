package com.meession.etm.module.crm.dal.mysql.clue;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmCluePageReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.publicpool.CrmCluePublicPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.enums.clue.CrmCluePoolStatusEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 线索 Mapper
 *
 * @author Wanwan
 */
@Mapper
public interface CrmClueMapper extends BaseMapperX<CrmClueDO> {

    /**
     * 当前读并锁定线索，使转换与更新、删除、转移及关联写操作串行化。
     */
    @Select("SELECT * FROM crm_clue WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    CrmClueDO selectByIdForUpdate(@Param("id") Long id);

    /**
     * 仅当线索仍处于期望的转换状态时更新，用于并发转换的原子抢占。
     */
    default int updateTransformStatusByIdAndTransformStatus(Long id, Boolean oldStatus, Boolean newStatus) {
        return update(new LambdaUpdateWrapper<CrmClueDO>()
                .eq(CrmClueDO::getId, id)
                .eq(CrmClueDO::getTransformStatus, oldStatus)
                .set(CrmClueDO::getTransformStatus, newStatus));
    }

    default int updateToPublicPool(Long id, Long expectedOwnerUserId, LocalDateTime poolEntryTime,
                                  String reasonCode, String reasonDetail) {
        return update(new LambdaUpdateWrapper<CrmClueDO>()
                .eq(CrmClueDO::getId, id)
                .eq(CrmClueDO::getOwnerUserId, expectedOwnerUserId)
                .eq(CrmClueDO::getPoolStatus, CrmCluePoolStatusEnum.OWNED.getStatus())
                .eq(CrmClueDO::getTransformStatus, false)
                .set(CrmClueDO::getOwnerUserId, null)
                .set(CrmClueDO::getPoolStatus, CrmCluePoolStatusEnum.PUBLIC.getStatus())
                .set(CrmClueDO::getPoolEntryTime, poolEntryTime)
                .set(CrmClueDO::getPoolPreviousOwnerUserId, expectedOwnerUserId)
                .set(CrmClueDO::getPoolReason, reasonCode)
                .set(CrmClueDO::getPoolReasonDetail, reasonDetail)
                .setSql("pool_cycle_count = pool_cycle_count + 1"));
    }

    default int updateClaimedFromPublicPool(Long id, Long ownerUserId, LocalDateTime ownerTime) {
        return update(new LambdaUpdateWrapper<CrmClueDO>()
                .eq(CrmClueDO::getId, id)
                .isNull(CrmClueDO::getOwnerUserId)
                .eq(CrmClueDO::getPoolStatus, CrmCluePoolStatusEnum.PUBLIC.getStatus())
                .eq(CrmClueDO::getTransformStatus, false)
                .set(CrmClueDO::getOwnerUserId, ownerUserId)
                .set(CrmClueDO::getOwnerTime, ownerTime)
                .set(CrmClueDO::getPoolStatus, CrmCluePoolStatusEnum.OWNED.getStatus())
                .set(CrmClueDO::getPoolEntryTime, null)
                .set(CrmClueDO::getPoolPreviousOwnerUserId, null)
                .set(CrmClueDO::getPoolReason, null)
                .set(CrmClueDO::getPoolReasonDetail, null));
    }

    default PageResult<CrmClueDO> selectPublicPage(CrmCluePublicPageReqVO pageReqVO) {
        LambdaQueryWrapperX<CrmClueDO> query = new LambdaQueryWrapperX<>();
        query.likeIfPresent(CrmClueDO::getName, pageReqVO.getName())
                .likeIfPresent(CrmClueDO::getMobile, pageReqVO.getMobile())
                .eqIfPresent(CrmClueDO::getIndustryId, pageReqVO.getIndustryId())
                .eqIfPresent(CrmClueDO::getLevel, pageReqVO.getLevel())
                .eqIfPresent(CrmClueDO::getSource, pageReqVO.getSource())
                .eq(CrmClueDO::getPoolStatus, CrmCluePoolStatusEnum.PUBLIC.getStatus())
                .isNull(CrmClueDO::getOwnerUserId)
                .eq(CrmClueDO::getTransformStatus, false)
                .orderByDesc(CrmClueDO::getPoolEntryTime)
                .orderByDesc(CrmClueDO::getId);
        return selectPage(pageReqVO, query);
    }

    default List<CrmClueDO> selectListByAutoPool(long afterId, LocalDateTime expireBefore,
                                                  int scanSize, int maxScanSize) {
        return selectList(new LambdaQueryWrapperX<CrmClueDO>()
                .eq(CrmClueDO::getPoolStatus, CrmCluePoolStatusEnum.OWNED.getStatus())
                .isNotNull(CrmClueDO::getOwnerUserId)
                .eq(CrmClueDO::getTransformStatus, false)
                .gt(CrmClueDO::getId, afterId)
                .apply("GREATEST(COALESCE(contact_last_time, owner_time, create_time), "
                        + "COALESCE(owner_time, create_time)) <= {0}", expireBefore)
                .orderByAsc(CrmClueDO::getId)
                .last("LIMIT " + Math.max(1, Math.min(scanSize, maxScanSize))));
    }

    default Long selectOwnedCountByUserId(Long userId) {
        return selectCount(new LambdaQueryWrapperX<CrmClueDO>()
                .eq(CrmClueDO::getOwnerUserId, userId)
                .eq(CrmClueDO::getPoolStatus, CrmCluePoolStatusEnum.OWNED.getStatus())
                .eq(CrmClueDO::getTransformStatus, false));
    }

    default PageResult<CrmClueDO> selectPage(CrmCluePageReqVO pageReqVO, Long userId) {
        MPJLambdaWrapperX<CrmClueDO> query = new MPJLambdaWrapperX<>();
        query.eq(CrmClueDO::getPoolStatus, CrmCluePoolStatusEnum.OWNED.getStatus());
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CLUE.getType(),
                CrmClueDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmClueDO.class)
                .likeIfPresent(CrmClueDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmClueDO::getTransformStatus, pageReqVO.getTransformStatus())
                .likeIfPresent(CrmClueDO::getTelephone, pageReqVO.getTelephone())
                .likeIfPresent(CrmClueDO::getMobile, pageReqVO.getMobile())
                .eqIfPresent(CrmClueDO::getIndustryId, pageReqVO.getIndustryId())
                .eqIfPresent(CrmClueDO::getLevel, pageReqVO.getLevel())
                .eqIfPresent(CrmClueDO::getSource, pageReqVO.getSource())
                .eqIfPresent(CrmClueDO::getFollowUpStatus, pageReqVO.getFollowUpStatus())
                .betweenIfPresent(CrmClueDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(CrmClueDO::getId);
        return selectJoinPage(pageReqVO, CrmClueDO.class, query);
    }

    default Long selectCountByFollow(Long userId) {
        MPJLambdaWrapperX<CrmClueDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CLUE.getType(),
                CrmClueDO::getId, userId, CrmSceneTypeEnum.OWNER.getType());
        // 未跟进 + 未转化
        query.eq(CrmClueDO::getFollowUpStatus, false)
                .eq(CrmClueDO::getTransformStatus, false);
        return selectCount(query);
    }

}
