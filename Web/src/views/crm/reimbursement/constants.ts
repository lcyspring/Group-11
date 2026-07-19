export const REIMBURSEMENT_STATUS = {
  DRAFT: 0,
  PROCESSING: 10,
  APPROVED: 20,
  REJECTED: 30,
  CANCELED: 40
} as const

export const EDITABLE_REIMBURSEMENT_STATUSES = [
  REIMBURSEMENT_STATUS.DRAFT,
  REIMBURSEMENT_STATUS.REJECTED,
  REIMBURSEMENT_STATUS.CANCELED
] as const

export const canEditReimbursement = (status?: number) =>
  EDITABLE_REIMBURSEMENT_STATUSES.includes(status as never)

export const canDeleteReimbursement = (status?: number, processInstanceId?: string) =>
  status === REIMBURSEMENT_STATUS.DRAFT && !processInstanceId

export const calculateReimbursementTotal = (amounts: Array<number | string | undefined>) =>
  Number(amounts.reduce<number>((sum, amount) => sum + Number(amount || 0), 0).toFixed(6))

export function isOccurredDateInRange(occurredDate: string, startDate: string, endDate: string) {
  if (!occurredDate || !startDate || !endDate) return false
  return occurredDate >= startDate && occurredDate <= endDate
}
