package com.meession.etm.module.infra.controller.admin.dataimport;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.excel.progress.ImportProgressTracker;
import com.meession.etm.framework.excel.service.DataImportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "管理后台 - 数据导入导出")
@RestController
@RequestMapping("/infra/data-import")
@Validated
public class DataImportExportController {

    @Resource
    private DataImportExportService dataImportExportService;

    @PostMapping("/export-template")
    @Operation(summary = "下载导入模板")
    @PreAuthorize("@ss.hasPermission('infra:data-import:export-template')")
    public void exportTemplate(HttpServletResponse response,
                               @RequestParam("templateType") String templateType) {
        // 根据模板类型生成对应的模板
        // 这里只是一个示例，实际需要根据 templateType 动态生成
        List<List<String>> head = new ArrayList<>();
        head.add(List.of("字段1"));
        head.add(List.of("字段2"));
        head.add(List.of("字段3"));

        List<List<Object>> data = new ArrayList<>();
        // 可以添加示例数据

        dataImportExportService.exportExcel(response, "导入模板.xlsx", "模板数据", head, data);
    }

    @PostMapping("/import-data")
    @Operation(summary = "导入数据（上传Excel）")
    @PreAuthorize("@ss.hasPermission('infra:data-import:import')")
    public CommonResult<Map<String, Object>> importData(@Valid @RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "templateType", required = false) String templateType) throws IOException {
        // 生成任务ID
        String taskId = UUID.randomUUID().toString();

        // 读取 Excel 数据
        List<List<Object>> dataList = new ArrayList<>();
        // 这里需要根据实际的 Class 来读取，暂时使用通用方式
        // 实际使用时需要传入对应的 headClass

        // 创建进度跟踪任务
        int totalCount = dataList.size();
        ImportProgressTracker.ImportTask task = ImportProgressTracker.createTask(taskId, totalCount);

        // 模拟处理过程
        try {
            for (int i = 0; i < dataList.size(); i++) {
                // 处理每一行数据
                // 这里添加实际的业务逻辑

                task.incrementProcessed();
                task.incrementSuccess();
            }
            task.setStatus("COMPLETED");
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
        }

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("totalCount", totalCount);
        result.put("successCount", task.getSuccessCount());
        result.put("failCount", task.getFailCount());
        result.put("status", task.getStatus());

        return CommonResult.success(result);
    }

    @GetMapping("/progress/{taskId}")
    @Operation(summary = "查询导入进度")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    @PreAuthorize("@ss.hasPermission('infra:data-import:query')")
    public CommonResult<Map<String, Object>> getProgress(@PathVariable("taskId") String taskId) {
        ImportProgressTracker.ImportTask task = ImportProgressTracker.getTask(taskId);
        if (task == null) {
            return CommonResult.error(404, "任务不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("totalCount", task.getTotalCount());
        result.put("processedCount", task.getProcessedCount());
        result.put("successCount", task.getSuccessCount());
        result.put("failCount", task.getFailCount());
        result.put("progress", task.getProgress());
        result.put("status", task.getStatus());
        result.put("errorMessage", task.getErrorMessage());

        return CommonResult.success(result);
    }

    @PostMapping("/export-data")
    @Operation(summary = "导出数据")
    @PreAuthorize("@ss.hasPermission('infra:data-import:export')")
    public void exportData(HttpServletResponse response,
                           @RequestParam("dataType") String dataType,
                           @RequestParam(value = "filters", required = false) String filters) {
        // 根据 dataType 和 filters 查询数据
        // 这里只是一个示例
        List<List<String>> head = new ArrayList<>();
        head.add(List.of("字段1"));
        head.add(List.of("字段2"));
        head.add(List.of("字段3"));

        List<List<Object>> data = new ArrayList<>();
        // 添加查询到的数据

        dataImportExportService.exportExcel(response, "导出数据.xlsx", "数据", head, data);
    }
}
