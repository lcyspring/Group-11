import type { WorkOrderSlaVO, WorkOrderVO } from '@/api/crm/workorder'

export const canMobileCheckIn = (order: WorkOrderVO) =>
  order.status !== 30 && order.serviceLatitude != null && order.serviceLongitude != null

export const slaState = (sla?: WorkOrderSlaVO) => {
  if (!sla) return 'NONE'
  if (sla.paused) return 'PAUSED'
  if (sla.overdue) return 'OVERDUE'
  if (sla.escalatedAt) return 'ESCALATED'
  if (sla.completedAt || sla.status === 2) return 'COMPLETED'
  return 'ACTIVE'
}
