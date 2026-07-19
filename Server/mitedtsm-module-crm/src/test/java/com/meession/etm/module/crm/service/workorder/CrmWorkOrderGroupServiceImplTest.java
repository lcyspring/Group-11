package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.module.crm.controller.admin.workorder.vo.CrmWorkOrderGroupSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupMemberDO;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderGroupMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderGroupMemberMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderMapper;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.WORK_ORDER_GROUP_IN_USE;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.WORK_ORDER_GROUP_MANAGER_NOT_MEMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmWorkOrderGroupServiceImplTest {

    @Mock private CrmWorkOrderGroupMapper groupMapper;
    @Mock private CrmWorkOrderGroupMemberMapper memberMapper;
    @Mock private CrmWorkOrderMapper workOrderMapper;
    @Mock private AdminUserApi adminUserApi;
    @InjectMocks private CrmWorkOrderGroupServiceImpl service;

    @Test
    void createGroupPersistsOrderedDistinctMembers() {
        doAnswer(invocation -> { ((CrmWorkOrderGroupDO) invocation.getArgument(0)).setId(9L); return 1; })
                .when(groupMapper).insert(any(CrmWorkOrderGroupDO.class));

        Long id = service.saveGroup(request().setMemberUserIds(List.of(2L, 3L, 2L)));

        assertEquals(9L, id);
        verify(adminUserApi).validateUserList(java.util.Set.of(2L, 3L));
        ArgumentCaptor<CrmWorkOrderGroupMemberDO> members = ArgumentCaptor.forClass(CrmWorkOrderGroupMemberDO.class);
        verify(memberMapper, times(2)).insert(members.capture());
        assertEquals(List.of(2L, 3L), members.getAllValues().stream().map(CrmWorkOrderGroupMemberDO::getUserId).toList());
        assertEquals(List.of(1, 2), members.getAllValues().stream().map(CrmWorkOrderGroupMemberDO::getSort).toList());
    }

    @Test
    void managerMustAlsoBeMember() {
        assertServiceException(() -> service.saveGroup(request().setManagerUserId(8L)),
                WORK_ORDER_GROUP_MANAGER_NOT_MEMBER);
        verify(groupMapper, never()).insert(any(CrmWorkOrderGroupDO.class));
    }

    @Test
    void referencedGroupCannotBeDeleted() {
        when(groupMapper.selectById(9L)).thenReturn(new CrmWorkOrderGroupDO().setId(9L));
        doReturn(1L).when(workOrderMapper)
                .selectCount(Mockito.<SFunction<CrmWorkOrderDO, ?>>any(), eq(9L));
        assertServiceException(() -> service.deleteGroup(9L), WORK_ORDER_GROUP_IN_USE);
    }

    private static CrmWorkOrderGroupSaveReqVO request() {
        return new CrmWorkOrderGroupSaveReqVO().setCode("service_a").setName("客服一组")
                .setManagerUserId(2L).setSupportedTypes(List.of(1, 3))
                .setMemberUserIds(List.of(2L, 3L)).setStatus(0).setSort(1);
    }
}
