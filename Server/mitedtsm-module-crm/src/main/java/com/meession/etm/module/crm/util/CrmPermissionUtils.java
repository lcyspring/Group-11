package com.meession.etm.module.crm.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.meession.etm.module.crm.dal.dataobject.permission.CrmPermissionDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import com.meession.etm.module.crm.framework.permission.CrmOwnerReadScope;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.yulichang.autoconfigure.MybatisPlusJoinProperties;
import com.github.yulichang.wrapper.MPJLambdaWrapper;

import java.util.Set;

import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 数据权限工具类
 *
 * @author HUIHUI
 */
public class CrmPermissionUtils {

    /**
     * 校验用户是否是 CRM 管理员
     *
     * @return 是/否
     */
    public static boolean isCrmAdmin() {
        return authorizationService().isCrmAdmin(getLoginUserId());
    }

    /**
     * 构造 CRM 数据类型数据【分页】查询条件
     *
     * @param query     连表查询对象
     * @param bizType   数据类型 {@link CrmBizTypeEnum}
     * @param bizId     数据编号
     * @param userId    用户编号
     * @param sceneType 场景类型
     */
    public static <T extends MPJLambdaWrapper<?>, S> void appendPermissionCondition(T query, Integer bizType, SFunction<S, ?> bizId,
                                                                                    Long userId, Integer sceneType) {
        MybatisPlusJoinProperties mybatisPlusJoinProperties = SpringUtil.getBean(MybatisPlusJoinProperties.class);
        final String ownerUserIdField = mybatisPlusJoinProperties.getTableAlias() + ".owner_user_id";
        // 缺省场景不能等价于“无数据权限条件”。CRM 管理员保留全量视图，普通用户默认仅看本人负责。
        if (sceneType == null) {
            sceneType = resolveDefaultSceneType(false, isCrmAdmin());
            if (sceneType == null) {
                return;
            }
        }
        // 场景一：我负责的数据
        if (CrmSceneTypeEnum.isOwner(sceneType)) {
            query.eq(ownerUserIdField, userId);
        }
        // 场景二：我参与的数据（我有读或写权限，并且不是负责人）
        if (CrmSceneTypeEnum.isInvolved(sceneType)) {
            if (CrmPermissionUtils.isCrmAdmin()) {
                return;
            }
            query.innerJoin(CrmPermissionDO.class, on -> on.eq(CrmPermissionDO::getBizType, bizType)
                    .eq(CrmPermissionDO::getBizId, bizId)
                    .in(CrmPermissionDO::getLevel, CrmPermissionLevelEnum.READ.getLevel(), CrmPermissionLevelEnum.WRITE.getLevel())
                    .eq(CrmPermissionDO::getUserId,userId));
            query.ne(ownerUserIdField, userId);
        }
        // 场景三：下属负责的数据（下属是负责人）
        if (CrmSceneTypeEnum.isSubordinate(sceneType)) {
            Set<Long> subordinateOwnerUserIds = authorizationService().resolveReadableSubordinateOwnerUserIds(userId);
            if (CollUtil.isEmpty(subordinateOwnerUserIds)) {
                query.eq(ownerUserIdField, -1); // 不返回任何结果
            } else {
                query.in(ownerUserIdField, subordinateOwnerUserIds);
            }
        }
        // 场景四：组织数据范围。组织范围只授予读取，不会被写、删除、转移和导出复用。
        if (CrmSceneTypeEnum.isOrganization(sceneType)) {
            CrmOwnerReadScope scope = authorizationService().resolveOwnerReadScope(userId);
            if (scope.all()) {
                return;
            }
            if (CollUtil.isEmpty(scope.ownerUserIds())) {
                query.eq(ownerUserIdField, -1);
            } else {
                query.in(ownerUserIdField, scope.ownerUserIds());
            }
        }
    }

    private static CrmAuthorizationService authorizationService() {
        return SpringUtil.getBean(CrmAuthorizationService.class);
    }

    static Integer resolveDefaultSceneType(boolean sceneProvided, boolean crmAdmin) {
        if (sceneProvided || crmAdmin) {
            return null;
        }
        return CrmSceneTypeEnum.OWNER.getType();
    }

}
