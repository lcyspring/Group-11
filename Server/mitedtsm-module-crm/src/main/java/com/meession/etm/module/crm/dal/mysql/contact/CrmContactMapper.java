package com.meession.etm.module.crm.dal.mysql.contact;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.contact.vo.CrmContactPageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmCustomerBirthdayPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CRM 联系人 Mapper
 *
 * @author 密讯
 */
@Mapper
public interface CrmContactMapper extends BaseMapperX<CrmContactDO> {

    /**
     * 锁定客户行，串行化同一客户的首联系人变更。
     */
    @Select("SELECT id FROM crm_customer WHERE id = #{customerId} AND deleted = 0 FOR UPDATE")
    Long lockCustomerById(@Param("customerId") Long customerId);

    /**
     * 锁后当前读联系人，避免 MySQL RR 快照返回加锁前的首联系人或客户归属状态。
     */
    @Select("SELECT * FROM crm_contact WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    CrmContactDO selectByIdForUpdate(@Param("id") Long id);

    /**
     * 使用当前读获取首联系人，避免事务早期普通查询形成的 RR 快照返回过期首联系人。
     */
    @Select("SELECT * FROM crm_contact WHERE customer_id = #{customerId} AND primary_contact = 1 " +
            "AND deleted = 0 ORDER BY id LIMIT 1 FOR UPDATE")
    CrmContactDO selectPrimaryContactByCustomerId(@Param("customerId") Long customerId);

    /**
     * 当前读查询同一客户下的重复手机号。调用方已锁定客户行，用于并发安全地执行唯一性校验。
     */
    @Select("SELECT id FROM crm_contact WHERE customer_id = #{customerId} AND mobile = #{mobile} " +
            "AND deleted = 0 AND (#{excludeId} IS NULL OR id != #{excludeId}) ORDER BY id LIMIT 1 FOR UPDATE")
    Long selectDuplicateMobileId(@Param("customerId") Long customerId, @Param("mobile") String mobile,
                                 @Param("excludeId") Long excludeId);

    default int unsetPrimaryContact(Long id) {
        return update(new LambdaUpdateWrapper<CrmContactDO>()
                .eq(CrmContactDO::getId, id)
                .eq(CrmContactDO::getPrimaryContact, true)
                .set(CrmContactDO::getPrimaryContact, false));
    }

    default int updateOwnerUserIdByCustomerId(Long customerId, Long ownerUserId) {
        return update(new LambdaUpdateWrapper<CrmContactDO>()
                .eq(CrmContactDO::getCustomerId, customerId)
                .set(CrmContactDO::getOwnerUserId, ownerUserId));
    }

    default PageResult<CrmContactDO> selectPageByCustomerId(CrmContactPageReqVO pageVO) {
        return selectPage(pageVO, new LambdaQueryWrapperX<CrmContactDO>()
                .eq(CrmContactDO::getCustomerId, pageVO.getCustomerId()) // 指定客户编号
                .likeIfPresent(CrmContactDO::getName, pageVO.getName())
                .eqIfPresent(CrmContactDO::getMobile, pageVO.getMobile())
                .eqIfPresent(CrmContactDO::getTelephone, pageVO.getTelephone())
                .eqIfPresent(CrmContactDO::getEmail, pageVO.getEmail())
                .eqIfPresent(CrmContactDO::getQq, pageVO.getQq())
                .eqIfPresent(CrmContactDO::getWechat, pageVO.getWechat())
                .orderByDesc(CrmContactDO::getId));
    }

    default PageResult<CrmContactDO> selectPageByBusinessId(CrmContactPageReqVO pageVO, Collection<Long> ids) {
        return selectPage(pageVO, new LambdaQueryWrapperX<CrmContactDO>()
                .in(CrmContactDO::getId, ids) // 指定联系人编号
                .likeIfPresent(CrmContactDO::getName, pageVO.getName())
                .eqIfPresent(CrmContactDO::getMobile, pageVO.getMobile())
                .eqIfPresent(CrmContactDO::getTelephone, pageVO.getTelephone())
                .eqIfPresent(CrmContactDO::getEmail, pageVO.getEmail())
                .eqIfPresent(CrmContactDO::getQq, pageVO.getQq())
                .eqIfPresent(CrmContactDO::getWechat, pageVO.getWechat())
                .orderByDesc(CrmContactDO::getId));
    }

    default PageResult<CrmContactDO> selectPage(CrmContactPageReqVO pageReqVO, Long userId) {
        MPJLambdaWrapperX<CrmContactDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CONTACT.getType(),
                CrmContactDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmContactDO.class)
                .likeIfPresent(CrmContactDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmContactDO::getMobile, pageReqVO.getMobile())
                .eqIfPresent(CrmContactDO::getTelephone, pageReqVO.getTelephone())
                .eqIfPresent(CrmContactDO::getEmail, pageReqVO.getEmail())
                .eqIfPresent(CrmContactDO::getQq, pageReqVO.getQq())
                .eqIfPresent(CrmContactDO::getWechat, pageReqVO.getWechat())
                .orderByDesc(CrmContactDO::getId);
        return selectJoinPage(pageReqVO, CrmContactDO.class, query);
    }

