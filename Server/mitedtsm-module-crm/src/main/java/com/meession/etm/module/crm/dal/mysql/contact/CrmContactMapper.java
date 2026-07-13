package com.meession.etm.module.crm.dal.mysql.contact;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.contact.vo.CrmContactPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

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

}
