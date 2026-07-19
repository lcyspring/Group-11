package com.meession.etm.module.crm.controller.admin.exporttask;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.apilog.core.annotation.ApiAccessLog;
import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerPageReqVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportDownloadTokenRespVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportTaskIdReqVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportTaskPageReqVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportTaskRespVO;
import com.meession.etm.module.crm.service.exporttask.CrmExportTaskService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.meession.etm.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/crm/export-task")
@RequiredArgsConstructor
public class CrmExportTaskController {
    private final CrmExportTaskService service;

    @PostMapping("/customer")
    @PreAuthorize("@ss.hasPermission('crm:customer:export')")
    public CommonResult<Long> createCustomerTask(@Valid @RequestBody CrmCustomerPageReqVO filter) {
        return success(service.createCustomerTask(filter, getLoginUserId()));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('crm:customer:export')")
    public CommonResult<PageResult<CrmExportTaskRespVO>> getTaskPage(@Valid CrmExportTaskPageReqVO request) {
        return success(service.getTaskPage(request, getLoginUserId()));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('crm:customer:export')")
    public CommonResult<CrmExportTaskRespVO> getTask(@RequestParam Long id) {
        return success(service.getTask(id, getLoginUserId()));
    }

    @PostMapping("/download-token")
    @PreAuthorize("@ss.hasPermission('crm:customer:export')")
    public CommonResult<CrmExportDownloadTokenRespVO> issueDownloadToken(
            @Valid @RequestBody CrmExportTaskIdReqVO request) {
        return success(service.issueDownloadToken(request.getId(), getLoginUserId()));
    }

    @GetMapping("/download")
    @PreAuthorize("@ss.hasPermission('crm:customer:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void download(@RequestParam Long id, @RequestParam String token,
                         HttpServletResponse response) throws IOException {
        CrmExportTaskService.DownloadFile file = service.download(id, token, getLoginUserId());
        response.setContentType(StrUtil.blankToDefault(file.contentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE));
        response.setContentLength(file.content().length);
        response.setHeader("Content-Disposition", ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build().toString());
        IoUtil.write(response.getOutputStream(), false, file.content());
    }
}
