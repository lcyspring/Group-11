import request from '@/config/axios'

export interface TripVO {
  id?: number
  userId?: number
  startTime: string | number
  endTime: string | number
  days?: number
  destination: string
  reason: string
  estimatedExpense?: number
  companionUserIds?: number[]
  attachmentUrls?: string[]
  status?: number
  processInstanceId?: string
  approvalTime?: string
  createTime?: string
  startUserSelectAssignees?: Record<string, number[]>
}

export const createTrip = (data: TripVO) => request.post({ url: '/bpm/oa/trip/create', data })
export const getTrip = (id: number) => request.get<TripVO>({ url: '/bpm/oa/trip/get', params: { id } })
export const getTripPage = (params: PageParam) => request.get({ url: '/bpm/oa/trip/page', params })
export const getReimbursableTrips = () => request.get<TripVO[]>({ url: '/bpm/oa/trip/reimbursable-list' })
