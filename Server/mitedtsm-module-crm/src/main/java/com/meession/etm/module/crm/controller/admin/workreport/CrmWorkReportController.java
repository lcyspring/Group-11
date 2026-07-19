package com.meession.etm.module.crm.controller.admin.workreport;

import com.meession.etm.framework.common.pojo.*;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.workreport.vo.*;
import com.meession.etm.module.crm.dal.dataobject.workreport.CrmWorkReportDO;
import com.meession.etm.module.crm.service.workreport.CrmWorkReportService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/crm/work-report")
public class CrmWorkReportController {
    @Resource private CrmWorkReportService service;
    @Resource private AdminUserApi adminUserApi;

    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('crm:work-report:create')")
    public CommonResult<Long> create(@Valid @RequestBody CrmWorkReportSaveReqVO req) { return success(service.create(getLoginUserId(), req)); }
    @PutMapping("/update") @PreAuthorize("@ss.hasPermission('crm:work-report:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody CrmWorkReportSaveReqVO req) { service.update(getLoginUserId(), req); return success(true); }
    @PutMapping("/submit") @PreAuthorize("@ss.hasPermission('crm:work-report:update')")
    public CommonResult<Boolean> submit(@RequestParam Long id) { service.submit(getLoginUserId(), id); return success(true); }
    @DeleteMapping("/delete") @PreAuthorize("@ss.hasPermission('crm:work-report:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) { service.delete(getLoginUserId(), id); return success(true); }
    @GetMapping("/get") @PreAuthorize("@ss.hasPermission('crm:work-report:query')")
    public CommonResult<CrmWorkReportRespVO> get(@RequestParam Long id) { return success(enrich(List.of(service.get(getLoginUserId(), id))).get(0)); }
    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('crm:work-report:query')")
    public CommonResult<PageResult<CrmWorkReportRespVO>> page(@Valid CrmWorkReportPageReqVO req) {
        PageResult<CrmWorkReportDO> page = service.page(getLoginUserId(), req);
        return success(new PageResult<>(enrich(page.getList()), page.getTotal()));
    }

    private List<CrmWorkReportRespVO> enrich(List<CrmWorkReportDO> rows) {
        Set<Long> ids = new HashSet<>();
        rows.forEach(row -> { ids.add(row.getAuthorUserId()); ids.addAll(Optional.ofNullable(row.getReceiverUserIds()).orElse(List.of())); });
        Map<Long, AdminUserRespDTO> users = ids.isEmpty() ? Map.of() : adminUserApi.getUserMap(ids);
        return rows.stream().map(row -> BeanUtils.toBean(row, CrmWorkReportRespVO.class)
                .setAuthorUserName(users.containsKey(row.getAuthorUserId()) ? users.get(row.getAuthorUserId()).getNickname() : null)
                .setReceiverUserNames(Optional.ofNullable(row.getReceiverUserIds()).orElse(List.of()).stream()
                        .map(users::get).filter(Objects::nonNull).map(AdminUserRespDTO::getNickname).toList())).toList();
    }
}
