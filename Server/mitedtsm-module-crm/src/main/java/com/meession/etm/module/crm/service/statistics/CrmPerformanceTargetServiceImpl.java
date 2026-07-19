package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetBaseReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetListReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.statistics.CrmPerformanceTargetDO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmPerformanceTargetMapper;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetScopeTypeEnum;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetTypeEnum;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_CONCURRENT_MODIFICATION;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_COUNT_DECIMAL;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_SCOPE_INVALID;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_TYPE_INVALID;

@Service
@Validated
public class CrmPerformanceTargetServiceImpl implements CrmPerformanceTargetService {

    @Resource
    private CrmPerformanceTargetMapper performanceTargetMapper;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePerformanceTarget(CrmPerformanceTargetSaveReqVO reqVO) {
        validateScope(reqVO.getScopeType(), reqVO.getScopeId());
        validateMonthlyTargets(reqVO.getTargetType(), reqVO.getMonthlyTargets());

        List<CrmPerformanceTargetDO> existing = performanceTargetMapper.selectListByScopeAndYear(
                reqVO.getScopeType(), reqVO.getScopeId(), reqVO.getTargetYear(), reqVO.getTargetType());
        Map<Integer, CrmPerformanceTargetDO> existingByMonth = convertMap(existing,
                CrmPerformanceTargetDO::getTargetMonth, Function.identity());
        try {
            for (int index = 0; index < reqVO.getMonthlyTargets().size(); index++) {
                int month = index + 1;
                CrmPerformanceTargetDO target = existingByMonth.get(month);
                if (target == null) {
                    performanceTargetMapper.insert(new CrmPerformanceTargetDO()
                            .setScopeType(reqVO.getScopeType())
                            .setScopeId(reqVO.getScopeId())
                            .setTargetYear(reqVO.getTargetYear())
                            .setTargetMonth(month)
                            .setTargetType(reqVO.getTargetType())
                            .setTargetValue(reqVO.getMonthlyTargets().get(index)));
                } else if (target.getTargetValue().compareTo(reqVO.getMonthlyTargets().get(index)) != 0) {
                    performanceTargetMapper.updateById(new CrmPerformanceTargetDO()
                            .setId(target.getId())
                            .setTargetValue(reqVO.getMonthlyTargets().get(index)));
                }
            }
        } catch (DuplicateKeyException ex) {
            // 唯一键负责兜住两个并发首次设置同一目标的竞态；整个年度更新会随事务回滚。
            throw exception(PERFORMANCE_TARGET_CONCURRENT_MODIFICATION);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePerformanceTarget(CrmPerformanceTargetBaseReqVO reqVO) {
        validateScope(reqVO.getScopeType(), reqVO.getScopeId());
        performanceTargetMapper.deleteByScopeYearAndType(reqVO.getScopeType(), reqVO.getScopeId(),
                reqVO.getTargetYear(), reqVO.getTargetType());
    }

    @Override
    public List<CrmPerformanceTargetDO> getPerformanceTargetList(CrmPerformanceTargetListReqVO reqVO) {
        validateScope(reqVO.getScopeType(), reqVO.getScopeId());
        return performanceTargetMapper.selectListByScopeAndYear(reqVO.getScopeType(), reqVO.getScopeId(),
                reqVO.getTargetYear(), null);
    }

    private void validateScope(Integer scopeType, Long scopeId) {
        CrmPerformanceTargetScopeTypeEnum scopeTypeEnum = CrmPerformanceTargetScopeTypeEnum.fromType(scopeType);
        if (scopeTypeEnum == null) {
            throw exception(PERFORMANCE_TARGET_SCOPE_INVALID);
        }
        switch (scopeTypeEnum) {
            case COMPANY -> {
                if (!Long.valueOf(0L).equals(scopeId)) {
                    throw exception(PERFORMANCE_TARGET_SCOPE_INVALID);
                }
            }
            case DEPARTMENT -> deptApi.validateDeptList(Collections.singleton(scopeId));
            case USER -> adminUserApi.validateUser(scopeId);
        }
    }

    private void validateMonthlyTargets(Integer targetType, List<BigDecimal> monthlyTargets) {
        CrmPerformanceTargetTypeEnum targetTypeEnum = CrmPerformanceTargetTypeEnum.fromType(targetType);
        if (targetTypeEnum == null) {
            throw exception(PERFORMANCE_TARGET_TYPE_INVALID);
        }
        if (!targetTypeEnum.isCount()) {
            return;
        }
        boolean containsDecimal = monthlyTargets.stream().anyMatch(value -> value.stripTrailingZeros().scale() > 0);
        if (containsDecimal) {
            throw exception(PERFORMANCE_TARGET_COUNT_DECIMAL);
        }
    }

}
