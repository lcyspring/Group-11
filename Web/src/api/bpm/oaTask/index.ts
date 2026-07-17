import request from '@/config/axios'
export interface OaTaskVO { id?: number; title: string; description?: string; creatorUserId?: number; assigneeUserId: number; participantUserIds?: string; priority?: number; status?: number; dueTime: string; reminderMinutes?: number; businessType?: string; businessId?: number; result?: string }
export const getOaTaskList = (status?: number) => request.get<OaTaskVO[]>({ url: '/bpm/oa/task/list', params: { status } })
export const createOaTask = (data: OaTaskVO) => request.post({ url: '/bpm/oa/task/create', data })
export const updateOaTask = (data: OaTaskVO) => request.put({ url: '/bpm/oa/task/update', data })
export const deleteOaTask = (id: number) => request.delete({ url: '/bpm/oa/task/delete', params: { id } })
export const startOaTask = (id: number) => request.put({ url: '/bpm/oa/task/start', params: { id } })
export const completeOaTask = (id: number, result?: string) => request.put({ url: '/bpm/oa/task/complete', params: { id, result } })
