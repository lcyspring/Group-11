package com.meession.etm.module.crm.controller.admin.statistics.vo;

/**
 * Common department / owner selection used by CRM statistics queries.
 *
 * <p>The browser-provided identifiers are untrusted. Controllers carrying a statistics data-scope
 * guard validate this selection before invoking a statistics service.</p>
 */
public interface CrmStatisticsScopedReqVO {

    Long getDeptId();

    default Long getUserId() {
        return null;
    }

}
