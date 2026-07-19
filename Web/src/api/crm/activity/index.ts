import request from '@/config/axios'

export interface ActivityPageReqVO {
  pageNo: number
  pageSize: number
  bizType: number
  bizId: number
}

export interface TaskVO {
  id?: number
  bizType: number
  bizId: number
  sourceClueId?: number
  type: number
  title: string
  description?: string
  priority: number
  status?: number
  assigneeUserId: number
  assigneeUserName?: string
  dueTime: number | string | Date
  remindTime?: number | string | Date
  notifySystem?: boolean
  notifyEmail?: boolean
  notifySms?: boolean
  startTime?: string | Date
  finishTime?: string | Date
  result?: string
  creator?: string
  creatorName?: string
  createTime?: string | Date
  updateTime?: string | Date
}

export interface TaskActionRecordVO {
  id: number
  taskId: number
  actionType: number
  fromStatus?: number
  toStatus: number
  operatorUserId?: number
  operatorUserName?: string
  remark?: string
  createTime: string | Date
}

export interface CallRecordVO {
  id?: number
  bizType: number
  bizId: number
  sourceClueId?: number
  contactId?: number
  direction: number
  status: number
  phone: string
  startTime: number | string | Date
  endTime?: number | string | Date
  durationSeconds?: number
  recordingUrl?: string
  summary?: string
  operatorUserId?: number
  operatorUserName?: string
  createTime?: string | Date
}

export interface SmsRecordVO {
  id?: number
  bizType: number
  bizId: number
  sourceClueId?: number
  contactId?: number
  direction: number
  status: number
  mobile: string
  content: string
  systemSmsLogId?: number
  externalMessageId?: string
  failureReason?: string
  occurredTime: number | string | Date
  operatorUserId?: number
  operatorUserName?: string
  createTime?: string | Date
}

export interface ClueConversionRecordVO {
  id: number
  clueId: number
  customerId: number
  primaryContactId?: number
  followUpCount: number
  taskCount: number
  callCount: number
  smsCount: number
  operatorUserId?: number
  operatorUserName?: string
  convertedAt: string | Date
}

export const getTaskPage = (params: ActivityPageReqVO & Record<string, any>) =>
  request.get({ url: '/crm/activity/task/page', params })
export const createTask = (data: TaskVO) => request.post({ url: '/crm/activity/task/create', data })
export const updateTask = (data: TaskVO) => request.put({ url: '/crm/activity/task/update', data })
export const startTask = (id: number, remark?: string) =>
  request.put({ url: '/crm/activity/task/start', data: { id, remark } })
export const completeTask = (id: number, remark?: string) =>
  request.put({ url: '/crm/activity/task/complete', data: { id, remark } })
export const markTaskUnable = (id: number, remark: string) =>
  request.put({ url: '/crm/activity/task/unable', data: { id, remark } })
export const cancelTask = (id: number, remark: string) =>
  request.put({ url: '/crm/activity/task/cancel', data: { id, remark } })
export const getTaskActionRecords = (taskId: number): Promise<TaskActionRecordVO[]> =>
  request.get({ url: '/crm/activity/task/action-records', params: { taskId } })

export const getCallRecordPage = (params: ActivityPageReqVO) =>
  request.get({ url: '/crm/activity/call/page', params })
export const createCallRecord = (data: CallRecordVO) =>
  request.post({ url: '/crm/activity/call/create', data })

export const getSmsRecordPage = (params: ActivityPageReqVO) =>
  request.get({ url: '/crm/activity/sms/page', params })
export const createSmsRecord = (data: SmsRecordVO) =>
  request.post({ url: '/crm/activity/sms/create', data })

export const getConversionRecord = (clueId: number): Promise<ClueConversionRecordVO | null> =>
  request.get({ url: '/crm/activity/conversion-record', params: { clueId } })
