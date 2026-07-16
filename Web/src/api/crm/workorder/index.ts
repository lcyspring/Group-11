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
export const getDispatchContext = async (type: number, groupId?: number) =>
  await request.get<WorkOrderDispatchContextVO>({ url: '/crm/work-order/dispatch-context', params: { type, groupId } })
export const getWorkOrderGroupList = async () =>
  await request.get<WorkOrderGroupVO[]>({ url: '/crm/work-order-group/list' })
export const saveWorkOrderGroup = async (data: WorkOrderGroupVO) =>
  await request.post<number>({ url: '/crm/work-order-group/save', data })
export const deleteWorkOrderGroup = async (id: number) =>
  await request.delete({ url: '/crm/work-order-group/delete', params: { id } })
