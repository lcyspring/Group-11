import request from '@/config/axios'
export interface WorkRequestVO { id?: number; title: string; content: string; urgency: number; status?: number; processInstanceId?: string }
export const getList = () => request.get<WorkRequestVO[]>({ url: '/bpm/oa/work-request/list' })
export const create = (data: WorkRequestVO) => request.post({ url: '/bpm/oa/work-request/create', data })
