import request from '@/config/axios'

export interface ContractAmendmentProductVO {
  id?: number
  productId: number
  productNameSnapshot?: string
  productNoSnapshot?: string
  productUnitSnapshot?: number
  productPrice?: number
  contractPrice: number
  count: number
  totalPrice?: number
}

export interface ContractAmendmentVO {
  id?: number
  contractId: number
  no?: string
  clientRequestId: string
  baseVersion?: number
  targetVersion?: number
  title: string
  reason: string
  auditStatus?: number
  processInstanceId?: string
  amountBefore?: number
  amountAfter?: number
  amountDelta?: number
  effectiveTime?: number
  contractName: string
  startTime?: number
  endTime?: number
  discountPercent: number
  signContactId?: number
  signUserId?: number
  remark?: string
  products: ContractAmendmentProductVO[]
}

export const createContractAmendment = (data: ContractAmendmentVO) =>
  request.post({ url: '/crm/contract-amendment/create', data })

export const updateContractAmendment = (data: ContractAmendmentVO) =>
  request.put({ url: '/crm/contract-amendment/update', data })

export const submitContractAmendment = (contractId: number, id: number) =>
  request.put({ url: '/crm/contract-amendment/submit', data: { contractId, id } })

export const getContractAmendment = (contractId: number, id: number) =>
  request.get({ url: '/crm/contract-amendment/get', params: { contractId, id } })

export const getContractAmendmentList = (contractId: number) =>
  request.get({ url: '/crm/contract-amendment/list', params: { contractId } })
