package com.meession.etm.module.erp.api.sale.event;

import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderRespDTO;

/** ERP 销售订单状态或履约数量变化事件。 */
public record ErpSaleOrderChangedEvent(ErpSaleOrderRespDTO order) {
}
