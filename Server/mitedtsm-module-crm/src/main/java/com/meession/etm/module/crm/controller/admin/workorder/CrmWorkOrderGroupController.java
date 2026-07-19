package com.meession.etm.module.crm.controller.admin.workorder;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.workorder.vo.CrmWorkOrderGroupRespVO;
import com.meession.etm.module.crm.controller.admin.workorder.vo.CrmWorkOrderGroupSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO;
import com.meession.etm.module.crm.service.workorder.CrmWorkOrderGroupService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 客服处理组")
@RestController
@RequestMapping("/crm/work-order-group")
public class CrmWorkOrderGroupController {

    @Resource private CrmWorkOrderGroupService groupService;
    @Resource private AdminUserApi adminUserApi;

    @GetMapping("/list")
    @Operation(summary = "获得客服处理组列表")
    @PreAuthorize("@ss.hasPermission('crm:work-order-group:query')")
    public CommonResult<List<CrmWorkOrderGroupRespVO>> list() {
        List<CrmWorkOrderGroupDO> groups = groupService.getGroupList();
        Map<Long, List<Long>> members = groupService.getMemberUserIdsMap(groups.stream().map(CrmWorkOrderGroupDO::getId).toList());
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        groups.stream().map(CrmWorkOrderGroupDO::getManagerUserId).filter(Objects::nonNull).forEach(userIds::add);
        members.values().forEach(userIds::addAll);
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        return success(BeanUtils.toBean(groups, CrmWorkOrderGroupRespVO.class, vo -> {
            List<Long> memberIds = members.getOrDefault(vo.getId(), List.of());
            vo.setMemberUserIds(memberIds);
            vo.setMemberUserNames(memberIds.stream().map(users::get).filter(Objects::nonNull)
                    .map(AdminUserRespDTO::getNickname).toList());
            AdminUserRespDTO manager = users.get(vo.getManagerUserId());
            if (manager != null) vo.setManagerUserName(manager.getNickname());
        }));
    }

    @PostMapping("/save")
    @Operation(summary = "新增或更新客服处理组")
    @PreAuthorize("@ss.hasPermission('crm:work-order-group:update')")
    public CommonResult<Long> save(@Valid @RequestBody CrmWorkOrderGroupSaveReqVO request) {
        return success(groupService.saveGroup(request));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客服处理组")
    @PreAuthorize("@ss.hasPermission('crm:work-order-group:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        groupService.deleteGroup(id);
        return success(true);
    }
}
