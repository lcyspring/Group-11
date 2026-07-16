package com.meession.etm.module.crm.dal.mysql.workorder;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupMemberDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface CrmWorkOrderGroupMemberMapper extends BaseMapperX<CrmWorkOrderGroupMemberDO> {

    default List<CrmWorkOrderGroupMemberDO> selectByGroupIds(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) return Collections.emptyList();
        return selectList(new LambdaQueryWrapperX<CrmWorkOrderGroupMemberDO>()
                .in(CrmWorkOrderGroupMemberDO::getGroupId, groupIds)
                .orderByAsc(CrmWorkOrderGroupMemberDO::getSort).orderByAsc(CrmWorkOrderGroupMemberDO::getId));
    }

    default List<CrmWorkOrderGroupMemberDO> selectByUserId(Long userId) {
        return selectList(CrmWorkOrderGroupMemberDO::getUserId, userId);
    }

    default void deleteByGroupId(Long groupId) {
        delete(CrmWorkOrderGroupMemberDO::getGroupId, groupId);
    }

    /**
     * 成员关系没有业务历史，更新组成员时物理清理旧关系，避免逻辑删除行占用唯一键。
     */
    @Delete("DELETE FROM crm_work_order_group_member WHERE group_id = #{groupId}")
    int physicalDeleteByGroupId(@Param("groupId") Long groupId);
}
