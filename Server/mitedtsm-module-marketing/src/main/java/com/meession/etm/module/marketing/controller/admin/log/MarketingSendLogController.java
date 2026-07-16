package com.meession.etm.module.marketing.controller.admin.log;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.marketing.controller.admin.log.vo.SendLogPageReqVO;
import com.meession.etm.module.marketing.controller.admin.log.vo.SendLogRespVO;
import com.meession.etm.module.marketing.controller.admin.log.vo.SendStatisticsRespVO;
import com.meession.etm.module.marketing.dal.dataobject.log.MarketingSendRecordDO;
import com.meession.etm.module.marketing.service.log.MarketingSendLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 营销发送记录")
@RestController
@RequestMapping("/marketing/send-log")
@Validated
public class MarketingSendLogController {

    @Resource
    private MarketingSendLogService sendLogService;

    @GetMapping("/page")
    @Operation(summary = "获得营销发送记录分页")
    @PreAuthorize("@ss.hasPermission('marketing:send-log:query')")
    public CommonResult<PageResult<SendLogRespVO>> getSendLogPage(@Valid SendLogPageReqVO pageVO) {
        PageResult<MarketingSendRecordDO> pageResult = sendLogService.getSendLogPage(pageVO);
        return success(BeanUtils.toBean(pageResult, SendLogRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得营销发送记录详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('marketing:send-log:query')")
    public CommonResult<SendLogRespVO> getSendLog(@RequestParam("id") Long id) {
        MarketingSendRecordDO record = sendLogService.getSendLog(id);
        return success(BeanUtils.toBean(record, SendLogRespVO.class));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获得活动发送统计")
    @Parameter(name = "campaignId", description = "活动编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('marketing:send-log:query')")
    public CommonResult<SendStatisticsRespVO> getStatistics(@RequestParam("campaignId") Long campaignId) {
        return success(sendLogService.getStatistics(campaignId));
    }

}
