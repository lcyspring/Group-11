import request from '@/config/axios'

export interface WorkOrderVO {
  id?: number
  no?: string
  title: string
  type: number
  priority: number
  status?: number
  customerId: number
  customerName?: string
  sourceType: number
  sourceId?: number
  serviceLatitude?: number
  serviceLongitude?: number
  geofenceRadiusMeters?: number
  checkInRequired?: boolean
  groupId?: number
  groupName?: string
  handlerUserId?: number
  handlerUserName?: string
  dispatchMode?: number
  assignTime?: Date
  ccUserIds?: number[]
  ccUserNames?: string[]
  description: string
  solution?: string
  attachmentUrls?: string[]
  processTime?: Date
  completeTime?: Date
  returnReason?: string
  creator?: string
  creatorName?: string
  createTime?: Date
  updateTime?: Date
  records?: WorkOrderRecordVO[]
  latestCheckIn?: WorkOrderCheckInVO
  sla?: WorkOrderSlaVO
}

export interface WorkOrderCheckInVO {
  id: number
  workOrderId: number
  userId: number
  latitude: number
  longitude: number
  accuracyMeters?: number
  distanceMeters: number
  createTime: Date
}

export interface WorkOrderSlaVO {
  id: number
  workOrderId: number
  policyId: number
  policyCode?: string
  policyName?: string
  responseDueTime: Date
  escalationDueTime?: Date
  resolutionDueTime: Date
  pausedSeconds: number
  pausedAt?: Date
  status: number
  escalatedAt?: Date
  completedAt?: Date
  paused: boolean
  overdue: boolean
}

export interface WorkOrderRecordVO {
  id: number
  actionType: number
  fromStatus?: number
  toStatus: number
  operatorUserId: number
  operatorUserName?: string
  handlerUserId?: number
  handlerUserName?: string
  remark?: string
  createTime: Date
}

export interface WorkOrderGroupVO {
  id?: number
  code: string
  name: string
  managerUserId: number
  managerUserName?: string
  supportedTypes: number[]
  memberUserIds: number[]
  memberUserNames?: string[]
  status: number
  sort: number
  remark?: string
}

export interface WorkOrderDispatchContextVO {
  enabled: boolean
  autoAssignOnCreate: boolean
  fallbackMode: string
  maxCcUsers: number
  manualAssignmentAllowed: boolean
  groups: WorkOrderGroupVO[]
  candidates: Array<{
    id: number
    nickname: string
    deptId?: number
    source: string
    openCount: number
  }>
}

export const getWorkOrderPage = async (params: any) => await request.get({ url: '/crm/work-order/page', params })
export const exportWorkOrders = async (params: any) =>
  await request.download({ url: '/crm/work-order/export-excel', params })
export const getWorkOrder = async (id: number) => await request.get({ url: '/crm/work-order/get?id=' + id })
export const createWorkOrder = async (data: WorkOrderVO) => await request.post({ url: '/crm/work-order/create', data })
export const updateWorkOrder = async (data: WorkOrderVO) => await request.put({ url: '/crm/work-order/update', data })
export const deleteWorkOrder = async (id: number) => await request.delete({ url: '/crm/work-order/delete?id=' + id })
export const startWorkOrder = async (id: number, remark?: string) => await request.put({ url: '/crm/work-order/start', data: { id, remark } })
export const assignWorkOrder = async (id: number, handlerUserId: number, groupId?: number, remark?: string) =>
  await request.put({ url: '/crm/work-order/assign', data: { id, handlerUserId, groupId, remark } })
export const claimWorkOrder = async (id: number, remark?: string) =>
  await request.put({ url: '/crm/work-order/claim', data: { id, remark } })
export const returnWorkOrder = async (id: number, reason: string) => await request.put({ url: '/crm/work-order/return', data: { id, reason } })
export const resubmitWorkOrder = async (id: number, remark?: string) => await request.put({ url: '/crm/work-order/resubmit', data: { id, remark } })
export const completeWorkOrder = async (id: number, solution: string) => await request.put({ url: '/crm/work-order/complete', data: { id, solution } })
export const checkInWorkOrder = async (id: number, latitude: number, longitude: number, accuracyMeters?: number, remark?: string) =>
  await request.put<WorkOrderCheckInVO>({ url: '/crm/work-order/check-in', data: { id, latitude, longitude, accuracyMeters, remark } })
export const getLatestCheckIn = async (id: number) => await request.get<WorkOrderCheckInVO>({ url: '/crm/work-order/check-in/latest', params: { id } })
export const getWorkOrderSla = async (id: number) => await request.get<WorkOrderSlaVO>({ url: '/crm/work-order/sla', params: { id } })
export const pauseWorkOrderSla = async (id: number, remark?: string) => await request.put({ url: '/crm/work-order/sla/pause', data: { id, remark } })
export const resumeWorkOrderSla = async (id: number, remark?: string) => await request.put({ url: '/crm/work-order/sla/resume', data: { id, remark } })
export const getWorkOrderSlaPolicies = async () => await request.get({ url: '/crm/work-order/sla/policies' })
export const getWorkOrderHolidays = async () => await request.get({ url: '/crm/work-order/sla/holidays' })
export const saveWorkOrderHoliday = async (data: any) => await request.post({ url: '/crm/work-order/sla/holiday/save', data })
export const deleteWorkOrderHoliday = async (id: number) => await request.delete({ url: '/crm/work-order/sla/holiday/delete', params: { id } })
export const getDispatchContext = async (type: number, groupId?: number) =>
  await request.get<WorkOrderDispatchContextVO>({ url: '/crm/work-order/dispatch-context', params: { type, groupId } })
export const getWorkOrderGroupList = async () =>
  await request.get<WorkOrderGroupVO[]>({ url: '/crm/work-order-group/list' })
export const saveWorkOrderGroup = async (data: WorkOrderGroupVO) =>
  await request.post<number>({ url: '/crm/work-order-group/save', data })
export const deleteWorkOrderGroup = async (id: number) =>
  await request.delete({ url: '/crm/work-order-group/delete', params: { id } })
