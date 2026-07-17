package com.meession.etm.framework.excel.service;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import com.meession.etm.framework.common.util.http.HttpUtils;
import com.meession.etm.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import com.meession.etm.framework.excel.core.handler.SelectSheetWriteHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据导入导出服务实现
 */
@Slf4j
@Service
public class DataImportExportServiceImpl implements DataImportExportService {

    @Override
    public void exportExcel(HttpServletResponse response, String fileName, String sheetName,
                            List<List<String>> head, List<List<Object>> data) {
        try {
            // 设置响应头
            response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(fileName));
            response.setContentType("application/vnd.ms-excel;charset=UTF-8");

            // 输出 Excel
            FastExcelFactory.write(response.getOutputStream())
                    .autoCloseStream(false)
                    .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                    .sheet(sheetName)
                    .head(head)
                    .doWrite(data);
        } catch (IOException e) {
            log.error("[exportExcel][导出 Excel 失败]", e);
            throw new RuntimeException("导出 Excel 失败", e);
        }
    }

    @Override
    public <T> void exportExcel(HttpServletResponse response, String fileName, String sheetName,
                                Class<T> headClass, List<T> data) {
        try {
            // 设置响应头
            response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(fileName));
            response.setContentType("application/vnd.ms-excel;charset=UTF-8");

            // 输出 Excel
            FastExcelFactory.write(response.getOutputStream(), headClass)
                    .autoCloseStream(false)
                    .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                    .registerWriteHandler(new SelectSheetWriteHandler(headClass))
                    .registerConverter(new LongStringConverter())
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (IOException e) {
            log.error("[exportExcel][导出 Excel 失败]", e);
            throw new RuntimeException("导出 Excel 失败", e);
        }
    }

    @Override
    public <T> List<T> importExcel(InputStream inputStream, Class<T> headClass) {
        try {
            return FastExcelFactory.read(inputStream, headClass, null)
                    .autoCloseStream(false)
                    .doReadAllSync();
        } catch (Exception e) {
            log.error("[importExcel][导入 Excel 失败]", e);
            throw new RuntimeException("导入 Excel 失败", e);
        }
    }

    @Override
    public void exportCsv(HttpServletResponse response, String fileName,
                          List<String> headers, List<List<String>> data) {
        try {
            // 设置响应头
            response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(fileName));
            response.setContentType("text/csv;charset=UTF-8");

            // 写入 CSV
            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
                // 写入 BOM
                writer.write('\ufeff');

                // 写入表头
                if (headers != null && !headers.isEmpty()) {
                    writer.println(String.join(",", headers.stream()
                            .map(this::escapeCsvField)
                            .toArray(String[]::new)));
                }

                // 写入数据
                if (data != null) {
                    for (List<String> row : data) {
                        writer.println(String.join(",", row.stream()
                                .map(this::escapeCsvField)
                                .toArray(String[]::new)));
                    }
                }
            }
        } catch (IOException e) {
            log.error("[exportCsv][导出 CSV 失败]", e);
            throw new RuntimeException("导出 CSV 失败", e);
        }
    }

    @Override
    public List<List<String>> importCsv(InputStream inputStream) {
        List<List<String>> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 跳过 BOM
                if (line.startsWith("\ufeff")) {
                    line = line.substring(1);
                }
                // 解析 CSV 行
                List<String> row = parseCsvLine(line);
                if (!row.isEmpty()) {
                    result.add(row);
                }
            }
        } catch (IOException e) {
            log.error("[importCsv][导入 CSV 失败]", e);
            throw new RuntimeException("导入 CSV 失败", e);
        }
        return result;
    }

    /**
     * 转义 CSV 字段
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // 如果包含逗号、双引号、换行符，需要用双引号包裹，并将双引号转义为两个双引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * 解析 CSV 行
     */
    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return result;
        }

        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;

        while (i < line.length()) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // 检查是否是转义的双引号
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                    } else {
                        // 结束当前字段
                        inQuotes = false;
                        i++;
                    }
                } else {
                    field.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    i++;
                } else if (c == ',') {
                    result.add(field.toString());
                    field = new StringBuilder();
                    i++;
                } else {
                    field.append(c);
                    i++;
                }
            }
        }

        // 添加最后一个字段
        result.add(field.toString());

        return result;
    }
}
