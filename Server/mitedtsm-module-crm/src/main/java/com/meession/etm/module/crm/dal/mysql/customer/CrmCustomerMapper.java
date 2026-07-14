package com.meession.etm.module.crm.dal.mysql.customer;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolConfigDO;
import com.meession.etm.module.crm.service.customer.bo.CrmCustomerPoolRecycleRuleConfig;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;
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

    default int updateOwnerUserIdById(Long id, Long ownerUserId) {
        return update(new LambdaUpdateWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getId, id)
                .set(CrmCustomerDO::getOwnerUserId, ownerUserId));
    }

    default PageResult<CrmCustomerDO> selectPage(CrmCustomerPageReqVO pageReqVO, Long ownerUserId) {
        MPJLambdaWrapperX<CrmCustomerDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        if (Boolean.TRUE.equals(pageReqVO.getPool())) {
            query.isNull(CrmCustomerDO::getOwnerUserId);
        } else {
            CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CUSTOMER.getType(),
                    CrmCustomerDO::getId, ownerUserId, pageReqVO.getSceneType());
        }
        // 拼接自身的查询条件
        query.selectAll(CrmCustomerDO.class)
                .likeIfPresent(CrmCustomerDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmCustomerDO::getMobile, pageReqVO.getMobile())
                .eqIfPresent(CrmCustomerDO::getIndustryId, pageReqVO.getIndustryId())
                .eqIfPresent(CrmCustomerDO::getLevel, pageReqVO.getLevel())
                .eqIfPresent(CrmCustomerDO::getSource, pageReqVO.getSource())
                .eqIfPresent(CrmCustomerDO::getFollowUpStatus, pageReqVO.getFollowUpStatus());

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

    default CrmCustomerDO selectByCustomerName(String name) {
        return selectOne(CrmCustomerDO::getName, name);
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
    default List<CrmCustomerDO> selectListByAutoPool(CrmCustomerPoolConfigDO poolConfig) {
        LambdaQueryWrapper<CrmCustomerDO> query = new LambdaQueryWrapper<>();
        query.gt(CrmCustomerDO::getOwnerUserId, 0);
        // 未锁定 + 未成交
        query.eq(CrmCustomerDO::getLockStatus, false).eq(CrmCustomerDO::getDealStatus, false);
        // 已经超时
        LocalDateTime dealExpireTime = LocalDateTime.now().minusDays(poolConfig.getDealExpireDays());
        LocalDateTime contactExpireTime = LocalDateTime.now().minusDays(poolConfig.getContactExpireDays());
        query.and(q -> {
            // 情况一：成交超时
            q.lt(CrmCustomerDO::getOwnerTime, dealExpireTime)
            // 情况二：跟进超时
            .or(w -> w.lt(CrmCustomerDO::getOwnerTime, contactExpireTime)
                    .and(p -> p.lt(CrmCustomerDO::getContactLastTime, contactExpireTime)
                            .or().isNull(CrmCustomerDO::getContactLastTime)));
        });
        return selectList(query);
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

    default List<CrmCustomerDO> selectListByAutoPoolConfig(CrmCustomerPoolRecycleRuleConfig config) {
        LambdaQueryWrapper<CrmCustomerDO> query = new LambdaQueryWrapper<>();
        query.gt(CrmCustomerDO::getOwnerUserId, 0);

        if (config.getExcludeLocked() == null || config.getExcludeLocked()) {
            query.eq(CrmCustomerDO::getLockStatus, false);
        }
        if (config.getExcludeDealed() == null || config.getExcludeDealed()) {
            query.eq(CrmCustomerDO::getDealStatus, false);
        }

        Integer contactExpireDays = config.getContactExpireDays() != null ? config.getContactExpireDays() : 30;
        Integer dealExpireDays = config.getDealExpireDays() != null ? config.getDealExpireDays() : 90;

        LocalDateTime dealExpireTime = LocalDateTime.now().minusDays(dealExpireDays);
        LocalDateTime contactExpireTime = LocalDateTime.now().minusDays(contactExpireDays);

        query.and(q -> {
            q.lt(CrmCustomerDO::getOwnerTime, dealExpireTime)
                    .or(w -> w.lt(CrmCustomerDO::getOwnerTime, contactExpireTime)
                            .and(p -> p.lt(CrmCustomerDO::getContactLastTime, contactExpireTime)
                                    .or().isNull(CrmCustomerDO::getContactLastTime)));
        });

        if (config.getMinCustomerLevel() != null) {
            query.ge(CrmCustomerDO::getLevel, config.getMinCustomerLevel());
        }

        return selectList(query);
    }

    default int updatePoolStatus(Long id, Integer poolStatus, LocalDateTime poolTime,
                                 String poolReason, Long poolRuleId) {
        return update(new LambdaUpdateWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getId, id)
                .set(CrmCustomerDO::getPoolStatus, poolStatus)
                .set(CrmCustomerDO::getPoolTime, poolTime)
                .set(CrmCustomerDO::getPoolReason, poolReason)
                .set(CrmCustomerDO::getPoolRuleId, poolRuleId));
    }

    default int updateReceiveInfo(Long id, Long ownerUserId, LocalDateTime ownerTime,
                                  LocalDateTime lastReceiveTime, LocalDateTime receiveFreezeEndTime) {
        return update(new LambdaUpdateWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getId, id)
                .set(CrmCustomerDO::getOwnerUserId, ownerUserId)
                .set(CrmCustomerDO::getOwnerTime, ownerTime)
                .set(CrmCustomerDO::getLastReceiveTime, lastReceiveTime)
                .set(CrmCustomerDO::getReceiveFreezeEndTime, receiveFreezeEndTime)
                .set(CrmCustomerDO::getPoolStatus, 0));
    }

    default List<CrmCustomerDO> selectDuplicateCustomers(String name, String mobile, String telephone,
                                                          String email, String qq, String wechat, Boolean strictMatch) {
        LambdaQueryWrapper<CrmCustomerDO> query = new LambdaQueryWrapper<>();

        if (strictMatch != null && strictMatch) {
            if (ObjUtil.isNotEmpty(name)) {
                query.eq(CrmCustomerDO::getName, name);
            }
            if (ObjUtil.isNotEmpty(mobile)) {
                query.eq(CrmCustomerDO::getMobile, mobile);
            }
            if (ObjUtil.isNotEmpty(telephone)) {
                query.eq(CrmCustomerDO::getTelephone, telephone);
            }
            if (ObjUtil.isNotEmpty(email)) {
                query.eq(CrmCustomerDO::getEmail, email);
            }
            if (ObjUtil.isNotEmpty(qq)) {
                query.eq(CrmCustomerDO::getQq, qq);
            }
            if (ObjUtil.isNotEmpty(wechat)) {
                query.eq(CrmCustomerDO::getWechat, wechat);
            }
        } else {
            boolean hasCondition = false;
            if (ObjUtil.isNotEmpty(name)) {
                query.like(CrmCustomerDO::getName, name);
                hasCondition = true;
            }
            if (ObjUtil.isNotEmpty(mobile)) {
                query.eq(CrmCustomerDO::getMobile, mobile);
                hasCondition = true;
            }
            if (ObjUtil.isNotEmpty(telephone)) {
                query.eq(CrmCustomerDO::getTelephone, telephone);
                hasCondition = true;
            }
            if (ObjUtil.isNotEmpty(email)) {
                query.eq(CrmCustomerDO::getEmail, email);
                hasCondition = true;
            }
            if (ObjUtil.isNotEmpty(qq)) {
                query.eq(CrmCustomerDO::getQq, qq);
                hasCondition = true;
            }
            if (ObjUtil.isNotEmpty(wechat)) {
                query.eq(CrmCustomerDO::getWechat, wechat);
                hasCondition = true;
            }
            if (!hasCondition) {
                return Collections.emptyList();
            }
        }

        query.last("LIMIT 20");
        return selectList(query);
    }

}
