package com.meession.etm.module.crm.controller.admin.clue;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.apilog.core.annotation.ApiAccessLog;
import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.collection.MapUtils;
import com.meession.etm.framework.common.util.number.NumberUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.framework.ip.core.utils.AreaUtils;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmCluePageReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueRespVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueSaveReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueTransferReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueTransformReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.publicpool.CrmCluePublicAssignReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.publicpool.CrmCluePublicClaimReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.publicpool.CrmCluePublicPageReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.publicpool.CrmCluePublicPutReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.service.clue.CrmClueService;
import com.meession.etm.module.crm.service.clue.CrmCluePublicPoolService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.meession.etm.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.common.pojo.PageParam.PAGE_SIZE_NONE;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static java.util.Collections.singletonList;

@Tag(name = "管理后台 - 线索")
@RestController
@RequestMapping("/crm/clue")
@Validated
public class CrmClueController {

    @Resource
    private CrmClueService clueService;
    @Resource
    private CrmCluePublicPoolService cluePublicPoolService;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmPermissionService permissionService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建线索")
    @PreAuthorize("@ss.hasPermission('crm:clue:create')")
    public CommonResult<Long> createClue(@Valid @RequestBody CrmClueSaveReqVO createReqVO) {
        return success(clueService.createClue(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新线索")
    @PreAuthorize("@ss.hasPermission('crm:clue:update')")
    public CommonResult<Boolean> updateClue(@Valid @RequestBody CrmClueSaveReqVO updateReqVO) {
        clueService.updateClue(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除线索")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:clue:delete')")
    public CommonResult<Boolean> deleteClue(@RequestParam("id") Long id) {
        clueService.deleteClue(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得线索")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:clue:query')")
    public CommonResult<CrmClueRespVO> getClue(@RequestParam("id") Long id) {
        CrmClueDO clue = clueService.getClue(id);
        return success(buildClueDetail(clue));
    }

    private CrmClueRespVO buildClueDetail(CrmClueDO clue) {
        if (clue == null) {
            return null;
        }
        return buildClueDetailList(singletonList(clue)).get(0);
    }

    @GetMapping("/page")
    @Operation(summary = "获得线索分页")
    @PreAuthorize("@ss.hasPermission('crm:clue:query')")
    public CommonResult<PageResult<CrmClueRespVO>> getCluePage(@Valid CrmCluePageReqVO pageVO) {
        PageResult<CrmClueDO> pageResult = clueService.getCluePage(pageVO, getLoginUserId());
        return success(new PageResult<>(buildClueDetailList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/public-page")
    @Operation(summary = "获得公共线索分页")
    @PreAuthorize("@ss.hasPermission('crm:clue-public:query')")
    public CommonResult<PageResult<CrmClueRespVO>> getPublicCluePage(
            @Valid CrmCluePublicPageReqVO pageReqVO) {
        PageResult<CrmClueDO> page = cluePublicPoolService.getPublicPage(pageReqVO);
        return success(new PageResult<>(buildClueDetailList(page.getList()), page.getTotal()));
    }

    @PutMapping("/put-public")
    @Operation(summary = "将负责的线索放入公共线索池")
    @PreAuthorize("@ss.hasPermission('crm:clue-public:put')")
    public CommonResult<Boolean> putCluePublic(@Valid @RequestBody CrmCluePublicPutReqVO reqVO) {
        cluePublicPoolService.putCluePublic(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/claim-public")
    @Operation(summary = "自助领取公共线索")
    @PreAuthorize("@ss.hasPermission('crm:clue-public:claim')")
    public CommonResult<Boolean> claimPublicClues(@Valid @RequestBody CrmCluePublicClaimReqVO reqVO) {
        cluePublicPoolService.claimPublicClues(reqVO.getClueIds(), getLoginUserId());
        return success(true);
    }

    @PutMapping("/assign-public")
    @Operation(summary = "主管分配公共线索")
    @PreAuthorize("@ss.hasPermission('crm:clue-public:assign')")
    public CommonResult<Boolean> assignPublicClues(@Valid @RequestBody CrmCluePublicAssignReqVO reqVO) {
        cluePublicPoolService.assignPublicClues(reqVO.getClueIds(), reqVO.getOwnerUserId(), getLoginUserId());
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出线索 Excel")
    @PreAuthorize("@ss.hasPermission('crm:clue:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportClueExcel(@Valid CrmCluePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PAGE_SIZE_NONE);
        Long userId = getLoginUserId();
        List<CrmClueDO> list = clueService.getCluePage(pageReqVO, userId).getList();
        permissionService.validateExportPermission(CrmBizTypeEnum.CRM_CLUE.getType(),
                convertSet(list, CrmClueDO::getId), userId);
        // 导出 Excel
        ExcelUtils.write(response, "线索.xls", "数据", CrmClueRespVO.class, buildClueDetailList(list));
    }

    private List<CrmClueRespVO> buildClueDetailList(List<CrmClueDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1.1 获取客户列表
        Map<Long, CrmCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(list, CrmClueDO::getCustomerId));
        // 1.2 获取创建人、负责人列表
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(list,
                contact -> Stream.of(NumberUtils.parseLong(contact.getCreator()), contact.getOwnerUserId(),
                        contact.getPoolPreviousOwnerUserId())));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        // 2. 转换成 VO
        return BeanUtils.toBean(list, CrmClueRespVO.class, clueVO -> {
            clueVO.setAreaName(AreaUtils.format(clueVO.getAreaId()));
            // 2.1 设置客户名称
            MapUtils.findAndThen(customerMap, clueVO.getCustomerId(), customer -> clueVO.setCustomerName(customer.getName()));
            // 2.2 设置创建人、负责人名称
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(clueVO.getCreator()),
                    user -> clueVO.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, clueVO.getOwnerUserId(), user -> {
                clueVO.setOwnerUserName(user.getNickname());
                MapUtils.findAndThen(deptMap, user.getDeptId(), dept -> clueVO.setOwnerUserDeptName(dept.getName()));
            });
            MapUtils.findAndThen(userMap, clueVO.getPoolPreviousOwnerUserId(),
                    user -> clueVO.setPoolPreviousOwnerUserName(user.getNickname()));
        });
    }

    @PutMapping("/transfer")
    @Operation(summary = "线索转移")
    @PreAuthorize("@ss.hasPermission('crm:clue:update')")
    public CommonResult<Boolean> transferClue(@Valid @RequestBody CrmClueTransferReqVO reqVO) {
        clueService.transferClue(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/transform")
    @Operation(summary = "线索转化为客户")
    @PreAuthorize("@ss.hasPermission('crm:clue:update')")
    public CommonResult<Boolean> transformClue(@Valid @RequestBody CrmClueTransformReqVO reqVO) {
        clueService.transformClue(reqVO, getLoginUserId());
        return success(Boolean.TRUE);
    }

    @GetMapping("/follow-count")
    @Operation(summary = "获得分配给我的、待跟进的线索数量")
    @PreAuthorize("@ss.hasPermission('crm:clue:query')")
    public CommonResult<Long> getFollowClueCount() {
        return success(clueService.getFollowClueCount(getLoginUserId()));
    }

}
