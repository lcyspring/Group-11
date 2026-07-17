import request from '@/config/axios'

export type LeaveVO = {
  id: number
  status: number
  type: number
  reason: string
  processInstanceId: string
  startTime: string
  endTime: string
  day?: number
  attachmentUrls?: string[]
  createTime: string
  startUserSelectAssignees?: Record<string, number[]>
}

export type LeaveBalanceVO = {
  leaveType: number
  year: number
  totalDays: number
  reservedDays: number
  usedDays: number
  availableDays: number
  balanceRequired: boolean
}

// 创建请假申请
export const createLeave = async (data: LeaveVO) => {
  return await request.post({ url: '/bpm/oa/leave/create', data: data })
}

// 获得请假申请
export const getLeave = async (id: number) => {
  return await request.get({ url: '/bpm/oa/leave/get?id=' + id })
}

// 获得请假申请分页
export const getLeavePage = async (params: PageParam) => {
  return await request.get({ url: '/bpm/oa/leave/page', params })
}

export const getLeaveBalance = async (type: number, year: number) => {
  return await request.get<LeaveBalanceVO>({ url: '/bpm/oa/leave/balance', params: { type, year } })
}
