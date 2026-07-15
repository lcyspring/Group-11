package com.meession.etm.module.crm.dal.mysql.customer;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolConfigDO;
import com.meession.etm.module.crm.dal.dataobject.permission.CrmPermissionDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolStatusEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 客户 Mapper
 *
 * @author Wanwan
 */
@Mapper
public interface CrmCustomerMapper extends BaseMapperX<CrmCustomerDO> {

    default Long selectCountByLockStatusAndOwnerUserId(Boolean lockStatus, Long ownerUserId) {
        return selectCount(new LambdaUpdateWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getLockStatus, lockStatus)
                .eq(CrmCustomerDO::getOwnerUserId, ownerUserId));
    }

    default Long selectCountByDealStatusAndOwnerUserId(@Nullable Boolean dealStatus, Long ownerUserId) {
        return selectCount(new LambdaQueryWrapperX<CrmCustomerDO>()
                .eqIfPresent(CrmCustomerDO::getDealStatus, dealStatus)
                .eq(CrmCustomerDO::getOwnerUserId, ownerUserId));
    }

    /**
     * 按主键顺序锁定客户，使公海批量领取/分配串行。
     */
    default List<CrmCustomerDO> selectByIdsForUpdate(Collection<Long> ids) {
        return selectList(new LambdaQueryWrapper<CrmCustomerDO>()
                .in(CrmCustomerDO::getId, ids)
                .orderByAsc(CrmCustomerDO::getId)
                .last("FOR UPDATE"));
    }

    /** 锁定单个客户，防止并发生命周期命令产生丢失更新或重复历史。 */
    default CrmCustomerDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getId, id)
                .last("FOR UPDATE"));
    }

    default int updateOwnerUserIdById(Long id, Long ownerUserId) {
        return update(new LambdaUpdateWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getId, id)
                .set(CrmCustomerDO::getOwnerUserId, ownerUserId));
    }

    default int updateToPublicPool(Long id, Long previousOwnerUserId, LocalDateTime poolEntryTime,
                                   String poolReason) {
        return update(new LambdaUpdateWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getId, id)
                .eq(CrmCustomerDO::getPoolStatus, CrmCustomerPoolStatusEnum.OWNED.getStatus())
                .isNotNull(CrmCustomerDO::getOwnerUserId)
                .set(CrmCustomerDO::getOwnerUserId, null)
                .set(CrmCustomerDO::getPoolStatus, CrmCustomerPoolStatusEnum.PUBLIC.getStatus())
                .set(CrmCustomerDO::getPoolEntryTime, poolEntryTime)
                .set(CrmCustomerDO::getPoolPreviousOwnerUserId, previousOwnerUserId)
                .set(CrmCustomerDO::getPoolReason, poolReason)
                .set(CrmCustomerDO::getGarbageTime, null)
                .set(CrmCustomerDO::getGarbageReason, null)
                .setSql("pool_cycle_count = pool_cycle_count + 1"));
    }

    default int updateClaimedFromPublicPool(Long id, Long ownerUserId, LocalDateTime ownerTime) {
        return update(new LambdaUpdateWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getId, id)
                .eq(CrmCustomerDO::getPoolStatus, CrmCustomerPoolStatusEnum.PUBLIC.getStatus())
                .isNull(CrmCustomerDO::getOwnerUserId)
                .set(CrmCustomerDO::getOwnerUserId, ownerUserId)
                .set(CrmCustomerDO::getOwnerTime, ownerTime)
                .set(CrmCustomerDO::getPoolStatus, CrmCustomerPoolStatusEnum.OWNED.getStatus())
                .set(CrmCustomerDO::getPoolEntryTime, null)
                .set(CrmCustomerDO::getPoolPreviousOwnerUserId, null)
                .set(CrmCustomerDO::getPoolReason, null));
    }

    /**
     * 显式更新上级客户编号。不能依赖 updateById，因为 MyBatis 默认会忽略 null，导致无法解除父关系。
     */
    default int updateParentCustomerIdById(Long id, Long parentCustomerId) {
        return update(new LambdaUpdateWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getId, id)
                .set(CrmCustomerDO::getParentCustomerId, parentCustomerId));
    }

    default PageResult<CrmCustomerDO> selectPage(CrmCustomerPageReqVO pageReqVO, Long ownerUserId) {
        MPJLambdaWrapperX<CrmCustomerDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        if (Boolean.TRUE.equals(pageReqVO.getPool())) {
            query.eq(CrmCustomerDO::getPoolStatus, CrmCustomerPoolStatusEnum.PUBLIC.getStatus())
                    .isNull(CrmCustomerDO::getOwnerUserId)
                    .orderByDesc(CrmCustomerDO::getPoolEntryTime)
                    .orderByDesc(CrmCustomerDO::getId);
        } else {
            query.eq(CrmCustomerDO::getPoolStatus, CrmCustomerPoolStatusEnum.OWNED.getStatus());
            CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CUSTOMER.getType(),
                    CrmCustomerDO::getId, ownerUserId, pageReqVO.getSceneType());
        }
        // 拼接自身的查询条件
        query.selectAll(CrmCustomerDO.class)
                .likeIfPresent(CrmCustomerDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmCustomerDO::getParentCustomerId, pageReqVO.getParentCustomerId())
                .eqIfPresent(CrmCustomerDO::getMobile, pageReqVO.getMobile())
                .eqIfPresent(CrmCustomerDO::getIndustryId, pageReqVO.getIndustryId())
                .eqIfPresent(CrmCustomerDO::getLevel, pageReqVO.getLevel())
                .eqIfPresent(CrmCustomerDO::getSource, pageReqVO.getSource())
                .eqIfPresent(CrmCustomerDO::getFollowUpStatus, pageReqVO.getFollowUpStatus())
                .eqIfPresent(CrmCustomerDO::getLifecycleStatus, pageReqVO.getLifecycleStatus());
        appendContactNameConditions(query, pageReqVO);

        // backlog 查询
        if (ObjUtil.isNotNull(pageReqVO.getContactStatus())) {
            Assert.isNull(pageReqVO.getPool(), "pool 必须是 null");
            LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
            LocalDateTime endOfToday = LocalDateTimeUtil.endOfDay(LocalDateTime.now());
            if (pageReqVO.getContactStatus().equals(CrmCustomerPageReqVO.CONTACT_TODAY)) { // 今天需联系
                query.between(CrmCustomerDO::getContactNextTime, beginOfToday, endOfToday);
            } else if (pageReqVO.getContactStatus().equals(CrmCustomerPageReqVO.CONTACT_EXPIRED)) { // 已逾期
                query.lt(CrmCustomerDO::getContactNextTime, beginOfToday);
            } else if (pageReqVO.getContactStatus().equals(CrmCustomerPageReqVO.CONTACT_ALREADY)) { // 已联系
                query.between(CrmCustomerDO::getContactLastTime, beginOfToday, endOfToday);
            } else {
                throw new IllegalArgumentException("未知联系状态：" + pageReqVO.getContactStatus());
            }
        }
        return selectJoinPage(pageReqVO, CrmCustomerDO.class, query);
    }

    /**
     * 按任意联系人或首联系人姓名筛选。联系人是一对多关系，因此必须使用 DISTINCT 保持客户分页不重复。
     */
    private static void appendContactNameConditions(MPJLambdaWrapperX<CrmCustomerDO> query,
                                                    CrmCustomerPageReqVO pageReqVO) {
        boolean joined = false;
        if (StrUtil.isNotBlank(pageReqVO.getContactName())) {
            query.innerJoin(CrmContactDO.class, "contact_filter",
                            CrmContactDO::getCustomerId, CrmCustomerDO::getId)
                    .like("contact_filter", CrmContactDO::getName, pageReqVO.getContactName());
            joined = true;
        }
        if (StrUtil.isNotBlank(pageReqVO.getPrimaryContactName())) {
            query.innerJoin(CrmContactDO.class, "primary_contact_filter",
                            CrmContactDO::getCustomerId, CrmCustomerDO::getId)
                    .eq("primary_contact_filter", CrmContactDO::getPrimaryContact, true)
                    .like("primary_contact_filter", CrmContactDO::getName, pageReqVO.getPrimaryContactName());
            joined = true;
        }
        if (joined) {
            query.distinct();
        }
    }

    default CrmCustomerDO selectByCustomerName(String name) {
        return selectOne(CrmCustomerDO::getName, name);
    }

    /**
     * 锁定当前租户的有效客户层级快照。所有层级写操作都先调用本方法，串行化同租户内的
     * 关系变更，避免两个并发请求分别通过校验后形成环。
     */
    default List<CrmCustomerDO> selectHierarchyListForUpdate() {
        return selectList(new LambdaQueryWrapper<CrmCustomerDO>()
                .select(CrmCustomerDO::getId, CrmCustomerDO::getParentCustomerId)
                .orderByAsc(CrmCustomerDO::getId)
                .last("FOR UPDATE"));
    }

    default List<CrmCustomerDO> selectDuplicateList(String name, String mobile, Long excludeId, Long userId) {
        MPJLambdaWrapperX<CrmCustomerDO> query = new MPJLambdaWrapperX<>();
        if (!CrmPermissionUtils.isCrmAdmin()) {
            query.innerJoin(CrmPermissionDO.class, on -> on.eq(CrmPermissionDO::getBizType,
                            CrmBizTypeEnum.CRM_CUSTOMER.getType())
                    .eq(CrmPermissionDO::getBizId, CrmCustomerDO::getId)
                    .eq(CrmPermissionDO::getUserId, userId)
                    .in(CrmPermissionDO::getLevel, CrmPermissionLevelEnum.OWNER.getLevel(),
                            CrmPermissionLevelEnum.READ.getLevel(), CrmPermissionLevelEnum.WRITE.getLevel()));
        }
        query.selectAll(CrmCustomerDO.class)
                .neIfPresent(CrmCustomerDO::getId, excludeId)
                .and(condition -> {
                    if (StrUtil.isNotBlank(name)) {
                        condition.eq(CrmCustomerDO::getName, name);
                    }
                    if (StrUtil.isNotBlank(mobile)) {
                        if (StrUtil.isNotBlank(name)) {
                            condition.or();
                        }
                        condition.eq(CrmCustomerDO::getMobile, mobile);
                    }
                })
                .orderByAsc(CrmCustomerDO::getId)
                .last("LIMIT 20");
        return selectJoinList(CrmCustomerDO.class, query);
    }

    default PageResult<CrmCustomerDO> selectPutPoolRemindCustomerPage(CrmCustomerPageReqVO pageReqVO,
                                                                      CrmCustomerPoolConfigDO poolConfig,
                                                                      Long ownerUserId) {
        final MPJLambdaWrapperX<CrmCustomerDO> query = buildPutPoolRemindCustomerQuery(pageReqVO, poolConfig, ownerUserId);
        return selectJoinPage(pageReqVO, CrmCustomerDO.class, query.selectAll(CrmCustomerDO.class));
    }

    default Long selectPutPoolRemindCustomerCount(CrmCustomerPageReqVO pageReqVO,
                                                  CrmCustomerPoolConfigDO poolConfig,
                                                  Long userId) {
        final MPJLambdaWrapperX<CrmCustomerDO> query = buildPutPoolRemindCustomerQuery(pageReqVO, poolConfig, userId);
        return selectCount(query);
    }

    private static MPJLambdaWrapperX<CrmCustomerDO> buildPutPoolRemindCustomerQuery(CrmCustomerPageReqVO pageReqVO,
                                                                                    CrmCustomerPoolConfigDO poolConfig,
                                                                                    Long ownerUserId) {
        MPJLambdaWrapperX<CrmCustomerDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CUSTOMER.getType(),
                CrmCustomerDO::getId, ownerUserId, pageReqVO.getSceneType());

        // 未锁定 + 未成交
        query.eq(CrmCustomerDO::getLockStatus, false).eq(CrmCustomerDO::getDealStatus, false);

        // 情况一：未成交提醒日期区间
        Integer dealExpireDays = poolConfig.getDealExpireDays();
        LocalDateTime startDealRemindTime = LocalDateTime.now().minusDays(dealExpireDays);
        LocalDateTime endDealRemindTime = LocalDateTime.now()
                .minusDays(Math.max(dealExpireDays - poolConfig.getNotifyDays(), 0));
        // 情况二：未跟进提醒日期区间
        Integer contactExpireDays = poolConfig.getContactExpireDays();
        LocalDateTime startContactRemindTime = LocalDateTime.now().minusDays(contactExpireDays);
        LocalDateTime endContactRemindTime = LocalDateTime.now()
                .minusDays(Math.max(contactExpireDays - poolConfig.getNotifyDays(), 0));
        query.and(q -> {
            // 情况一：成交超时提醒
            q.between(CrmCustomerDO::getOwnerTime, startDealRemindTime, endDealRemindTime)
            // 情况二：跟进超时提醒
            .or(w -> w.between(CrmCustomerDO::getOwnerTime, startContactRemindTime, endContactRemindTime)
                    .and(p -> p.between(CrmCustomerDO::getContactLastTime, startContactRemindTime, endContactRemindTime)
                            .or().isNull(CrmCustomerDO::getContactLastTime)));
        });
        return query;
    }

    /**
     * 获得需要过期到公海的客户列表
     *
     * @return 客户列表
     */
    default List<CrmCustomerDO> selectListByAutoPool(CrmCustomerPoolConfigDO poolConfig, Long afterId,
                                                     LocalDateTime now, int scanSize, int maxScanSize) {
        int highValueMultiplier = poolConfig.getHighValueExpireMultiplier();
        LocalDateTime normalDealExpireTime = now.minusDays(poolConfig.getDealExpireDays());
        LocalDateTime highDealExpireTime = now.minusDays((long) poolConfig.getDealExpireDays()
                * highValueMultiplier);
        LocalDateTime normalContactExpireTime = now.minusDays(poolConfig.getContactExpireDays());
        LocalDateTime highContactExpireTime = now.minusDays((long) poolConfig.getContactExpireDays()
                * highValueMultiplier);
        LambdaQueryWrapper<CrmCustomerDO> query = new LambdaQueryWrapper<>();
        query.eq(CrmCustomerDO::getPoolStatus, CrmCustomerPoolStatusEnum.OWNED.getStatus())
                .gt(CrmCustomerDO::getOwnerUserId, 0)
                .gt(CrmCustomerDO::getId, afterId)
                .eq(CrmCustomerDO::getLockStatus, false)
                .eq(CrmCustomerDO::getDealStatus, false)
                .and(scope -> scope
                        .and(high -> high.ge(CrmCustomerDO::getLevel,
                                        poolConfig.getHighValueLevelThreshold())
                                .and(expired -> appendExpiredCondition(expired, highDealExpireTime,
                                        highContactExpireTime)))
                        .or(normal -> normal
                                .and(level -> level.isNull(CrmCustomerDO::getLevel)
                                        .or().lt(CrmCustomerDO::getLevel,
                                                poolConfig.getHighValueLevelThreshold()))
                                .and(expired -> appendExpiredCondition(expired, normalDealExpireTime,
                                        normalContactExpireTime))))
                .orderByAsc(CrmCustomerDO::getId)
                .last("LIMIT " + Math.max(1, Math.min(scanSize, maxScanSize)));
        return selectList(query);
    }

    private static void appendExpiredCondition(LambdaQueryWrapper<CrmCustomerDO> query,
                                               LocalDateTime dealExpireTime,
                                               LocalDateTime contactExpireTime) {
        query.apply("COALESCE(owner_time, create_time) < {0}", dealExpireTime)
                .or(follow -> follow
                        .apply("COALESCE(owner_time, create_time) < {0}", contactExpireTime)
                        .and(last -> last.isNull(CrmCustomerDO::getContactLastTime)
                                .or().lt(CrmCustomerDO::getContactLastTime, contactExpireTime)));
    }

    default Long selectCountByTodayContact(Long ownerUserId) {
        MPJLambdaWrapperX<CrmCustomerDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CUSTOMER.getType(),
                CrmCustomerDO::getId, ownerUserId, CrmSceneTypeEnum.OWNER.getType());
        // 今天需联系
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        LocalDateTime endOfToday = LocalDateTimeUtil.endOfDay(LocalDateTime.now());
        query.between(CrmCustomerDO::getContactNextTime, beginOfToday, endOfToday);
        return selectCount(query);
    }

    default Long selectCountByFollow(Long ownerUserId) {
        MPJLambdaWrapperX<CrmCustomerDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CUSTOMER.getType(),
                CrmCustomerDO::getId, ownerUserId, CrmSceneTypeEnum.OWNER.getType());
        // 未跟进
        query.eq(CrmClueDO::getFollowUpStatus, false);
        return selectCount(query);
    }

}
