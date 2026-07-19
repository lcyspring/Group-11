package com.meession.etm.module.crm.framework.excel.core;

import com.meession.etm.framework.excel.core.function.ExcelColumnSelectFunction;
import com.meession.etm.framework.ip.core.Area;
import com.meession.etm.framework.ip.core.utils.AreaUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 地区下拉框数据源的 {@link ExcelColumnSelectFunction} 实现类
 *
 * @author HUIHUI
 */
@Service
public class AreaExcelColumnSelectFunction implements ExcelColumnSelectFunction {

    public static final String NAME = "getCrmAreaNameList"; // 防止和别的模块重名

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getOptions() {
        // 导出完整地区路径，避免同名区县在 Excel 下拉框中无法区分。
        Area area = AreaUtils.getArea(Area.ID_CHINA);
        return AreaUtils.getAreaNodePathList(area.getChildren());
    }

}
