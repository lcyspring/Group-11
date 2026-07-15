package com.meession.etm.module.crm.framework.permission.core.aop;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.util.spring.SpringExpressionUtils;
import com.meession.etm.framework.web.core.util.WebFrameworkUtils;
import com.meession.etm.module.crm.dal.dataobject.permission.CrmPermissionDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.*;
import static com.meession.etm.framework.common.util.json.JsonUtils.toJsonString;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CRM_PERMISSION_DENIED;

/**
 * Crm 数据权限校验 AOP 切面
 *
 * @author HUIHUI
 */
@Component
@Aspect
@Slf4j
public class CrmPermissionAspect {

    @Resource
    private CrmPermissionService crmPermissionService;

    @Resource
    private CrmAuthorizationService crmAuthorizationService;

    @Before("@annotation(crmPermission)")
    public void doBefore(JoinPoint joinPoint, CrmPermission crmPermission) {
        // 1.1 获取相关属性值
        Map<String, Object> expressionValues = parseExpressions(joinPoint, crmPermission);
        Integer bizType = StrUtil.isEmpty(crmPermission.bizTypeValue()) ?
                crmPermission.bizType()[0].getType() : (Integer) expressionValues.get(crmPermission.bizTypeValue()); // 模块类型
        // 1.2 处理兼容多个 bizId 的情况
        Object object = expressionValues.get(crmPermission.bizId()); // 模块数据编号
        Set<Long> bizIds = new HashSet<>();
        if (object instanceof Collection<?>) {
            bizIds.addAll(convertSet((Collection<?>) object, item -> Long.parseLong(item.toString())));
        } else {
            bizIds.add(Long.parseLong(object.toString()));
        }
        Integer permissionLevel = crmPermission.level().getLevel(); // 需要的权限级别

        // 2. 逐个校验权限
        List<CrmPermissionDO> permissionList = crmPermissionService.getPermissionListByBiz(bizType, bizIds);
        Map<Long, List<CrmPermissionDO>> multiMap = convertMultiMap(permissionList, CrmPermissionDO::getBizId);
        bizIds.forEach(bizId -> validatePermission(bizType, multiMap.get(bizId), permissionLevel));
    }

    private void validatePermission(Integer bizType, List<CrmPermissionDO> bizPermissions, Integer permissionLevel) {
        // 1. 如果是超级管理员则直接通过
        Long userId = getUserId();
        if (crmAuthorizationService.isCrmAdmin(userId)) {
            return;
        }
        // 特殊：没有数据权限的情况，针对 READ 的特殊处理
        if (CollUtil.isEmpty(bizPermissions)) {
            // 1.1 公海数据，如果没有团队成员，大家也应该有 READ 权限才对
            if (CrmPermissionLevelEnum.isRead(permissionLevel)) {
                return;
            }
            // 没有数据权限的情况下超出了读权限直接报错，避免后面校验空指针
            throw exception(CRM_PERMISSION_DENIED, CrmBizTypeEnum.getNameByType(bizType));
        } else { // 1.2 有数据权限但是没有负责人的情况
            if (!anyMatch(bizPermissions, item -> CrmPermissionLevelEnum.isOwner(item.getLevel()))
                && CrmPermissionLevelEnum.isRead(permissionLevel)) {
                return;
            }
        }

        // 2. 只考虑自的身权限
        if (crmAuthorizationService.isGranted(bizPermissions, userId, permissionLevel)) {
            return;
        }

        // 3. 没有权限，抛出异常
        log.info("[doBefore][userId({}) 要求权限({}) 实际权限({}) 数据校验错误]", // 打个 info 日志，方便后续排查问题、审计
                userId, permissionLevel, toJsonString(bizPermissions));
        throw exception(CRM_PERMISSION_DENIED, CrmBizTypeEnum.getNameByType(bizType));
    }

    /**
     * 获得用户编号
     *
     * @return 用户编号
     */
    private static Long getUserId() {
        return WebFrameworkUtils.getLoginUserId();
    }

    private static Map<String, Object> parseExpressions(JoinPoint joinPoint, CrmPermission crmPermission) {
        // 1. 需要解析的表达式
        List<String> expressionStrings = new ArrayList<>(2);
        expressionStrings.add(crmPermission.bizId());
        if (StrUtil.isNotEmpty(crmPermission.bizTypeValue())) { // 为空则表示 bizType 有值
            expressionStrings.add(crmPermission.bizTypeValue());
        }
        // 2. 执行解析
        return SpringExpressionUtils.parseExpressions(joinPoint, expressionStrings);
    }

}
