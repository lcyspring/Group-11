import request from '@/config/axios'
export interface OaEventVO { id?: number; title: string; description?: string; startTime: string; endTime: string; allDay?: boolean; location?: string; participantUserIds?: string; reminderMinutes?: number; status?: number }
export const getOaEventList = (from: string, to: string) => request.get<OaEventVO[]>({ url: '/bpm/oa/event/list', params: { from, to } })
export const createOaEvent = (data: OaEventVO) => request.post({ url: '/bpm/oa/event/create', data })
export const updateOaEvent = (data: OaEventVO) => request.put({ url: '/bpm/oa/event/update', data })
export const deleteOaEvent = (id: number) => request.delete({ url: '/bpm/oa/event/delete', params: { id } })
