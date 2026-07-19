import request from '@/config/axios'
import { formatDateTimeValue, type DateTimeValue } from '../../../utils/dateTimeValue'
export interface OaTaskVO { id?: number; title: string; description?: string; creatorUserId?: number; assigneeUserId: number; participantUserIds?: string; priority?: number; status?: number; dueTime: string; reminderMinutes?: number; businessType?: string; businessId?: number; result?: string }
type OaTaskRespVO = Omit<OaTaskVO, 'dueTime'> & { dueTime: DateTimeValue }
export const getOaTaskList = async (status?: number): Promise<OaTaskVO[]> => {
  const rows = await request.get<OaTaskRespVO[]>({ url: '/bpm/oa/task/list', params: { status } })
  return rows.map((row) => ({ ...row, dueTime: formatDateTimeValue(row.dueTime) }))
}
const toSavePayload = (data: OaTaskVO) => ({ ...data, dueTime: new Date(data.dueTime.replace(' ', 'T')).getTime() })
export const createOaTask = (data: OaTaskVO) => request.post({ url: '/bpm/oa/task/create', data: toSavePayload(data) })
export const updateOaTask = (data: OaTaskVO) => request.put({ url: '/bpm/oa/task/update', data: toSavePayload(data) })
export const deleteOaTask = (id: number) => request.delete({ url: '/bpm/oa/task/delete', params: { id } })
export const startOaTask = (id: number) => request.put({ url: '/bpm/oa/task/start', params: { id } })
export const completeOaTask = (id: number, result?: string) => request.put({ url: '/bpm/oa/task/complete', params: { id, result } })
