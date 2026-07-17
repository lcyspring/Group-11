package com.meession.etm.framework.excel.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

/**
 * 数据导入导出服务接口
 * 提供通用的 Excel/CSV 数据导入导出能力
 */
public interface DataImportExportService {

    /**
     * 导出 Excel
     * @param response HTTP 响应
     * @param fileName 文件名
     * @param sheetName 工作表名
     * @param head 表头列表
     * @param data 数据列表
     */
    void exportExcel(HttpServletResponse response, String fileName, String sheetName,
                     List<List<String>> head, List<List<Object>> data);

    /**
     * 导出 Excel（使用 Class 定义表头）
     */
    <T> void exportExcel(HttpServletResponse response, String fileName, String sheetName,
                         Class<T> headClass, List<T> data);

    /**
     * 导入 Excel
     * @param inputStream 输入流
     * @param headClass 表头对应的 Class
     * @return 解析后的数据列表
     */
    <T> List<T> importExcel(InputStream inputStream, Class<T> headClass);

    /**
     * 导出 CSV
     */
    void exportCsv(HttpServletResponse response, String fileName,
                   List<String> headers, List<List<String>> data);

    /**
     * 导入 CSV
     */
    List<List<String>> importCsv(InputStream inputStream);
}
