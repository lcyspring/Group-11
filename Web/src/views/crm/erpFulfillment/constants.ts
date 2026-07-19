export const FULFILLMENT_STATUS = {
  CREATING: 0,
  CREATED: 10,
  FAILED: 20
} as const

export const ERP_AUDIT_STATUS = {
  PROCESSING: 10,
  APPROVED: 20
} as const

export const fulfillmentStatusType = (status?: number) =>
  status === FULFILLMENT_STATUS.CREATED
    ? 'success'
    : status === FULFILLMENT_STATUS.FAILED
      ? 'danger'
      : 'warning'

export const progressPercent = (current?: number | string, total?: number | string) => {
  const denominator = Number(total || 0)
  if (denominator <= 0) return 0
  return Math.max(0, Math.min(100, Number(((Number(current || 0) / denominator) * 100).toFixed(2))))
}

export const canCreateOrRetryFulfillment = (eligible: boolean, status?: number) =>
  eligible && status !== FULFILLMENT_STATUS.CREATED

export const canRefreshFulfillment = (status?: number, erpOrderId?: number) =>
  status === FULFILLMENT_STATUS.CREATED && Boolean(erpOrderId)
