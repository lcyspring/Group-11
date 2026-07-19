import request from '@/config/axios'

export interface OaEventSaveReqVO {
  title: string
  description?: string
  startTime: string
  endTime: string
  allDay?: boolean
  location?: string
  participantUserIds?: string
  reminderMinutes?: number
}

export interface OaEventUpdateReqVO extends OaEventSaveReqVO {
  id: number
}

export interface OaEventVO extends OaEventUpdateReqVO {
  status?: number
}

const toLocalDateTime = (value: string) => value.replace(' ', 'T')

export const getOaEventList = (from: string, to: string) =>
  request.get<OaEventVO[]>({
    url: '/bpm/oa/event/list',
    params: { from: toLocalDateTime(from), to: toLocalDateTime(to) }
  })

const toSavePayload = (data: OaEventSaveReqVO | OaEventUpdateReqVO) => ({
  ...data,
  startTime: new Date(toLocalDateTime(data.startTime)).getTime(),
  endTime: new Date(toLocalDateTime(data.endTime)).getTime()
})

export const createOaEvent = (data: OaEventSaveReqVO) =>
  request.post<number>({ url: '/bpm/oa/event/create', data: toSavePayload(data) })

export const updateOaEvent = (data: OaEventUpdateReqVO) =>
  request.put<boolean>({ url: '/bpm/oa/event/update', data: toSavePayload(data) })

export const deleteOaEvent = (id: number) =>
  request.delete<boolean>({ url: '/bpm/oa/event/delete', params: { id } })
