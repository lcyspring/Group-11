export const INVOICE_STATUS = {
  DRAFT: 0,
  ISSUED: 10,
  PARTIALLY_RED: 20,
  FULLY_RED: 30,
  VOIDED: 40
} as const

export const INVOICE_DIRECTION = { BLUE: 1, RED: -1 } as const
export const INVOICE_TYPE = { VAT_ORDINARY: 1, VAT_SPECIAL: 2 } as const

export const canEditInvoice = (status?: number, direction?: number) =>
  status === INVOICE_STATUS.DRAFT && direction === INVOICE_DIRECTION.BLUE

export const canIssueInvoice = canEditInvoice

export const canRedFlushInvoice = (status?: number, direction?: number) =>
  direction === INVOICE_DIRECTION.BLUE &&
  (status === INVOICE_STATUS.ISSUED || status === INVOICE_STATUS.PARTIALLY_RED)

export const canVoidInvoice = (status?: number) => status === INVOICE_STATUS.ISSUED

export const remainingRedAmount = (amount = 0, redAmount = 0) =>
  Math.max(0, Number((amount - redAmount).toFixed(6)))

/** Normalize Element Plus' `x` date value to the Server-wide epoch-millisecond contract. */
export const toInvoiceEpochMillis = (value: string | number) => {
  const timestamp = Number(value)
  if (!Number.isFinite(timestamp) || timestamp <= 0) {
    throw new RangeError('invoice date must be a positive epoch-millisecond value')
  }
  return Math.trunc(timestamp)
}