    default List<CrmContactDO> selectListByCustomerId(Long customerId) {
        return selectList(CrmContactDO::getCustomerId, customerId);
    }

    default List<CrmContactDO> selectPrimaryContactListByCustomerIds(Collection<Long> customerIds) {
        return selectList(new LambdaQueryWrapperX<CrmContactDO>()
                .in(CrmContactDO::getCustomerId, customerIds)
                .eq(CrmContactDO::getPrimaryContact, true));
    }

    default List<CrmContactDO> selectListByCustomerIdOwnerUserId(Long customerId, Long ownerUserId) {
        return selectList(CrmContactDO::getCustomerId, customerId,
                CrmContactDO::getOwnerUserId, ownerUserId);
    }

    default List<CrmContactDO> selectBirthdayContacts(String monthDay) {
        return selectList(new LambdaQueryWrapperX<CrmContactDO>()
                .apply("DATE_FORMAT(birthday, '%m-%d') = {0}", monthDay)
                .orderByAsc(CrmContactDO::getId));
    }

    default List<CrmContactDO> selectPrimaryContactsByLifecycleStatus(Integer lifecycleStatus) {
        MPJLambdaWrapperX<CrmContactDO> query = new MPJLambdaWrapperX<>();
        query.selectAll(CrmContactDO.class)
                .innerJoin(CrmCustomerDO.class, CrmCustomerDO::getId, CrmContactDO::getCustomerId)
                .eq(CrmContactDO::getPrimaryContact, true)
                .eq(CrmCustomerDO::getLifecycleStatus, lifecycleStatus)
                .orderByAsc(CrmContactDO::getId);
        return selectJoinList(CrmContactDO.class, query);
    }

    default List<CrmContactDO> selectPrimaryContactsByLifecycleChangedBetween(Integer lifecycleStatus,
                                                                               LocalDateTime begin,
                                                                               LocalDateTime end) {
        MPJLambdaWrapperX<CrmContactDO> query = new MPJLambdaWrapperX<>();
        query.selectAll(CrmContactDO.class)
                .innerJoin(CrmCustomerDO.class, CrmCustomerDO::getId, CrmContactDO::getCustomerId)
                .eq(CrmContactDO::getPrimaryContact, true)
                .eq(CrmCustomerDO::getLifecycleStatus, lifecycleStatus)
                .ge(CrmCustomerDO::getLifecycleStatusChangeTime, begin)
                .lt(CrmCustomerDO::getLifecycleStatusChangeTime, end)
                .orderByAsc(CrmContactDO::getId);
        return selectJoinList(CrmContactDO.class, query);
    }

    default PageResult<CrmContactDO> selectUpcomingBirthdayPage(CrmCustomerBirthdayPageReqVO request,
                                                                 LocalDate today, boolean all,
                                                                 Set<Long> ownerUserIds) {
        LocalDate endDate = today.plusDays(request.getUpcomingDays());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        String start = today.format(formatter);
        String end = endDate.format(formatter);
        MPJLambdaWrapperX<CrmContactDO> query = new MPJLambdaWrapperX<>();
        query.selectAll(CrmContactDO.class)
                .innerJoin(CrmCustomerDO.class, CrmCustomerDO::getId, CrmContactDO::getCustomerId)
                .isNotNull(CrmContactDO::getBirthday);
        if (endDate.getYear() == today.getYear()) {
            query.apply("DATE_FORMAT(t.birthday, '%m-%d') BETWEEN {0} AND {1}", start, end);
        } else {
            query.and(date -> date.apply("DATE_FORMAT(t.birthday, '%m-%d') >= {0}", start)
                    .or().apply("DATE_FORMAT(t.birthday, '%m-%d') <= {0}", end));
        }
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            query.and(keyword -> keyword.like(CrmContactDO::getName, request.getKeyword())
                    .or().like(CrmCustomerDO::getName, request.getKeyword()));
        }
        if (!all) {
            if (ownerUserIds.isEmpty()) query.eq(CrmContactDO::getOwnerUserId, -1L);
            else query.in(CrmContactDO::getOwnerUserId, ownerUserIds);
        }
        query.last("ORDER BY DATE_FORMAT(t.birthday, '%m-%d'), t.id");
        return selectJoinPage(request, CrmContactDO.class, query);
    }

}
