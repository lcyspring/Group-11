import request from '@/config/axios'
export interface OaDocumentVO { id?: number; parentId?: number; name: string; description?: string; visibility: number; currentVersion?: number }
export const list = (parentId?: number) => request.get<OaDocumentVO[]>({ url: '/bpm/oa/document/list', params: { parentId } })
export const create = (data: OaDocumentVO) => request.post({ url: '/bpm/oa/document/create', data })
export const archive = (id: number) => request.delete({ url: '/bpm/oa/document/archive', params: { id } })
export const versions = (id: number) => request.get({ url: '/bpm/oa/document/versions', params: { id } })
